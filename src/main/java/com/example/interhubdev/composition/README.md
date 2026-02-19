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
- `schedule` - lesson and room information
- `offering` - offering information and offering teachers
- `subject` - subject information
- `group` - group information
- `document` - lesson materials and homework
- `teacher` - teacher information
- `program` - curriculum subject information
- `auth` - current user for authentication
- `error` - error handling

## Error Handling

All errors are thrown as `AppException` via `Errors` and handled by the global exception handler:
- `NOT_FOUND (404)` - lesson, offering, subject, group, room, teacher, or curriculum subject not found
- `UNAUTHORIZED (401)` - authentication required

## Future Extensions

When adding new use cases:

1. **Evaluate necessity**: Only add endpoints for complex scenarios where multiple modules need to be queried
2. **Follow the pattern**: One endpoint = one use case
3. **Reuse DTOs**: Do not create new view DTOs; reuse existing DTOs from source modules
4. **Keep it simple**: No business logic, only data aggregation
5. **Document the use case**: Clearly describe what screen/workflow the endpoint serves
