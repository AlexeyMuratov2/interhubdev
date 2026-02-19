# University schema — логика (3 слоя)

## Цепочка: Program → Curriculum → Curriculum_subject → Offering → Lesson

- **Program** — абстрактная образовательная программа (шаблон). Ссылка на department опциональна.
- **Curriculum** — версия учебного плана для программы (для набора/года). UNIQUE (program_id, version).
- **Curriculum_subject** — предмет в рамках плана: семестр, часы, тип контроля. UNIQUE (curriculum_id, subject_id, semester_no).
- **Student_group** привязана к **program** и **curriculum**; у группы один план, но своя реализация (offering). Состав группы задаётся через **student_group_member** (n:m студент–группа).
- **Group_subject_offering** — как именно группа проходит предмет: преподаватель, аудитория, формат. UNIQUE (group_id, curriculum_subject_id). Отличия группы задаются offering и **group_curriculum_override**.
- **Lesson** — конкретное занятие: дата + timeslot + аудитория (может отличаться от аудитории по умолчанию в offering).

## Каскадность / поведение при удалении

| Таблица / FK | При удалении родителя |
|--------------|------------------------|
| curriculum → program | RESTRICT |
| student_group → program, curriculum | RESTRICT |
| student_group → curator_user (users) | SET NULL |
| student_group_member → student_group, students | CASCADE |
| group_leader → student_group, students | CASCADE |
| curriculum_subject → curriculum, subject, assessment_type | RESTRICT |
| group_subject_offering → student_group | CASCADE |
| group_subject_offering → curriculum_subject, teacher, room | RESTRICT / SET NULL |
| lesson → offering, timeslot, room | CASCADE / RESTRICT / SET NULL |
| group_curriculum_override → student_group | CASCADE |

## Миграции (Flyway)

- **V7** — department, program, assessment_type, subject, curriculum
- **V8** — student_group, students.group_id (deprecated in V20), group_leader
- **V9** — curriculum_subject
- **V10** — room, timeslot
- **V11** — group_subject_offering (offering_teacher удалена в V42)
- **V42** — drop offering_teacher (список преподавателей выводится из main + слотов)
- **V12** — lesson
- **V13** — group_curriculum_override (ENUM: ADD / REMOVE / REPLACE)
- **V19** — student_group.curator_user_id (вместо curator_teacher_id)
- **V20** — student_group_member (n:m студент–группа), удаление students.group_id
