# Composition Module

## Purpose

The Composition module exists to **aggregate data from multiple modules** into a single response, reducing the number of frontend requests in complex scenarios. This is especially important for screens that require data from many different sources.

## Architecture Principle: Read-Only Composition

The module follows a **"read-only composition"** pattern:

- **No business logic**: The module does not contain business rules or validation logic. It only reads and composes data.
- **No data modification**: The module never creates, updates, or deletes data. It is purely read-only.
- **Reuse existing DTOs**: The module reuses DTOs from other modules (schedule, offering, subject, group, document, teacher, program). It does not create new "view DTOs" except for a container/wrapper that aggregates existing DTOs.
- **Simple orchestration**: The internal logic is straightforward and predictable. The module does not decide which materials/homework are "more important", does not sort "by its own rules", and does not filter without explicit basis—it returns everything that is linked to the lesson.

## Extension Rule

**Add only complex use cases** where it is really necessary to aggregate data from multiple modules. Do not turn this module into a CRUD layer or a general-purpose data access layer.

## Design Principle: One Endpoint = One Use Case

Each endpoint represents **one specific use case**. The module should not become a generic "get everything" service. Each endpoint has a clear purpose and aggregates data for a specific screen or workflow.

## Current Use Cases

### Use Case #1: Lesson Full Details

**Endpoint**: `GET /api/composition/lessons/{lessonId}/full-details`

**Purpose**: Aggregates all data needed for the "Full Lesson Information" screen in a single request.

**What the endpoint returns**:

1. **Subject Information**
   - Subject name (and all available subject data from SubjectDto)
   - Retrieved via: offering → curriculumSubject → subject

2. **Group Information**
   - Group ID (required)
   - Basic group info (code, name, description) if available
   - Retrieved via: offering → group

3. **Lesson Materials**
   - All materials linked to the lesson
   - Materials come from the document module (reusing existing LessonMaterialDto)
   - Materials are represented so the frontend can render a list of lesson materials (name/type/identifiers/links—within what's already available in document DTO)

4. **Homework Assignments**
   - All homework assignments linked to the specific lesson instance
   - Homework is a business entity from the document module, linked to lessons via a junction table
   - Response includes all homework related to this lesson, with full data already returned by existing homework DTOs

5. **Lesson Instance Details** (required block)
   - **Building**: Where the lesson will take place
   - **Room**: Which classroom/auditorium it will be held in
   - **Teacher**: Who is teaching this lesson
   - **Date and Time**: When the lesson will take place
   - **Offering Information**: Information about the offering this lesson belongs to
   - **Lesson Instance Information**: Information about the specific lesson instance, if offering and specific lesson are represented as different entities

All this data is returned in a single request and is sufficient to display full information about the lesson on the screen.

**Empty Results Handling**: If some data is missing (e.g., no materials or no homework), the endpoint returns a correct "empty result" for the corresponding part, so the UI can render an empty state.

### Use Case #4: Group Subject Info

**Endpoint**: `GET /api/composition/groups/{groupId}/subjects/{subjectId}/info`

**Purpose**: Aggregates all data needed for the teacher's "Group subject info" screen in a single request. Data is returned only if the requester is a teacher assigned to an offering slot for this subject and group (or an administrator).

**Query parameters**: Optional `semesterId`; if omitted, the current semester is used for homework count and attendance.

**What the endpoint returns**:

1. **Subject, group, program** – SubjectDto, StudentGroupDto, ProgramDto
2. **Offering and slots** – GroupSubjectOfferingDto, list of OfferingSlotDto, list of OfferingTeacherItemDto
3. **Curriculum** – CurriculumSubjectDto for this subject, CurriculumDto, and full list of CurriculumSubjectDto (study plan)
4. **Semester** – SemesterDto used for the report period
5. **Total homework count** – Number of homework assignments for this offering in the semester
6. **Students** – For each student in the group: StudentDto, UserDto, total points for the subject, number of submitted homeworks in the semester, and attendance percentage. Attendance rules: LATE is not counted as absence; EXCUSED is counted as absence; percentage = (PRESENT + LATE) / totalSessions × 100 (null if no sessions).

**Authorization**: Requester must be authenticated. Only teachers who are assigned to the offering (main teacher or slot teacher) for this group and subject, or users with ADMIN/MODERATOR/SUPER_ADMIN role, can view.

### Use Case #8: Student Subject Info

**Endpoint**: `GET /api/composition/student/subjects/{offeringId}/info`

**Purpose**: Aggregates all data needed for the student's "Subject detail" screen in a single request. Data is returned only if the requester is a student in the offering's group (or an administrator).

**Query parameters**: Optional `semesterId`; if omitted, the current semester is used for statistics.

**What the endpoint returns**:

1. **Subject** – SubjectDto, resolved department name
2. **Curriculum subject** – CurriculumSubjectDto (duration weeks, hours, credits, semester number)
3. **Offering** – GroupSubjectOfferingDto
4. **Schedule slots** – list of OfferingSlotDto (weekly timetable)
5. **Teachers** – For each teacher assigned to the offering: TeacherDto, UserDto, and role (MAIN, LECTURE, PRACTICE, LAB). Batch-loaded; no N+1.
6. **Student statistics** – StudentSubjectStatsDto:
   - Attendance percentage: (PRESENT + LATE) / totalMarked × 100 (null if no sessions with marks)
   - Submitted homework count: number of distinct homework assignments the student has submitted
   - Total homework count: number of homework assignments for this offering in the semester
   - Total points: sum of all ACTIVE grade entries for this offering

**Authorization**: Requester must be authenticated. Only students who belong to the offering's group, or users with ADMIN/MODERATOR/SUPER_ADMIN role, can view.

## Internal Logic

The endpoint works as follows:

1. Gets basic lesson data by lesson ID
2. Retrieves:
   - Subject name via offering → curriculumSubject → subject
   - Group ID (and minimal group info if available)
   - Information about offering and specific lesson instance
   - Building, room, teacher
   - Date and time of the lesson
   - List of lesson materials
   - List of linked homework assignments

The internal orchestration is simple and predictable: the module does not solve which materials/homework are "more important", does not sort "by its own rules", does not filter without explicit basis—it returns everything linked to the lesson.

## Dependencies

The module depends on:
- `academic` - semesters for teacher student groups and group subject info
- `schedule` - lesson and room information
- `offering` - offering information and offering teachers
- `subject` - subject information
- `group` - group information and group members with users
- `document` - lesson materials and homework (including batch homework IDs by lesson IDs)
- `teacher` - teacher information
- `program` - curriculum subject information
- `auth` - current user for authentication
- `error` - error handling
- `student` - roster by group
- `attendance` - session attendance and group attendance summary
- `grades` - points per student per offering
- `submission` - homework submissions (including batch by homework IDs)
- `user` - UserDto for student display in group subject info
- `department` - department name resolution for subject detail

## Error Handling

All errors are thrown as `AppException` via `Errors` and handled by the global exception handler:
- `NOT_FOUND (404)` - lesson, offering, subject, group, room, teacher, curriculum subject, or semester not found
- `UNAUTHORIZED (401)` - authentication required
- `FORBIDDEN (403)` - requester is not a teacher of the offering (for group subject info / roster attendance / homework submissions)

## Future Extensions

When adding new use cases:

1. **Evaluate necessity**: Only add endpoints for complex scenarios where multiple modules need to be queried
2. **Follow the pattern**: One endpoint = one use case
3. **Reuse DTOs**: Do not create new view DTOs; reuse existing DTOs from source modules
4. **Keep it simple**: No business logic, only data aggregation
5. **Document the use case**: Clearly describe what screen/workflow the endpoint serves
