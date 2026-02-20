# Контракт API: уведомления об отсутствии

Документ-контракт для фронтенда по эндпоинтам модуля **Attendance**, связанным с **уведомлениями об отсутствии** (absence notices). Описаны раздельные сценарии для **студентов** (создание, редактирование, отмена уведомлений) и **учителей** (просмотр, принятие/отклонение с комментарием), а также привязка уведомлений к записям посещаемости и смежные ручки.

---

## 1. Общие сведения

### 1.1 Воркфлоу и жизненный цикл уведомления

- **Студент** создаёт уведомление об отсутствии или опоздании на занятие (`POST /api/attendance/notices`). На одно занятие у студента может быть только одно активное уведомление в статусе `SUBMITTED`.
- Пока уведомление в статусе **SUBMITTED**, студент может:
  - **редактировать** его (`PUT /api/attendance/notices/{id}`) — изменить тип, причину, вложения;
  - **отменить** его (`POST /api/attendance/notices/{id}/cancel`) — статус переходит в `CANCELED`.
- **Учитель** (преподаватель этого занятия) может:
  - **принять** уведомление (`POST /api/attendance/notices/{id}/approve`) — статус переходит в `APPROVED`, опционально оставить комментарий;
  - **отклонить** уведомление (`POST /api/attendance/notices/{id}/reject`) — статус переходит в `REJECTED`, опционально оставить комментарий.
- После ответа учителя (**APPROVED** или **REJECTED**) студент **не может** редактировать или отменять уведомление. В ответе уведомления заполняются поля `teacherComment`, `respondedAt`, `respondedBy`.
- Дополнительно учитель может **привязать** уведомление к записи посещаемости (attach/detach) при отметке посещаемости.

**Статусы уведомления:** `SUBMITTED` (активное, ожидает решения), `CANCELED` (отменено студентом), `APPROVED` (принято учителем), `REJECTED` (отклонено учителем), `ACKNOWLEDGED`, `ATTACHED` (зарезервированы).

### 1.2 Базовый URL и префиксы

| Префикс | Назначение |
|---------|------------|
| `/api` | Общий префикс REST API |
| `/api/attendance` | Посещаемость и уведомления об отсутствии |
| `/api/attendance/notices` | Эндпоинты для **студентов**: создание, редактирование, отмена, список своих уведомлений |
| `/api/attendance/teachers/me/notices` | Список всех уведомлений текущего учителя (пагинация, фильтр по статусам) |
| `/api/attendance/sessions/{sessionId}/notices` | Уведомления по одному занятию |
| `/api/attendance/notices/{id}/approve` | Принять уведомление (учитель) |
| `/api/attendance/notices/{id}/reject` | Отклонить уведомление (учитель) |
| `/api/attendance/records/{recordId}/attach-notice` | Привязать уведомление к записи посещаемости |
| `/api/attendance/records/{recordId}/detach-notice` | Отвязать уведомление от записи посещаемости |
| `/api/attendance/sessions/{sessionId}` | Посещаемость по занятию (ростер, счётчики) |
| `/api/attendance/sessions/{sessionId}/students/{studentId}` | Отметить посещаемость одного студента (в т.ч. с привязкой уведомления) |

Хост и порт задаются окружением развёртывания.

### 1.3 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- **Студенты:** эндпоинты под `/api/attendance/notices` доступны только пользователям с профилем студента (по JWT определяется текущий пользователь, по нему — studentId). Студент может создавать/редактировать/отменять только свои уведомления и запрашивать только свой список (`GET /mine`).
- **Учителя:** эндпоинты ленты уведомлений, по занятию, approve/reject доступны **TEACHER** (только для занятий своего offering) или **ADMIN**, **MODERATOR**, **SUPER_ADMIN**. Эндпоинт `GET /api/attendance/teachers/me/notices` доступен только учителям; если пользователь не учитель — `403 Forbidden`.
- При отсутствии прав возвращается `403 Forbidden` (код `ATTENDANCE_FORBIDDEN`, `ATTENDANCE_TEACHER_CANNOT_RESPOND`, `ATTENDANCE_NOTICE_NOT_OWNED` и т.д.), при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

### 1.4 Типы данных в ответах

- **UUID** — строка в формате UUID.
- **Даты и время** — строки **ISO-8601** (например `"2025-02-20T12:00:00"`).
- **Числа** — JSON number (integer где применимо).
- **null** — явное отсутствие значения. Поля, описанные как optional в DTO, в JSON могут быть `null`.
- **Optional-поля** в DTO в JSON представлены как значение или `null`.

### 1.5 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (см. сводную таблицу в конце документа) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для валидации — объект «имя поля → сообщение» |

Пример ответа с ошибкой (403):

```json
{
  "code": "FORBIDDEN",
  "message": "User is not a teacher",
  "timestamp": "2025-02-20T15:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных

### 2.1 AbsenceNoticeDto

Уведомление об отсутствии или опоздании, созданное студентом. После ответа учителя заполняются поля ответа.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор уведомления |
| `lessonSessionId` | UUID | Id занятия (lesson session) |
| `studentId` | UUID | Id студента (student profile ID) |
| `type` | string | Тип: `ABSENT` или `LATE` |
| `reasonText` | string \| null | Причина (опционально) |
| `status` | string | Статус: `SUBMITTED`, `CANCELED`, `ACKNOWLEDGED`, `ATTACHED`, `APPROVED`, `REJECTED` |
| `submittedAt` | string | Дата и время подачи (ISO-8601) |
| `updatedAt` | string | Дата и время обновления (ISO-8601) |
| `canceledAt` | string \| null | Дата и время отмены (если отменено студентом) |
| `attachedRecordId` | UUID \| null | Id записи посещаемости, к которой привязано уведомление (если привязано) |
| `fileIds` | array of string | Список идентификаторов файлов (Document module), прикреплённых к уведомлению |
| `teacherComment` | string \| null | Комментарий учителя при принятии/отклонении (если был указан) |
| `respondedAt` | string \| null | Дата и время ответа учителя (ISO-8601) |
| `respondedBy` | UUID \| null | Id пользователя (учителя), ответившего на уведомление |

### 2.2 SubmitAbsenceNoticeRequest (создание и редактирование)

Тело запроса для создания и редактирования уведомления студентом.

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `lessonSessionId` | UUID | обязательно | Id занятия |
| `type` | string | обязательно | Тип: `ABSENT` или `LATE` |
| `reasonText` | string | нет | Причина (макс. 2000 символов) |
| `fileIds` | array of string | нет | Список id файлов (макс. 10 элементов; каждый не пустая строка) |

При **редактировании** (`PUT`) поле `lessonSessionId` должно совпадать с занятием уведомления (менять занятие нельзя).

### 2.3 RespondToAbsenceNoticeRequest (ответ учителя)

Тело запроса для принятия или отклонения уведомления учителем. Тело опционально (можно отправить пустой объект или не отправлять).

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `comment` | string | нет | Комментарий учителя (макс. 2000 символов) |

### 2.4 TeacherAbsenceNoticePage (лента уведомлений учителя)

Постраничный ответ со списком уведомлений для дашборда учителя.

| Поле | Тип | Описание |
|------|-----|----------|
| `items` | array of **TeacherAbsenceNoticeItemDto** | Элементы текущей страницы |
| `nextCursor` | UUID \| null | Курсор для следующей страницы; `null`, если следующей страницы нет |

### 2.5 TeacherAbsenceNoticeItemDto

Один элемент ленты: уведомление и сводные данные по студенту, занятию, офферингу, **слоту офферинга (offering_slot)** и группе. Для качественного UI в ответе обязательно присутствуют:

- **Предмет:** в `offering` возвращается название предмета (`subjectName`), id предмета в учебном плане (`curriculumSubjectId`), формат и примечания офферинга.
- **Слот офферинга (offering_slot):** в `slot` возвращается полная информация о недельном слоте, к которому привязано занятие (день недели, время, тип занятия — лекция/семинар/практика/лабораторная, аудитория, преподаватель слота). Без отдельного запроса фронтенд может отобразить, «какое это занятие» и по какому расписанию оно идёт.

Дополнительные запросы для отрисовки страницы не требуются.

| Поле | Тип | Описание |
|------|-----|----------|
| `notice` | **AbsenceNoticeDto** | Само уведомление об отсутствии |
| `student` | **TeacherNoticeStudentSummary** \| null | Сводка по студенту |
| `lesson` | **TeacherNoticeLessonSummary** \| null | Сводка по занятию (дата, время, тема, тип) |
| `offering` | **TeacherNoticeOfferingSummary** \| null | Сводка по офферингу (в т.ч. **предмет** — название, id, формат, примечания) |
| `slot` | **TeacherNoticeSlotSummary** \| null | Сводка по **слоту офферинга** (недельный слот: день, время, тип занятия, аудитория, преподаватель). `null`, если занятие не привязано к слоту. |
| `group` | **TeacherNoticeGroupSummary** \| null | Сводка по группе студента |

### 2.6 TeacherNoticeStudentSummary

Сводка по студенту для отображения в карточке уведомления.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля студента |
| `studentId` | string | Университетский номер студента |
| `displayName` | string | Имя для отображения |
| `groupName` | string | Название группы из профиля студента |

### 2.7 TeacherNoticeLessonSummary

Сводка по занятию (уроку). Поле `lessonType` указывает тип занятия (лекция, семинар и т.д.) для отображения на UI.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id занятия (lesson session) |
| `offeringId` | UUID | Id офферинга |
| `date` | string | Дата занятия (ISO-8601 date) |
| `startTime` | string | Время начала (например `"10:00:00"`) |
| `endTime` | string | Время окончания |
| `topic` | string \| null | Тема занятия |
| `status` | string | Статус: `planned`, `cancelled`, `done` |
| `lessonType` | string \| null | Тип занятия: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR`. `null`, если занятие не привязано к слоту (например, старое занятие). |

### 2.8 TeacherNoticeOfferingSummary

Сводка по офферингу с полной информацией для отображения, в том числе **информация о предмете**: название, id в учебном плане, формат проведения, примечания.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id офферинга |
| `groupId` | UUID | Id группы |
| `curriculumSubjectId` | UUID | Id предмета учебного плана (curriculum subject) |
| `subjectName` | string \| null | **Название предмета** для отображения (из учебного плана). `null`, если предмет не найден. |
| `format` | string \| null | Формат проведения: `offline`, `online`, `mixed`. Может быть `null`. |
| `notes` | string \| null | Примечания к офферингу. Может быть `null`. |

### 2.9 TeacherNoticeSlotSummary

Сводка по **слоту офферинга (offering_slot)** — недельному слоту, к которому привязано занятие. Позволяет отобразить тип занятия (лекция, семинар, практика, лабораторная), день и время по расписанию, аудиторию и преподавателя слота без дополнительных запросов.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id слота офферинга |
| `offeringId` | UUID | Id офферинга |
| `dayOfWeek` | integer | День недели (1 = понедельник … 7 = воскресенье) |
| `startTime` | string | Время начала слота (например `"10:00:00"`) |
| `endTime` | string | Время окончания слота |
| `lessonType` | string | Тип занятия: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` |
| `roomId` | UUID \| null | Id аудитории (может быть `null`) |
| `teacherId` | UUID \| null | Id преподавателя, ведущего этот слот (может быть `null`) |
| `timeslotId` | UUID \| null | Id шаблона времени (timeslot), если слот создан из шаблона |

В ответе ленты учителя поле `slot` в каждом элементе заполняется, если занятие привязано к слоту офферинга; иначе `slot` = `null`.

### 2.10 TeacherNoticeGroupSummary

Сводка по группе студента.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id группы |
| `code` | string | Код группы |
| `name` | string \| null | Название группы |

### 2.11 AttendanceRecordDto

Запись посещаемости по одному студенту на одно занятие.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор записи |
| `lessonSessionId` | UUID | Id занятия |
| `studentId` | UUID | Id студента |
| `status` | string | Статус: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `minutesLate` | integer \| null | Минуты опоздания (при status = LATE) |
| `teacherComment` | string \| null | Комментарий преподавателя |
| `markedBy` | UUID | Id пользователя, отметившего посещаемость |
| `markedAt` | string | Дата и время отметки (ISO-8601) |
| `updatedAt` | string | Дата и время обновления (ISO-8601) |
| `absenceNoticeId` | UUID \| null | Id уведомления, привязанного к записи (если есть) |

### 2.12 SessionAttendanceDto

Посещаемость по занятию: счётчики и список студентов с их статусами и уведомлениями.

| Поле | Тип | Описание |
|------|-----|----------|
| `sessionId` | UUID | Id занятия |
| `counts` | object | Карта «статус → количество» (PRESENT, ABSENT, LATE, EXCUSED) |
| `unmarkedCount` | integer | Количество студентов без отметки |
| `students` | array of **SessionAttendanceStudentDto** | Список студентов с полями посещаемости и уведомлений |

Элемент `students`: **SessionAttendanceStudentDto** — `studentId`, `status`, `minutesLate`, `teacherComment`, `markedAt`, `markedBy`, `absenceNoticeId`, `notices` (массив **StudentNoticeDto**: `id`, `type`, `status`, `reasonText`, `submittedAt`, `fileIds`).

---

## 3. Эндпоинты для студентов

Все эндпоинты под `/api/attendance/notices` требуют, чтобы текущий пользователь был студентом (имел профиль студента). Иначе возвращается `403` или `404` (student not found).

### 3.1 POST /api/attendance/notices — создать уведомление

**Назначение:** создать новое уведомление об отсутствии или опоздании на занятие. Студент может иметь только одно активное (SUBMITTED) уведомление на одно занятие. Если такое уже есть — нужно использовать редактирование (`PUT`) или сначала отменить.

**Метод и путь:** `POST /api/attendance/notices`

**Роли:** пользователь должен быть студентом (по JWT определяется studentId).

**Тело запроса:** объект **SubmitAbsenceNoticeRequest** (`lessonSessionId`, `type`, `reasonText`, `fileIds`).

**Успешный ответ:** `201 Created` — объект **AbsenceNoticeDto** (созданное уведомление в статусе `SUBMITTED`).

**Ошибки:**
- `400 Bad Request` — неверное количество вложений, неверный формат fileId, студент не в группе занятия (коды `ATTENDANCE_INVALID_ATTACHMENT_COUNT`, `ATTENDANCE_INVALID_FILE_ID`, `ATTENDANCE_STUDENT_NOT_IN_GROUP`).
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `404 Not Found` — занятие не найдено, офферинг не найден (коды `ATTENDANCE_SESSION_NOT_FOUND`, `ATTENDANCE_OFFERING_NOT_FOUND`). Если пользователь не студент — `ATTENDANCE_STUDENT_NOT_FOUND`.
- `409 Conflict` — у студента уже есть активное уведомление на это занятие (код `ATTENDANCE_NOTICE_ALREADY_EXISTS`, сообщение «Student already has an active absence notice for this session. Use update instead.»).

---

### 3.2 PUT /api/attendance/notices/{id} — редактировать уведомление

**Назначение:** изменить тип, причину и вложения у своего уведомления. Допустимо только для уведомлений в статусе **SUBMITTED**. После ответа учителя (APPROVED/REJECTED) редактирование запрещено.

**Метод и путь:** `PUT /api/attendance/notices/{id}`

**Параметры (path):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `id` | UUID | да | Id уведомления |

**Тело запроса:** объект **SubmitAbsenceNoticeRequest** (`lessonSessionId` должен совпадать с занятием уведомления).

**Успешный ответ:** `200 OK` — объект **AbsenceNoticeDto** (обновлённое уведомление).

**Ошибки:**
- `400 Bad Request` — уведомление уже получило ответ учителя или не в статусе SUBMITTED (код `ATTENDANCE_NOTICE_CANNOT_UPDATE_AFTER_RESPONSE`), неверные вложения или lessonSessionId не совпадает (код `ATTENDANCE_VALIDATION_FAILED`).
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `403 Forbidden` — уведомление не принадлежит текущему студенту (код `ATTENDANCE_NOTICE_NOT_OWNED`).
- `404 Not Found` — уведомление не найдено (код `ATTENDANCE_NOTICE_NOT_FOUND`).

---

### 3.3 POST /api/attendance/notices/{id}/cancel — отменить уведомление

**Назначение:** отменить своё активное уведомление (статус переходит в `CANCELED`). Допустимо только для статуса **SUBMITTED**. После ответа учителя (APPROVED/REJECTED) отмена запрещена.

**Метод и путь:** `POST /api/attendance/notices/{id}/cancel`

**Параметры (path):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `id` | UUID | да | Id уведомления |

**Тело запроса:** не требуется (можно пустое тело или `{}`).

**Успешный ответ:** `200 OK` — объект **AbsenceNoticeDto** (уведомление в статусе `CANCELED`, заполнено `canceledAt`).

**Ошибки:**
- `400 Bad Request` — уведомление уже получило ответ учителя (код `ATTENDANCE_NOTICE_CANNOT_CANCEL_AFTER_RESPONSE`) или не в статусе SUBMITTED (код `ATTENDANCE_NOTICE_NOT_CANCELABLE`).
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `403 Forbidden` — уведомление не принадлежит текущему студенту (код `ATTENDANCE_NOTICE_NOT_OWNED`).
- `404 Not Found` — уведомление не найдено (код `ATTENDANCE_NOTICE_NOT_FOUND`).

---

### 3.4 GET /api/attendance/notices/mine — список своих уведомлений

**Назначение:** получить список уведомлений об отсутствии текущего студента с опциональной фильтрацией по периоду.

**Метод и путь:** `GET /api/attendance/notices/mine`

**Параметры (query):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `from` | string (ISO-8601 datetime) | нет | Нижняя граница по дате подачи (submittedAt >= from) |
| `to` | string (ISO-8601 datetime) | нет | Верхняя граница по дате подачи (submittedAt <= to) |

**Успешный ответ:** `200 OK` — массив объектов **AbsenceNoticeDto**, упорядоченных по `submittedAt` по убыванию.

**Ошибки:**
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `404 Not Found` — у текущего пользователя нет профиля студента (код `ATTENDANCE_STUDENT_NOT_FOUND`).

---

## 4. Эндпоинты для учителей

### 4.1 GET /api/attendance/teachers/me/notices — лента уведомлений учителя

**Назначение:** постраничное получение всех уведомлений об отсутствии для текущего учителя с полным контекстом для отрисовки страницы без дополнительных запросов: студент, занятие, **предмет** (в офферинге: название, id в учебном плане, формат, примечания), **слот офферинга (offering_slot)** (день недели, время, тип занятия — лекция/семинар/практика/лабораторная, аудитория, преподаватель слота), группа. Учитываются только занятия, по которым учитель привязан к offering.

**Метод и путь:** `GET /api/attendance/teachers/me/notices`

**Роли:** только **TEACHER** (или ADMIN/MODERATOR/SUPER_ADMIN). Если пользователь не учитель — `403 Forbidden`.

**Параметры (query):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `statuses` | string | нет | Фильтр по статусам: через запятую, например `SUBMITTED,APPROVED,REJECTED`. Допустимые значения: `SUBMITTED`, `CANCELED`, `ACKNOWLEDGED`, `ATTACHED`, `APPROVED`, `REJECTED`. Если не передан или пустой — возвращаются уведомления всех статусов. |
| `cursor` | UUID | нет | Курсор пагинации (значение `nextCursor` из предыдущего ответа). Для первой страницы не передавать. |
| `limit` | integer | нет | Размер страницы (по умолчанию 30, максимум 30). |

**Успешный ответ:** `200 OK` — объект **TeacherAbsenceNoticePage** (`items`, `nextCursor`). Каждый элемент в `items` — **TeacherAbsenceNoticeItemDto** с полными сводками: **предмет** в `offering` (`subjectName`, `curriculumSubjectId`, `format`, `notes`); **слот офферинга** в `slot` (TeacherNoticeSlotSummary: день недели, время, тип занятия, аудитория, преподаватель); в `lesson` — дата, время, `lessonType`.

**Ошибки:**
- `400 Bad Request` — неверное значение в `statuses` (код `BAD_REQUEST`, сообщение вида «Invalid status: … Valid values: [SUBMITTED, CANCELED, ACKNOWLEDGED, ATTACHED, APPROVED, REJECTED]»).
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `403 Forbidden` — пользователь не является учителем (код `FORBIDDEN`, сообщение «User is not a teacher»).

---

### 4.2 GET /api/attendance/sessions/{sessionId}/notices — уведомления по занятию

**Назначение:** получить список уведомлений об отсутствии по конкретному занятию.

**Метод и путь:** `GET /api/attendance/sessions/{sessionId}/notices`

**Роли:** TEACHER (только для занятий своего offering) или ADMIN, MODERATOR, SUPER_ADMIN.

**Параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `sessionId` | UUID | да (path) | Id занятия (lesson session) |
| `includeCanceled` | boolean | нет | По умолчанию `false` — возвращаются только уведомления в статусе SUBMITTED. Если `true` — все уведомления по занятию (включая CANCELED, APPROVED, REJECTED и др.). |

**Успешный ответ:** `200 OK` — массив объектов **AbsenceNoticeDto**, упорядоченных по `submittedAt` по убыванию.

**Ошибки:**
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `403 Forbidden` — пользователь не является преподавателем этого занятия (код `ATTENDANCE_FORBIDDEN`).
- `404 Not Found` — занятие не найдено (код `ATTENDANCE_SESSION_NOT_FOUND`).

---

### 4.3 POST /api/attendance/notices/{id}/approve — принять уведомление

**Назначение:** принять уведомление об отсутствии (статус переходит в `APPROVED`). Доступно только для уведомлений в статусе **SUBMITTED**. Можно передать опциональный комментарий (отображается студенту в полях `teacherComment`, `respondedAt`, `respondedBy`).

**Метод и путь:** `POST /api/attendance/notices/{id}/approve`

**Параметры (path):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `id` | UUID | да | Id уведомления |

**Тело запроса:** опционально — объект **RespondToAbsenceNoticeRequest** с полем `comment` (string, макс. 2000). Можно отправить пустой объект `{}` или не отправлять тело.

**Успешный ответ:** `200 OK` — объект **AbsenceNoticeDto** (статус `APPROVED`, заполнены `teacherComment`, `respondedAt`, `respondedBy` при наличии).

**Ошибки:**
- `400 Bad Request` — уведомление уже получило ответ учителя (код `ATTENDANCE_NOTICE_ALREADY_RESPONDED`).
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).
- `403 Forbidden` — пользователь не является преподавателем занятия этого уведомления (код `ATTENDANCE_TEACHER_CANNOT_RESPOND`).
- `404 Not Found` — уведомление или занятие не найдены (коды `ATTENDANCE_NOTICE_NOT_FOUND`, `ATTENDANCE_SESSION_NOT_FOUND`).

---

### 4.4 POST /api/attendance/notices/{id}/reject — отклонить уведомление

**Назначение:** отклонить уведомление об отсутствии (статус переходит в `REJECTED`). Доступно только для уведомлений в статусе **SUBMITTED**. Можно передать опциональный комментарий.

**Метод и путь:** `POST /api/attendance/notices/{id}/reject`

**Параметры (path):**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `id` | UUID | да | Id уведомления |

**Тело запроса:** опционально — объект **RespondToAbsenceNoticeRequest** с полем `comment`. Можно отправить пустой объект `{}` или не отправлять тело.

**Успешный ответ:** `200 OK` — объект **AbsenceNoticeDto** (статус `REJECTED`, заполнены `teacherComment`, `respondedAt`, `respondedBy` при наличии).

**Ошибки:** те же, что и для `POST .../approve`.

---

## 5. Редактирование связи «уведомление — запись посещаемости»

Учитель может **привязать** уведомление к записи посещаемости или **отвязать** его (при отметке посещаемости).

### 5.1 POST /api/attendance/records/{recordId}/attach-notice

**Назначение:** привязать уведомление об отсутствии к существующей записи посещаемости.

**Метод и путь:** `POST /api/attendance/records/{recordId}/attach-notice`

**Роли:** TEACHER (только для занятий своего offering) или ADMIN, MODERATOR, SUPER_ADMIN.

**Параметры:** `recordId` (path, UUID). **Тело:** `{ "noticeId": "<UUID>" }`.

**Успешный ответ:** `200 OK` — объект **AttendanceRecordDto** с заполненным `absenceNoticeId`.

**Ошибки:** `400` (уведомление отменено или не подходит к записи), `401`, `403`, `404` (запись или уведомление не найдены).

---

### 5.2 POST /api/attendance/records/{recordId}/detach-notice

**Назначение:** отвязать уведомление от записи посещаемости.

**Метод и путь:** `POST /api/attendance/records/{recordId}/detach-notice`

**Роли:** те же. **Тело:** не требуется.

**Успешный ответ:** `200 OK` — объект **AttendanceRecordDto** с `absenceNoticeId: null`.

---

## 6. Посещаемость по занятию и отметка одного студента

### 6.1 GET /api/attendance/sessions/{sessionId}

**Назначение:** получить посещаемость по занятию (ростер, счётчики, уведомления по студентам).

**Метод и путь:** `GET /api/attendance/sessions/{sessionId}`

**Параметры:** `sessionId` (path), `includeCanceled` (query, boolean, по умолчанию false).

**Успешный ответ:** `200 OK` — объект **SessionAttendanceDto**.

---

### 6.2 PUT /api/attendance/sessions/{sessionId}/students/{studentId}

**Назначение:** отметить или обновить посещаемость одного студента; при необходимости указать привязку уведомления (`absenceNoticeId` или `autoAttachLastNotice`).

**Метод и путь:** `PUT /api/attendance/sessions/{sessionId}/students/{studentId}`

**Тело запроса:** `status` (обязательно), `minutesLate`, `teacherComment`, `absenceNoticeId`, `autoAttachLastNotice`.

**Успешный ответ:** `200 OK` — объект **AttendanceRecordDto**.

---

## 7. Сводная таблица ошибок

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Неверный параметр (например, неверный статус в `statuses`), неверные данные в теле |
| 400 | VALIDATION_FAILED | Ошибка валидации запроса (в `details` — объект «поле → сообщение») |
| 400 | ATTENDANCE_VALIDATION_FAILED | Нарушение бизнес-правил посещаемости |
| 400 | ATTENDANCE_STUDENT_NOT_IN_GROUP | Студент не в ростре группы занятия |
| 400 | ATTENDANCE_NOTICE_CANCELED | Уведомление отменено, привязка невозможна |
| 400 | ATTENDANCE_NOTICE_NOT_CANCELABLE | Уведомление нельзя отменить (не SUBMITTED или уже ответ учителя) |
| 400 | ATTENDANCE_NOTICE_ALREADY_RESPONDED | Уведомление уже получило ответ учителя (approve/reject повторно) |
| 400 | ATTENDANCE_NOTICE_CANNOT_UPDATE_AFTER_RESPONSE | Редактирование уведомления после ответа учителя |
| 400 | ATTENDANCE_NOTICE_CANNOT_CANCEL_AFTER_RESPONSE | Отмена уведомления после ответа учителя |
| 400 | ATTENDANCE_NOTICE_DOES_NOT_MATCH_RECORD | Уведомление не подходит к записи (другое занятие/студент) |
| 400 | ATTENDANCE_INVALID_ATTACHMENT_COUNT | Превышено максимальное количество вложений (10) |
| 400 | ATTENDANCE_INVALID_FILE_ID | Неверный формат или пустой fileId |
| 401 | UNAUTHORIZED | Не передан или невалидный JWT |
| 403 | FORBIDDEN | Пользователь не учитель (для `/teachers/me/notices`) |
| 403 | ATTENDANCE_FORBIDDEN | Нет прав на занятие/запись (не преподаватель этого offering) |
| 403 | ATTENDANCE_NOTICE_NOT_OWNED | Уведомление не принадлежит текущему студенту |
| 403 | ATTENDANCE_TEACHER_CANNOT_RESPOND | Учитель не может ответить на это уведомление (не его занятие) |
| 404 | NOT_FOUND | Общий «не найдено» |
| 404 | ATTENDANCE_SESSION_NOT_FOUND | Занятие не найдено |
| 404 | ATTENDANCE_NOTICE_NOT_FOUND | Уведомление не найдено |
| 404 | ATTENDANCE_RECORD_NOT_FOUND | Запись посещаемости не найдена |
| 404 | ATTENDANCE_LESSON_NOT_FOUND | Занятие не найдено |
| 404 | ATTENDANCE_STUDENT_NOT_FOUND | Студент не найден / у пользователя нет профиля студента |
| 404 | ATTENDANCE_OFFERING_NOT_FOUND | Офферинг не найден |
| 409 | ATTENDANCE_NOTICE_ALREADY_EXISTS | У студента уже есть активное уведомление на это занятие (при создании) |

---

## 8. Примеры запросов и ответов

### Создание уведомления (студент)

**Запрос:**  
`POST /api/attendance/notices`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440001",
  "type": "ABSENT",
  "reasonText": "Выезд на соревнования",
  "fileIds": ["file-uuid-1", "file-uuid-2"]
}
```

**Ответ:** `201 Created`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440001",
  "studentId": "660e8400-e29b-41d4-a716-446655440002",
  "type": "ABSENT",
  "reasonText": "Выезд на соревнования",
  "status": "SUBMITTED",
  "submittedAt": "2025-02-20T09:00:00",
  "updatedAt": "2025-02-20T09:00:00",
  "canceledAt": null,
  "attachedRecordId": null,
  "fileIds": ["file-uuid-1", "file-uuid-2"],
  "teacherComment": null,
  "respondedAt": null,
  "respondedBy": null
}
```

---

### Редактирование уведомления (студент)

**Запрос:**  
`PUT /api/attendance/notices/a1b2c3d4-e5f6-7890-abcd-ef1234567890`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440001",
  "type": "LATE",
  "reasonText": "Опоздание по уважительной причине",
  "fileIds": ["file-uuid-1"]
}
```

**Ответ:** `200 OK` — объект **AbsenceNoticeDto** с обновлёнными полями, `status` остаётся `SUBMITTED`.

---

### Принятие уведомления с комментарием (учитель)

**Запрос:**  
`POST /api/attendance/notices/a1b2c3d4-e5f6-7890-abcd-ef1234567890/approve`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "comment": "Учтено. Пришлите справку до конца недели."
}
```

**Ответ:** `200 OK`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440001",
  "studentId": "660e8400-e29b-41d4-a716-446655440002",
  "type": "ABSENT",
  "reasonText": "Выезд на соревнования",
  "status": "APPROVED",
  "submittedAt": "2025-02-20T09:00:00",
  "updatedAt": "2025-02-20T14:00:00",
  "canceledAt": null,
  "attachedRecordId": null,
  "fileIds": ["file-uuid-1"],
  "teacherComment": "Учтено. Пришлите справку до конца недели.",
  "respondedAt": "2025-02-20T14:00:00",
  "respondedBy": "880e8400-e29b-41d4-a716-446655440004"
}
```

---

### Отклонение уведомления без тела (учитель)

**Запрос:**  
`POST /api/attendance/notices/a1b2c3d4-e5f6-7890-abcd-ef1234567890/reject`  
`Authorization: Bearer <JWT>`

(тело пустое или `{}`)

**Ответ:** `200 OK` — объект **AbsenceNoticeDto** со статусом `REJECTED`, `teacherComment`/`respondedAt`/`respondedBy` заполнены при наличии или null.

---

### Конфликт: уже есть активное уведомление на занятие

**Запрос:** повторное `POST /api/attendance/notices` с тем же `lessonSessionId` для того же студента.

**Ответ:** `409 Conflict`

```json
{
  "code": "ATTENDANCE_NOTICE_ALREADY_EXISTS",
  "message": "Student already has an active absence notice for this session. Use update instead.",
  "timestamp": "2025-02-20T15:00:00.123Z",
  "details": null
}
```

---

### Получение первой страницы уведомлений учителя (только ожидающие и принятые)

**Запрос:**  
`GET /api/attendance/teachers/me/notices?statuses=SUBMITTED,APPROVED&limit=30`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK` — объект **TeacherAbsenceNoticePage**. В каждом элементе `items`: `notice`, `student`, `lesson`, **`offering`** (информация о **предмете**: `subjectName`, `curriculumSubjectId`, `format`, `notes`), **`slot`** (информация о **слоте офферинга (offering_slot)**: день недели, время, тип занятия, аудитория, преподаватель), `group`. Пример фрагмента элемента:

```json
{
  "notice": { "id": "...", "lessonSessionId": "...", "studentId": "...", "type": "ABSENT", "status": "SUBMITTED", ... },
  "student": { "id": "...", "studentId": "12345", "displayName": "Иванов И.", "groupName": "Группа 1" },
  "lesson": {
    "id": "...",
    "offeringId": "...",
    "date": "2025-02-20",
    "startTime": "10:00:00",
    "endTime": "11:30:00",
    "topic": "Тема занятия",
    "status": "planned",
    "lessonType": "LECTURE"
  },
  "offering": {
    "id": "...",
    "groupId": "...",
    "curriculumSubjectId": "...",
    "subjectName": "Математический анализ",
    "format": "offline",
    "notes": null
  },
  "slot": {
    "id": "...",
    "offeringId": "...",
    "dayOfWeek": 1,
    "startTime": "10:00:00",
    "endTime": "11:30:00",
    "lessonType": "LECTURE",
    "roomId": "...",
    "teacherId": "...",
    "timeslotId": null
  },
  "group": { "id": "...", "code": "GR-1", "name": "Группа 1" }
}
```

---

### Уведомления по занятию (включая ответы учителя)

**Запрос:**  
`GET /api/attendance/sessions/550e8400-e29b-41d4-a716-446655440001/notices?includeCanceled=true`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK` — массив **AbsenceNoticeDto** (все статусы, включая APPROVED, REJECTED, CANCELED; при необходимости с заполненными полями ответа учителя).
