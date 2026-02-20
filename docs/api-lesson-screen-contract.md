# Контракт API: экран урока (посещаемость и баллы)

Документ-контракт для фронтенда по реализации фич **экрана урока**: (1) таблица посещаемости всех студентов группы с заявками на пропуск, (2) отметка посещаемости студентами занятия, (3) проставление баллов. Описаны эндпоинты модулей **Composition**, **Attendance** и **Grades**, необходимые для отрисовки и взаимодействия с этим экраном.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|------------|
| `/api` | Общий префикс REST API |
| `/api/composition` | Агрегирующие эндпоинты (ростер посещаемости по уроку с данными для UI) |
| `/api/attendance` | Посещаемость: получение данных по занятию, массовая и одиночная отметка |
| `/api/grades` | Баллы: создание/обновление записей, итоги по студенту и по группе |

Хост и порт задаются окружением развёртывания.

### 1.2 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- **Таблица посещаемости (Composition):** доступ имеют пользователи, которые являются **преподавателем этого занятия** (привязаны к offering урока) или **ADMIN**, **MODERATOR**, **SUPER_ADMIN**. Иначе — `403 Forbidden`.
- **Отметка посещаемости (Attendance):** те же роли — преподаватель занятия или администратор. Студент не может отмечать посещаемость.
- **Проставление баллов (Grades):** **TEACHER** или **ADMIN** (и при необходимости привязка к offering/группе по логике модуля). Иначе — `403 Forbidden`.
- При отсутствии или невалидном JWT возвращается `401 Unauthorized` (код `UNAUTHORIZED`).

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID.
- **Даты и время** — строки **ISO-8601** (например `"2025-02-20T12:00:00"`, дата `"2025-02-20"`). Время урока — **HH:mm:ss** (например `"13:00:00"`).
- **Числа** — JSON number; для баллов может использоваться число с дробной частью (BigDecimal).
- **null** — явное отсутствие значения. Optional-поля в JSON могут быть `null`.
- **Перечисления (enum)** в JSON передаются строкой: например `PRESENT`, `ABSENT`, `SEMINAR`, `HOMEWORK`.

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (см. сводную таблицу в конце документа) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для валидации — объект «имя поля → сообщение» |

Пример (401):

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-02-20T15:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных

### 2.1 LessonRosterAttendanceDto (ростер посещаемости по уроку)

Ответ эндпоинта **GET /api/composition/lessons/{lessonId}/roster-attendance**. Содержит всё необходимое для отрисовки таблицы посещаемости на экране урока: урок, группа, название предмета, счётчики и по каждой строке — студент, статус посещаемости и заявки на пропуск.

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object | Информация об уроке — **LessonDto** (см. п. 2.2) |
| `group` | object | Информация о группе — **StudentGroupDto** (см. п. 2.3) |
| `subjectName` | string | Название предмета для заголовка (например, "Mathematics") |
| `counts` | object | Карта «статус посещаемости → количество»: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `unmarkedCount` | integer | Количество студентов без отметки посещаемости |
| `rows` | array of **LessonRosterAttendanceRowDto** | Одна строка на студента в группе (порядок — ростер группы) |

### 2.2 LessonRosterAttendanceRowDto (строка таблицы посещаемости)

| Поле | Тип | Описание |
|------|-----|----------|
| `student` | object | Данные студента для отображения — **StudentDto** (см. п. 2.4) |
| `status` | string \| null | Статус посещаемости: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`; `null` — не отмечен |
| `minutesLate` | integer \| null | Минуты опоздания (при status = LATE) |
| `teacherComment` | string \| null | Комментарий преподавателя к записи посещаемости |
| `markedAt` | string \| null | Дата и время отметки (ISO-8601) |
| `markedBy` | UUID \| null | Id пользователя, отметившего посещаемость |
| `attachedAbsenceNoticeId` | UUID \| null | Id заявки на пропуск, привязанной к этой записи (если есть) |
| `notices` | array of **StudentNoticeDto** | Все заявки на пропуск этого студента по этому уроку (см. п. 2.5) |
| `lessonPoints` | number | Сумма баллов, поставленных именно по этому уроку (сумма активных записей баллов с привязкой к данному занятию). 0, если по уроку баллов нет. |

### 2.3 LessonDto (урок)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id урока (lesson session) |
| `offeringId` | UUID | Id офферинга |
| `offeringSlotId` | UUID \| null | Id слота офферинга |
| `date` | string | Дата урока (ISO-8601 date) |
| `startTime` | string | Время начала (HH:mm:ss) |
| `endTime` | string | Время окончания (HH:mm:ss) |
| `timeslotId` | UUID \| null | Id шаблона времени |
| `roomId` | UUID \| null | Id аудитории |
| `topic` | string \| null | Тема урока |
| `status` | string \| null | Статус: `PLANNED`, `CANCELLED`, `DONE` или `null` |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.4 StudentGroupDto (группа)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id группы |
| `programId` | UUID | Id образовательной программы |
| `curriculumId` | UUID | Id учебного плана |
| `code` | string \| null | Код группы |
| `name` | string \| null | Название группы |
| `description` | string \| null | Описание |
| `startYear` | integer | Год начала |
| `graduationYear` | integer \| null | Год выпуска |
| `curatorUserId` | UUID \| null | Id куратора |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.5 StudentDto (профиль студента)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля студента |
| `userId` | UUID | Id пользователя (аккаунта) |
| `studentId` | string \| null | Университетский номер студента |
| `chineseName` | string \| null | Имя на китайском |
| `faculty` | string \| null | Факультет |
| `course` | string \| null | Курс |
| `enrollmentYear` | integer \| null | Год поступления |
| `groupName` | string \| null | Название группы из профиля |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.6 StudentNoticeDto (краткая сводка заявки на пропуск в контексте занятия)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id уведомления |
| `type` | string | Тип: `ABSENT` или `LATE` |
| `status` | string | Статус: `SUBMITTED`, `CANCELED`, `APPROVED`, `REJECTED` и др. |
| `reasonText` | string \| null | Причина (если указана) |
| `submittedAt` | string | Дата и время подачи (ISO-8601) |
| `fileIds` | array of string | Список id прикреплённых файлов (Document module) |

### 2.7 AttendanceRecordDto (запись посещаемости)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id записи |
| `lessonSessionId` | UUID | Id занятия |
| `studentId` | UUID | Id студента |
| `status` | string | Статус: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `minutesLate` | integer \| null | Минуты опоздания (при LATE) |
| `teacherComment` | string \| null | Комментарий преподавателя |
| `markedBy` | UUID | Id пользователя, отметившего посещаемость |
| `markedAt` | string | Дата и время отметки (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |
| `absenceNoticeId` | UUID \| null | Id привязанной заявки на пропуск (если есть) |

### 2.8 GradeEntryDto (запись баллов)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id записи |
| `studentId` | UUID | Id студента |
| `offeringId` | UUID | Id офферинга |
| `points` | number | Баллы (от -9999.99 до 9999.99) |
| `typeCode` | string | Код типа: `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM` |
| `typeLabel` | string \| null | Подпись типа (обязательна при typeCode = CUSTOM) |
| `description` | string \| null | Описание |
| `lessonSessionId` | UUID \| null | Id занятия (привязка к уроку) |
| `homeworkSubmissionId` | UUID \| null | Id сдачи ДЗ (привязка к домашнему заданию) |
| `gradedBy` | UUID | Id пользователя, выставившего баллы |
| `gradedAt` | string | Дата и время выставления (ISO-8601) |
| `status` | string | Статус: `ACTIVE` или `VOIDED` |

### 2.9 StudentOfferingGradesDto (баллы студента по офферингу)

| Поле | Тип | Описание |
|------|-----|----------|
| `studentId` | UUID | Id студента |
| `offeringId` | UUID | Id офферинга |
| `entries` | array of **GradeEntryDto** | Список записей (с учётом фильтров from/to и includeVoided) |
| `totalPoints` | number | Сумма баллов по активным записям |
| `breakdownByType` | object | Карта «typeCode → сумма баллов» по активным записям |

### 2.10 GroupOfferingSummaryDto (сводка баллов группы по офферингу)

| Поле | Тип | Описание |
|------|-----|----------|
| `groupId` | UUID | Id группы |
| `offeringId` | UUID | Id офферинга |
| `rows` | array of **GroupOfferingSummaryRow** | Одна строка на студента группы |

**GroupOfferingSummaryRow:**

| Поле | Тип | Описание |
|------|-----|----------|
| `studentId` | UUID | Id студента |
| `totalPoints` | number | Сумма баллов по активным записям |
| `breakdownByType` | object | Карта «typeCode → сумма баллов» |

---

## 3. Данные для таблицы посещаемости (Composition)

Эндпоинт возвращает агрегированные данные для отрисовки таблицы посещаемости на экране урока: все студенты группы, их статус посещаемости по этому уроку, заявки на пропуск и **баллы, поставленные именно по этому уроку** (поле `lessonPoints` в каждой строке — сумма активных записей баллов с привязкой к данному занятию). Одного запроса достаточно для заполнения таблицы без дополнительных вызовов по студентам, заявкам или оценкам.

### 3.1 GET /api/composition/lessons/{lessonId}/roster-attendance

**Назначение:** получить для экрана урока полный ростер посещаемости: урок, группа, название предмета, счётчики по статусам и по каждому студенту группы — данные для отображения (ФИО, номер, факультет и т.д.), статус посещаемости по этому занятию, список заявок на пропуск и **сумма баллов по этому уроку** (`lessonPoints`). Порядок строк совпадает с ростером группы.

**Метод и путь:** `GET /api/composition/lessons/{lessonId}/roster-attendance`

**Права доступа:** преподаватель этого занятия (привязан к offering урока) или ADMIN, MODERATOR, SUPER_ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (lesson session) |

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `includeCanceled` | boolean | нет | По умолчанию `false`. Если `true`, в массив `notices` у каждого студента включаются и отменённые заявки (CANCELED). |

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK` — объект **LessonRosterAttendanceDto** (см. п. 2.1).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|-----------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Пользователь не преподаватель этого занятия | Сообщение от модуля attendance |
| 404 | NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | NOT_FOUND | Офферинг или группа не найдены | `"Offering not found: ..."`, `"Group not found: ..."` |

---

## 4. Отметка посещаемости (Attendance)

Эндпоинты для получения данных по занятию и для проставления отметок посещаемости (одним студентом или списком). Используются на экране урока вместе с данными из п. 3.

### 4.1 GET /api/attendance/sessions/{sessionId}

**Назначение:** получить посещаемость по занятию: счётчики по статусам и список студентов с их статусами и заявками на пропуск. Альтернатива агрегату из Composition: здесь в каждой строке только `studentId`, без полей студента (имя, номер и т.д.). Для экрана урока предпочтительно использовать **GET /api/composition/lessons/{lessonId}/roster-attendance** (п. 3.1), где уже есть полные данные студента.

**Метод и путь:** `GET /api/attendance/sessions/{sessionId}`

**Права доступа:** преподаватель этого занятия или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | Id занятия (lesson session) |

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `includeCanceled` | boolean | нет | По умолчанию `false`. Включать ли отменённые заявки в списке уведомлений по студентам. |

**Успешный ответ:** `200 OK` — объект **SessionAttendanceDto**:

| Поле | Тип | Описание |
|------|-----|----------|
| `sessionId` | UUID | Id занятия |
| `counts` | object | Карта «статус → количество» (PRESENT, ABSENT, LATE, EXCUSED) |
| `unmarkedCount` | integer | Количество без отметки |
| `students` | array of **SessionAttendanceStudentDto** | Список: studentId, status, minutesLate, teacherComment, markedAt, markedBy, absenceNoticeId, notices (массив **StudentNoticeDto**) |

**Ошибки:** 401 UNAUTHORIZED, 403 ATTENDANCE_FORBIDDEN, 404 ATTENDANCE_LESSON_NOT_FOUND (или ATTENDANCE_OFFERING_NOT_FOUND).

---

### 4.2 POST /api/attendance/sessions/{sessionId}/records/bulk

**Назначение:** массово отметить посещаемость по занятию для нескольких студентов. Транзакция «всё или ничего»: при ошибке валидации по любому элементу откатывается вся операция.

**Метод и путь:** `POST /api/attendance/sessions/{sessionId}/records/bulk`

**Права доступа:** преподаватель этого занятия или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | Id занятия (lesson session) |

**Тело запроса:** объект с полем `items` — массив **MarkAttendanceItem**.

**MarkAttendanceItem:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | да | Id студента (профиль студента) |
| `status` | string | да | Статус: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `minutesLate` | integer | нет | Минуты опоздания (опционально; только при status = LATE, если указано — число >= 0; при других статусах — null) |
| `teacherComment` | string | нет | Комментарий преподавателя (макс. 2000 символов) |
| `absenceNoticeId` | UUID | нет | Явная привязка заявки на пропуск к записи (должна относиться к этому занятию и студенту, не CANCELED). Взаимоисключающе с `autoAttachLastNotice`. |
| `autoAttachLastNotice` | boolean | нет | Если `true`, автоматически привязать последнюю поданную заявку этого студента по этому занятию. Взаимоисключающе с `absenceNoticeId`. |

**Успешный ответ:** `201 Created` — массив **AttendanceRecordDto** (созданные или обновлённые записи).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|-----------------|
| 400 | ATTENDANCE_VALIDATION_FAILED | Неверный status/minutesLate (например minutesLate < 0 или не null при статусе не LATE), комментарий превышает лимит |
| 400 | ATTENDANCE_STUDENT_NOT_IN_GROUP | Студент не в группе этого занятия |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | ATTENDANCE_FORBIDDEN | Пользователь не преподаватель занятия |
| 404 | ATTENDANCE_LESSON_NOT_FOUND | Занятие не найдено |
| 404 | ATTENDANCE_STUDENT_NOT_FOUND | Студент не найден |
| 404 | ATTENDANCE_OFFERING_NOT_FOUND | Офферинг не найден |

При указании `absenceNoticeId` дополнительно: 404 ATTENDANCE_NOTICE_NOT_FOUND, 400 ATTENDANCE_NOTICE_CANCELED, ATTENDANCE_NOTICE_DOES_NOT_MATCH_RECORD.

---

### 4.3 PUT /api/attendance/sessions/{sessionId}/students/{studentId}

**Назначение:** отметить или обновить посещаемость одного студента по занятию. Используется при редактировании одной строки в таблице посещаемости на экране урока.

**Метод и путь:** `PUT /api/attendance/sessions/{sessionId}/students/{studentId}`

**Права доступа:** преподаватель этого занятия или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | Id занятия |
| `studentId` | UUID | Id студента (профиль студента) |

**Тело запроса:** объект **MarkAttendanceRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `status` | string | да | `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `minutesLate` | integer | нет | Опционально; только при status = LATE, если указано — число >= 0 |
| `teacherComment` | string | нет | Макс. 2000 символов |
| `absenceNoticeId` | UUID | нет | Привязать указанную заявку (взаимоисключающе с autoAttachLastNotice) |
| `autoAttachLastNotice` | boolean | нет | Автопривязка последней поданной заявки студента по этому занятию |

**Успешный ответ:** `200 OK` — объект **AttendanceRecordDto**.

**Ошибки:** те же, что в п. 4.2 (для одного студента).

---

## 5. Проставление баллов (Grades)

Эндпоинты для создания, обновления и аннулирования записей баллов, а также для получения итогов по студенту и по группе по офферингу. На экране урока используются при отображении/редактировании баллов за занятие или за тип работы (семинар, ДЗ и т.д.).

### 5.1 POST /api/grades/entries

**Назначение:** создать одну запись баллов (одному студенту по офферингу). Можно привязать к занятию (`lessonSessionId`) и/или к сдаче ДЗ (`homeworkSubmissionId`).

**Метод и путь:** `POST /api/grades/entries`

**Права доступа:** TEACHER или ADMIN.

**Тело запроса:** объект **CreateGradeEntryRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | да | Id студента |
| `offeringId` | UUID | да | Id офферинга |
| `points` | number | да | Баллы (от -9999.99 до 9999.99) |
| `typeCode` | string | да | `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM` |
| `typeLabel` | string | нет | Подпись типа (макс. 255 символов); при typeCode = CUSTOM обязательна |
| `description` | string | нет | Описание (макс. 2000 символов) |
| `lessonSessionId` | UUID | нет | Привязка к занятию |
| `homeworkSubmissionId` | UUID | нет | Привязка к сдаче ДЗ |
| `gradedAt` | string (ISO-8601) | нет | Дата/время выставления; при отсутствии — текущее время |

**Успешный ответ:** `201 Created` — объект **GradeEntryDto** (статус ACTIVE).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|-----------------|
| 400 | GRADE_VALIDATION_FAILED | typeCode = CUSTOM без typeLabel; points вне диапазона; превышение длины полей |
| 400 | GRADE_OFFERING_NOT_FOR_GROUP | Офферинг не принадлежит группе студента |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Пользователь не учитель и не администратор |
| 404 | GRADE_OFFERING_NOT_FOUND | Офферинг не найден |
| 404 | GRADE_STUDENT_NOT_FOUND | Студент не найден |

---

### 5.2 POST /api/grades/entries/bulk

**Назначение:** массово создать записи баллов по одному офферингу для нескольких студентов. Общие поля (typeCode, typeLabel, description, lessonSessionId, gradedAt) задаются один раз; в каждом элементе — studentId, points и опционально homeworkSubmissionId.

**Метод и путь:** `POST /api/grades/entries/bulk`

**Права доступа:** TEACHER или ADMIN.

**Тело запроса:** объект **BulkCreateGradeEntriesRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `offeringId` | UUID | да | Id офферинга |
| `typeCode` | string | да | Код типа (SEMINAR, EXAM, …) |
| `typeLabel` | string | нет | Подпись типа (при CUSTOM обязательна) |
| `description` | string | нет | Описание |
| `lessonSessionId` | UUID | нет | Привязка к занятию для всех записей |
| `gradedAt` | string (ISO-8601) | нет | Дата/время выставления |
| `items` | array of **BulkGradeItem** | да | Не пустой массив |

**BulkGradeItem:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | да | Id студента |
| `points` | number | да | Баллы (от -9999.99 до 9999.99) |
| `homeworkSubmissionId` | UUID | нет | Привязка к сдаче ДЗ для этого студента |

**Успешный ответ:** `201 Created` — массив **GradeEntryDto**.

**Ошибки:** те же, что в п. 5.1; при невалидном элементе в `items` — 400 GRADE_VALIDATION_FAILED или 404 по студенту/офферингу.

---

### 5.3 PUT /api/grades/entries/{id}

**Назначение:** обновить запись баллов (баллы, тип, описание, привязки к занятию/ДЗ, дата выставления). Допустимо только для записей в статусе ACTIVE.

**Метод и путь:** `PUT /api/grades/entries/{id}`

**Права доступа:** TEACHER или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id записи баллов |

**Тело запроса:** объект **UpdateGradeEntryRequest** (все поля опциональны; переданные перезаписывают существующие):

| Поле | Тип | Описание |
|------|-----|----------|
| `points` | number | Баллы (от -9999.99 до 9999.99) |
| `typeCode` | string | Код типа |
| `typeLabel` | string | Подпись типа |
| `description` | string | Описание |
| `lessonSessionId` | UUID | Привязка к занятию |
| `homeworkSubmissionId` | UUID | Привязка к сдаче ДЗ |
| `gradedAt` | string (ISO-8601) | Дата/время выставления |

**Успешный ответ:** `200 OK` — объект **GradeEntryDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|-----------------|
| 400 | GRADE_ENTRY_VOIDED | Запись в статусе VOIDED (аннулирована) |
| 400 | GRADE_VALIDATION_FAILED | Неверные значения полей |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Нет прав |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись не найдена |

---

### 5.4 DELETE /api/grades/entries/{id}

**Назначение:** аннулировать запись баллов (soft-delete: статус VOIDED). Такая запись не участвует в итогах и не обновляется.

**Метод и путь:** `DELETE /api/grades/entries/{id}`

**Права доступа:** TEACHER или ADMIN.

**Параметры пути:** `id` — UUID записи.

**Тело запроса:** не используется.

**Успешный ответ:** `204 No Content` (тело пустое).

**Ошибки:** 401 UNAUTHORIZED, 403 GRADE_FORBIDDEN, 404 GRADE_ENTRY_NOT_FOUND.

---

### 5.5 GET /api/grades/entries/{id}

**Назначение:** получить одну запись баллов по id.

**Метод и путь:** `GET /api/grades/entries/{id}`

**Права доступа:** TEACHER или ADMIN (по логике модуля grades).

**Успешный ответ:** `200 OK` — объект **GradeEntryDto**. При отсутствии записи — `404 Not Found`.

---

### 5.6 GET /api/grades/students/{studentId}/offerings/{offeringId}

**Назначение:** получить баллы студента по офферингу: список записей, сумма баллов и разбивка по типам. Для экрана урока можно использовать для отображения баллов одного студента по текущему предмету (offeringId берётся из урока).

**Метод и путь:** `GET /api/grades/students/{studentId}/offerings/{offeringId}`

**Права доступа:** TEACHER или ADMIN (и по логике модуля — доступ к этому студенту/офферингу).

**Параметры пути:** `studentId`, `offeringId` — UUID.

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `from` | string (ISO-8601 datetime) | нет | Фильтр: gradedAt >= from |
| `to` | string (ISO-8601 datetime) | нет | Фильтр: gradedAt <= to |
| `includeVoided` | boolean | нет | По умолчанию `false`. Включать ли аннулированные записи в список и в итогах. |

**Успешный ответ:** `200 OK` — объект **StudentOfferingGradesDto** (п. 2.9).

**Ошибки:** 401 UNAUTHORIZED, 403 GRADE_FORBIDDEN, 404 GRADE_STUDENT_NOT_FOUND, GRADE_OFFERING_NOT_FOUND.

---

### 5.7 GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary

**Назначение:** получить сводку баллов по группе и офферингу: по каждому студенту группы — сумма баллов и разбивка по типам. Удобно для экрана урока: группа и offering известны из урока, одним запросом получаем баллы всех студентов по этому предмету.

**Метод и путь:** `GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary`

**Права доступа:** TEACHER или ADMIN (и доступ к группе/офферингу).

**Параметры пути:** `groupId`, `offeringId` — UUID.

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `from` | string (ISO-8601 datetime) | нет | Фильтр: gradedAt >= from |
| `to` | string (ISO-8601 datetime) | нет | Фильтр: gradedAt <= to |
| `includeVoided` | boolean | нет | По умолчанию `false`. Учитывать ли аннулированные записи в итогах. |

**Успешный ответ:** `200 OK` — объект **GroupOfferingSummaryDto** (п. 2.10).

**Ошибки:** 401 UNAUTHORIZED, 403 GRADE_FORBIDDEN, 404 GRADE_GROUP_NOT_FOUND, GRADE_OFFERING_NOT_FOUND, 400 GRADE_OFFERING_NOT_FOR_GROUP (офферинг не принадлежит группе).

---

### 5.8 PUT /api/grades/lessons/{lessonId}/students/{studentId}/points

**Назначение:** установить или заменить баллы одного студента за один урок (одна ячейка в таблице на экране урока). Если у студента уже есть активные записи баллов, привязанные к этому уроку, их сумма заменяется на переданное значение (первая запись обновляется, остальные аннулируются). Если записей не было — создаётся одна новая запись с указанными баллами. Упрощает UX: не нужно создавать/обновлять запись вручную через общий CRUD.

**Метод и путь:** `PUT /api/grades/lessons/{lessonId}/students/{studentId}/points`

**Права доступа:** TEACHER или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (lesson session) |
| `studentId` | UUID | Id студента (профиль студента) |

**Тело запроса:** объект **SetLessonPointsRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `points` | number | да | Баллы (от -9999.99 до 9999.99) — новое значение для этого урока |

**Успешный ответ:** `200 OK` — объект **GradeEntryDto** (запись, которая теперь хранит баллы за этот урок: созданная или обновлённая).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|-----------------|
| 400 | GRADE_VALIDATION_FAILED | Баллы вне допустимого диапазона |
| 400 | GRADE_STUDENT_NOT_IN_GROUP | Студент не в группе этого урока |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Нет прав на управление баллами |
| 404 | GRADE_LESSON_NOT_FOUND | Урок не найден |
| 404 | GRADE_OFFERING_NOT_FOUND | Офферинг урока не найден |
| 404 | GRADE_STUDENT_NOT_FOUND | Студент не найден |

---

## 6. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|-----------------|
| 400 | ATTENDANCE_VALIDATION_FAILED | Неверные status/minutesLate (minutesLate &lt; 0 или не null при статусе не LATE)/comment или привязка заявки |
| 400 | ATTENDANCE_STUDENT_NOT_IN_GROUP | Студент не в группе занятия |
| 400 | GRADE_VALIDATION_FAILED | Неверные баллы, тип (CUSTOM без typeLabel), длина полей |
| 400 | GRADE_OFFERING_NOT_FOR_GROUP | Офферинг не принадлежит группе студента/группе в summary |
| 400 | GRADE_ENTRY_VOIDED | Попытка обновить аннулированную запись баллов |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN, ATTENDANCE_FORBIDDEN | Нет прав на просмотр/отметку посещаемости по занятию |
| 403 | GRADE_FORBIDDEN | Нет прав на управление баллами |
| 404 | NOT_FOUND | Урок, офферинг, группа, предмет не найдены (Composition) |
| 404 | ATTENDANCE_LESSON_NOT_FOUND | Занятие не найдено |
| 404 | ATTENDANCE_OFFERING_NOT_FOUND | Офферинг не найден |
| 404 | ATTENDANCE_STUDENT_NOT_FOUND | Студент не найден |
| 404 | ATTENDANCE_NOTICE_NOT_FOUND | Заявка на пропуск не найдена |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись баллов не найдена |
| 404 | GRADE_OFFERING_NOT_FOUND | Офферинг не найден |
| 404 | GRADE_STUDENT_NOT_FOUND | Студент не найден |
| 404 | GRADE_GROUP_NOT_FOUND | Группа не найдена |
| 404 | GRADE_LESSON_NOT_FOUND | Урок не найден (для set-points-for-lesson) |
| 400 | GRADE_STUDENT_NOT_IN_GROUP | Студент не в группе урока (для set-points-for-lesson) |

---

## 7. Примеры запросов и ответов

### 7.1 Получение ростера посещаемости по уроку

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/roster-attendance?includeCanceled=false
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
{
  "lesson": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "offeringId": "660e8400-e29b-41d4-a716-446655440001",
    "offeringSlotId": "770e8400-e29b-41d4-a716-446655440002",
    "date": "2025-02-20",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "timeslotId": null,
    "roomId": "990e8400-e29b-41d4-a716-446655440004",
    "topic": "Algorithms",
    "status": "PLANNED",
    "createdAt": "2025-02-01T10:00:00",
    "updatedAt": "2025-02-01T10:00:00"
  },
  "group": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "programId": "d4e5f6a7-b8c9-0123-def0-234567890123",
    "curriculumId": "e5f6a7b8-c9d0-1234-ef01-345678901234",
    "code": "CS-2024-1",
    "name": "Group A",
    "description": null,
    "startYear": 2024,
    "graduationYear": null,
    "curatorUserId": null,
    "createdAt": "2024-09-01T00:00:00",
    "updatedAt": "2024-09-01T00:00:00"
  },
  "subjectName": "Introduction to Algorithms",
  "counts": {
    "PRESENT": 18,
    "ABSENT": 1,
    "LATE": 1,
    "EXCUSED": 0
  },
  "unmarkedCount": 2,
  "rows": [
    {
      "student": {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "studentId": "2024001",
        "chineseName": "张三",
        "faculty": "CS",
        "course": "1",
        "enrollmentYear": 2024,
        "groupName": "Group A",
        "createdAt": "2024-09-01T00:00:00",
        "updatedAt": "2024-09-01T00:00:00"
      },
      "status": "PRESENT",
      "minutesLate": null,
      "teacherComment": null,
      "markedAt": "2025-02-20T13:05:00",
      "markedBy": "12345678-1234-1234-1234-123456789abc",
      "attachedAbsenceNoticeId": null,
      "notices": [],
      "lessonPoints": 8.5
    },
    {
      "student": {
        "id": "c3d4e5f6-a7b8-9012-cdef-123456789002",
        "userId": "d4e5f6a7-b8c9-0123-def0-234567890102",
        "studentId": "2024002",
        "chineseName": "李四",
        "faculty": "CS",
        "course": "1",
        "enrollmentYear": 2024,
        "groupName": "Group A",
        "createdAt": "2024-09-01T00:00:00",
        "updatedAt": "2024-09-01T00:00:00"
      },
      "status": "LATE",
      "minutesLate": 15,
      "teacherComment": null,
      "markedAt": "2025-02-20T13:20:00",
      "markedBy": "12345678-1234-1234-1234-123456789abc",
      "attachedAbsenceNoticeId": "e5f6a7b8-c9d0-1234-ef01-456789012345",
      "notices": [
        {
          "id": "e5f6a7b8-c9d0-1234-ef01-456789012345",
          "type": "LATE",
          "status": "SUBMITTED",
          "reasonText": "Transport delay",
          "submittedAt": "2025-02-20T12:50:00",
          "fileIds": []
        }
      ],
      "lessonPoints": 7.0
    }
  ]
}
```

### 7.2 Отметка посещаемости одного студента

**Запрос:**

```
PUT /api/attendance/sessions/550e8400-e29b-41d4-a716-446655440000/students/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "status": "EXCUSED",
  "minutesLate": null,
  "teacherComment": "Medical certificate provided",
  "absenceNoticeId": "e5f6a7b8-c9d0-1234-ef01-456789012345",
  "autoAttachLastNotice": false
}
```

**Ответ:** `200 OK`

```json
{
  "id": "f1e2d3c4-b5a6-9870-5432-109876543210",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "EXCUSED",
  "minutesLate": null,
  "teacherComment": "Medical certificate provided",
  "markedBy": "12345678-1234-1234-1234-123456789abc",
  "markedAt": "2025-02-20T13:25:00",
  "updatedAt": "2025-02-20T13:25:00",
  "absenceNoticeId": "e5f6a7b8-c9d0-1234-ef01-456789012345"
}
```

### 7.3 Массовое создание баллов по занятию

**Запрос:**

```
POST /api/grades/entries/bulk
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "typeCode": "SEMINAR",
  "typeLabel": null,
  "description": "Seminar activity Feb 20",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "gradedAt": "2025-02-20T14:30:00",
  "items": [
    { "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "points": 8.5, "homeworkSubmissionId": null },
    { "studentId": "c3d4e5f6-a7b8-9012-cdef-123456789002", "points": 7.0, "homeworkSubmissionId": null }
  ]
}
```

**Ответ:** `201 Created` — массив **GradeEntryDto** (по одному на каждый элемент `items`).

### 7.4 Сводка баллов группы по офферингу

**Запрос:**

```
GET /api/grades/groups/c3d4e5f6-a7b8-9012-cdef-123456789012/offerings/660e8400-e29b-41d4-a716-446655440001/summary?includeVoided=false
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
{
  "groupId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "rows": [
    {
      "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "totalPoints": 24.5,
      "breakdownByType": { "SEMINAR": 16.0, "HOMEWORK": 8.5 }
    },
    {
      "studentId": "c3d4e5f6-a7b8-9012-cdef-123456789002",
      "totalPoints": 20.0,
      "breakdownByType": { "SEMINAR": 14.0, "HOMEWORK": 6.0 }
    }
  ]
}
```

### 7.5 Установка/замена баллов студента за урок

**Запрос:**

```
PUT /api/grades/lessons/550e8400-e29b-41d4-a716-446655440000/students/a1b2c3d4-e5f6-7890-abcd-ef1234567890/points
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "points": 9.0
}
```

**Ответ:** `200 OK` — объект **GradeEntryDto** (запись, в которой теперь хранятся баллы за этот урок для данного студента).

```json
{
  "id": "a1b2c3d4-1111-2222-3333-444444444444",
  "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "points": 9.0,
  "typeCode": "OTHER",
  "typeLabel": null,
  "description": null,
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "homeworkSubmissionId": null,
  "gradedBy": "12345678-1234-1234-1234-123456789abc",
  "gradedAt": "2025-02-20T15:00:00",
  "status": "ACTIVE"
}
```

---

Документ охватывает все эндпоинты, необходимые для реализации на фронтенде экрана урока: загрузка таблицы посещаемости (Composition), отметка посещаемости (Attendance) и проставление/просмотр баллов (Grades).
