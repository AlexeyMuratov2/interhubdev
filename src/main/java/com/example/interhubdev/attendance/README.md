# Attendance Module

Модуль для фиксации официальной посещаемости студентов по конкретным занятиям (lesson sessions).

## Описание

Модуль Attendance хранит официальные отметки посещаемости, которые ставят преподаватели для студентов по каждому занятию. Каждая запись представляет собой одну отметку на одного студента на одно занятие.

### Статусы посещаемости

- **PRESENT** — присутствовал
- **ABSENT** — отсутствовал
- **LATE** — опоздал (требует указания `minutesLate`)
- **EXCUSED** — отсутствовал по уважительной причине (может содержать `teacherComment`)

### Модель данных

Таблица `attendance_record` содержит:
- `lesson_session_id` — ссылка на занятие (lesson.id)
- `student_id` — ссылка на студента (students.id)
- `status` — статус посещаемости
- `minutes_late` — опционально, только для статуса LATE
- `teacher_comment` — опциональный комментарий преподавателя
- `marked_by` — пользователь, который поставил отметку
- `marked_at` — время отметки
- `updated_at` — время последнего обновления

**Ограничение уникальности:** одна запись на студента на занятие (`UNIQUE(lesson_session_id, student_id)`).

**UNMARKED статус:** если для студента нет записи посещаемости на занятие, это означает, что посещаемость не отмечена (UNMARKED). В ответах API это представлено как `null` в поле `status`.

## Subsystems

### 1. Attendance Records (attendance.records)

Official attendance records marked by teachers for lesson sessions. See main description above.

### 2. Absence Notices (attendance.notice)

Students can submit absence notices to inform teachers about planned absences or lateness for lesson sessions.

#### Features

- **Submit/Update Notice**: Students can submit a notice for a lesson session with:
  - Type: `ABSENT` (will be absent) or `LATE` (will be late)
  - Optional reason text
  - File attachments (0-10 files, stored file IDs from Document module)
- **Cancel Notice**: Students can cancel their active (`SUBMITTED`) notices
- **View Notices**: 
  - Students can view their own notices
  - Teachers can view notices for sessions they teach

#### Data Model

- **absence_notice**: One notice per student per session (only one active `SUBMITTED` notice allowed)
- **absence_notice_attachment**: File attachments linked to notices (stored file IDs from Document module)

#### Status Flow

- `SUBMITTED` → `CANCELED` (by student)
- `SUBMITTED` → `ATTACHED` (reserved for future: link to attendance_record)
- `SUBMITTED` → `ACKNOWLEDGED` (reserved for future: teacher acknowledgment)

#### Access Control

- **Students**: Can create/update/cancel only their own notices
- **Teachers**: Can view notices for sessions they teach (same permission as marking attendance)

#### TODO: Future Extensions

- Integration events: `AbsenceNoticeSubmittedOrUpdated` event for notifications
- Link notices to attendance records (`attached_record_id` field prepared)
- Batch validation of file IDs via DocumentApi (optional, currently only format validation)

## API Endpoints

### Attendance Records

### 1. Bulk mark attendance
```
POST /api/attendance/sessions/{sessionId}/records/bulk
```
Отметить посещаемость для нескольких студентов сразу. Транзакция all-or-nothing.

### 2. Single mark attendance
```
PUT /api/attendance/sessions/{sessionId}/students/{studentId}
```
Отметить или обновить посещаемость для одного студента.

### 3. Get session attendance
```
GET /api/attendance/sessions/{sessionId}
```
Получить список посещаемости по занятию с подсчетами и статусами для всех студентов из ростер группы.

### 4. Get student attendance
```
GET /api/attendance/students/{studentId}?from=...&to=...&offeringId=...&groupId=...
```
Получить историю посещаемости студента с агрегатами.

### 5. Get group attendance summary
```
GET /api/attendance/groups/{groupId}/summary?from=...&to=...&offeringId=...
```
Получить сводку посещаемости по группе (подсчеты по студентам).

### Absence Notices (Student Operations)

### 6. Submit or update absence notice
```
POST /api/attendance/notices
Body: { lessonSessionId, type (ABSENT|LATE), reasonText?, fileIds[] }
```
Создать новое уведомление об отсутствии/опоздании или обновить существующее активное уведомление для занятия.

### 7. Cancel absence notice
```
POST /api/attendance/notices/{id}/cancel
```
Отменить активное уведомление (только статус SUBMITTED).

### 8. Get my absence notices
```
GET /api/attendance/notices/mine?from=...&to=...
```
Получить список своих уведомлений об отсутствии за период.

### Absence Notices (Teacher Operations)

### 9. Get session absence notices
```
GET /api/attendance/sessions/{sessionId}/notices?includeCanceled=false
```
Получить список уведомлений об отсутствии для занятия (по умолчанию только активные SUBMITTED).

## Права доступа

- **Отметка посещаемости:** TEACHER (только для своих занятий) или ADMIN/MODERATOR/SUPER_ADMIN
- **Чтение по занятию:** TEACHER (только для своих занятий) или ADMIN/MODERATOR/SUPER_ADMIN
- **Чтение по студенту:** STUDENT (только свои записи) или TEACHER/ADMIN/MODERATOR/SUPER_ADMIN
- **Сводка по группе:** TEACHER (для групп, которые они ведут) или ADMIN/MODERATOR/SUPER_ADMIN

## Точки расширения для будущих событий

В коде модуля есть TODO комментарии в местах, где в будущем должны появиться события интеграции:

1. **AttendanceServiceImpl.markAttendanceBulk()** — после сохранения записей:
   ```java
   // TODO: publish IntegrationEvent AttendanceMarked for each record
   // IntegrationEvent event = new AttendanceMarkedEvent(recordId, sessionId, studentId, status, markedBy, markedAt);
   // eventPublisher.publish(event);
   ```

2. **AttendanceServiceImpl.markAttendanceSingle()** — после сохранения записи:
   ```java
   // TODO: publish IntegrationEvent AttendanceMarked
   // IntegrationEvent event = new AttendanceMarkedEvent(saved.getId(), sessionId, studentId, status, markedBy, now);
   // eventPublisher.publish(event);
   ```

Эти события могут использоваться для:
- Отправки уведомлений студентам/преподавателям
- Push-уведомлений
- Интеграции с внешними системами
- Аналитики и отчетности

## Подсистема Absence Notices

### Описание

Студенты могут отправлять уведомления о планируемом отсутствии или опоздании на занятия. Уведомления могут содержать причину и прикрепленные файлы (обычно фото справок).

### Модель данных

- **absence_notice**: Уведомление студента о пропуске/опоздании
  - Одно активное (`SUBMITTED`) уведомление на студента на занятие
  - Тип: `ABSENT` (отсутствие) или `LATE` (опоздание)
  - Опциональная причина (текст)
  - Статусы: `SUBMITTED`, `CANCELED`, `ACKNOWLEDGED` (зарезервировано), `ATTACHED` (зарезервировано для связи с attendance_record)

- **absence_notice_attachment**: Прикрепленные файлы
  - Ссылки на файлы из модуля Document (storedFileId)
  - До 10 файлов на уведомление
  - Уникальность: (notice_id, file_id)

### Права доступа

- **Студенты**: Могут создавать/обновлять/отменять только свои уведомления
- **Преподаватели**: Могут просматривать уведомления для занятий, которые они ведут

### TODO: События и уведомления

В коде use-case `SubmitOrUpdateAbsenceNoticeUseCase` есть TODO комментарии:

```java
// TODO: publish IntegrationEvent AbsenceNoticeSubmittedOrUpdated {
//   noticeId, sessionId, studentId, type, submittedAt/updatedAt
// }
// TODO: notify teachers of session via Notification module (future outbox)
```

Эти события могут использоваться для:
- Уведомления преподавателей о новых уведомлениях студентов
- Push-уведомлений
- Интеграции с внешними системами

### Будущие функции (не реализованы)

- **Связь с attendance_record**: Поле `attached_record_id` подготовлено, но логика привязки не реализована
- **Подтверждение преподавателем**: Статус `ACKNOWLEDGED` зарезервирован для будущего использования
- **Батч-валидация fileIds**: Опциональная проверка существования файлов через DocumentApi (сейчас только валидация формата)

## Будущие функции (не реализованы)

- **Автоматический расчет процента посещаемости:** агрегаты по студентам/группам за период
- **История изменений:** аудит изменений отметок посещаемости

## Зависимости модуля

- `schedule` — получение информации о занятиях
- `offering` — получение информации о предложениях (groupId, teacherIds)
- `student` — получение ростеров групп, валидация студентов
- `group` — валидация групп
- `teacher` — получение профилей преподавателей для проверки прав
- `auth` — текущий пользователь
- `user` — роли пользователей
- `error` — обработка ошибок
- `document` — хранилище файлов (для вложений absence notices; хранятся только ID файлов, валидация опциональна)