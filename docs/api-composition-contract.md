# Контракт API: композиция данных (агрегирующие эндпоинты)

Документ-контракт для фронтенда. Описывает read-only эндпоинты модуля Composition, которые агрегируют данные из нескольких модулей в один ответ для снижения числа запросов на сложных экранах. В контракте приведены структуры запросов и ответов, а также возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/composition` | Агрегирующие эндпоинты (полная информация по уроку и др.) |

Хост и порт задаются окружением развёртывания. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы к эндпоинтам модуля выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе) или с учётом cookie-механизма, принятого в проекте.
- Эндпоинты модуля **только для чтения** (GET). Доступ имеют **все аутентифицированные пользователи**; при отсутствии JWT или невалидном токене возвращается `401 Unauthorized`.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки в формате **ISO-8601**: дата-время вида `"2025-02-05T12:00:00"`, дата вида `"2025-02-05"`. Время урока — строка в формате **HH:mm:ss** (например, `"13:00:00"`).
- **Числа** — JSON number; для целых полей — integer; для кредитов/весов может использоваться number с дробной частью.
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.
- **Массивы** — всегда массив (например, `[]` при отсутствии элементов).

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для ошибок валидации — объект «имя поля → сообщение»; иначе может отсутствовать или быть `null` |

Пример ответа с ошибкой «не найдено» (404):

```json
{
  "code": "NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

Пример ответа с ошибкой авторизации (401):

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных (переиспользуемые DTO)

Ответы модуля Composition переиспользуют DTO из других модулей. Ниже приведены структуры всех типов, входящих в ответ **LessonFullDetailsDto**.

### 2.1 LessonDto (урок)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор урока |
| `offeringId` | UUID | Id офферинга (предложение занятия) |
| `offeringSlotId` | UUID \| null | Id слота офферинга, из которого сгенерирован урок |
| `date` | string | Дата урока (ISO-8601 date, например `"2025-02-19"`) |
| `startTime` | string | Время начала (HH:mm:ss) |
| `endTime` | string | Время окончания (HH:mm:ss) |
| `timeslotId` | UUID \| null | Id шаблона времени |
| `roomId` | UUID \| null | Id аудитории (если назначена) |
| `topic` | string \| null | Тема урока |
| `status` | string \| null | Статус: `PLANNED`, `CANCELLED`, `DONE` или `null` |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

### 2.2 SubjectDto (предмет)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id предмета |
| `code` | string \| null | Код предмета |
| `chineseName` | string \| null | Название на китайском |
| `englishName` | string \| null | Название на английском |
| `description` | string \| null | Описание |
| `departmentId` | UUID \| null | Id департамента |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.3 StudentGroupDto (группа)

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
| `curatorUserId` | UUID \| null | Id пользователя-куратора |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.4 GroupSubjectOfferingDto (офферинг)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id офферинга |
| `groupId` | UUID | Id группы |
| `curriculumSubjectId` | UUID | Id элемента учебного плана (curriculum subject) |
| `teacherId` | UUID \| null | Id основного преподавателя (профиль teacher) |
| `roomId` | UUID \| null | Id аудитории по умолчанию |
| `format` | string \| null | Формат: например `offline`, `online`, `mixed` |
| `notes` | string \| null | Заметки |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.5 OfferingSlotDto (слот офферинга)

Еженедельный слот офферинга (день недели, время, тип занятия). Урок может быть сгенерирован из такого слота; в этом случае в **LessonDto** заполнено поле `offeringSlotId`.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id слота |
| `offeringId` | UUID | Id офферинга |
| `dayOfWeek` | integer | День недели (1–7, понедельник = 1) |
| `startTime` | string | Время начала (HH:mm:ss) |
| `endTime` | string | Время окончания (HH:mm:ss) |
| `timeslotId` | UUID \| null | Id шаблона времени (опционально) |
| `lessonType` | string \| null | Тип занятия: например `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` |
| `roomId` | UUID \| null | Id аудитории (переопределение для слота) |
| `teacherId` | UUID \| null | Id преподавателя (переопределение для слота) |
| `createdAt` | string | Дата создания (ISO-8601) |

### 2.6 CurriculumSubjectDto (элемент учебного плана)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id элемента учебного плана |
| `curriculumId` | UUID | Id учебного плана |
| `subjectId` | UUID | Id предмета в каталоге |
| `semesterNo` | integer | Номер семестра |
| `courseYear` | integer \| null | Курс |
| `durationWeeks` | integer | Длительность в неделях |
| `hoursTotal` | integer \| null | Всего часов |
| `hoursLecture` | integer \| null | Часы лекций |
| `hoursPractice` | integer \| null | Часы практики |
| `hoursLab` | integer \| null | Часы лабораторных |
| `hoursSeminar` | integer \| null | Часы семинаров |
| `hoursSelfStudy` | integer \| null | Часы самостоятельной работы |
| `hoursConsultation` | integer \| null | Часы консультаций |
| `hoursCourseWork` | integer \| null | Часы курсовой работы |
| `assessmentTypeId` | UUID | Id типа контроля |
| `credits` | number \| null | Кредиты |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.7 RoomDto (аудитория)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id аудитории |
| `buildingId` | UUID | Id здания |
| `buildingName` | string \| null | Название здания |
| `number` | string \| null | Номер аудитории |
| `capacity` | integer \| null | Вместимость |
| `type` | string \| null | Тип помещения |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.8 TeacherDto (профиль преподавателя)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля преподавателя |
| `userId` | UUID | Id пользователя (аккаунта) |
| `teacherId` | string \| null | Внешний идентификатор преподавателя |
| `faculty` | string \| null | Факультет |
| `englishName` | string \| null | Имя на английском |
| `position` | string \| null | Должность |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.9 OfferingTeacherItemDto (преподаватель офферинга)

Список выводится из основного преподавателя офферинга и преподавателей слотов (только чтение).

| Поле | Тип | Описание |
|------|-----|----------|
| `teacherId` | UUID | Id профиля преподавателя |
| `role` | string \| null | Роль: `null` для основного преподавателя офферинга; `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` для преподавателя из слота |

### 2.10 StoredFileDto (файл в хранилище)

Используется внутри **LessonMaterialDto** (поле `files`) и **HomeworkDto** (поле `files`).

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла |
| `size` | integer | Размер в байтах |
| `contentType` | string \| null | MIME-тип |
| `originalName` | string \| null | Оригинальное имя файла |
| `uploadedAt` | string | Дата загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 2.11 LessonMaterialDto (материал урока)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id материала |
| `lessonId` | UUID | Id урока |
| `name` | string \| null | Название материала |
| `description` | string \| null | Описание |
| `authorId` | UUID | Id автора материала |
| `publishedAt` | string | Дата публикации (ISO-8601) |
| `files` | array of **StoredFileDto** | Список файлов материала (упорядоченный) |

### 2.12 HomeworkDto (домашнее задание)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id домашнего задания |
| `lessonId` | UUID | Id урока |
| `title` | string \| null | Заголовок |
| `description` | string \| null | Описание |
| `points` | integer \| null | Максимальное количество баллов |
| `files` | array of **StoredFileDto** | Прикреплённые файлы (порядок сохранён) |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.13 StudentDto (студент)

Используется в **LessonHomeworkSubmissionsDto** и **LessonRosterAttendanceDto**.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля студента |
| `userId` | UUID | Id пользователя (аккаунта) |
| `studentId` | string | Внешний идентификатор студента |
| `chineseName` | string \| null | Имя на китайском |
| `faculty` | string \| null | Факультет |
| `course` | string \| null | Курс |
| `enrollmentYear` | integer \| null | Год поступления |
| `groupName` | string \| null | Название группы |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.14 HomeworkSubmissionDto (отправка домашнего задания)

Модуль submission. Используется в **StudentHomeworkItemDto**.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id отправки |
| `homeworkId` | UUID | Id домашнего задания |
| `authorId` | UUID | Id студента (автора) |
| `submittedAt` | string | Дата и время отправки (ISO-8601) |
| `description` | string \| null | Текстовое описание (если есть) |
| `storedFileIds` | array of UUID | Id файлов в хранилище (порядок сохранён) |

---

## 3. Полная информация по уроку (Lesson Full Details)

Один агрегирующий GET возвращает все данные, необходимые фронтенду для экрана «Полная информация по уроку»: предмет, группа, материалы, домашние задания, здание, кабинет, преподаватели, дата и время, офферинг и элемент учебного плана.

### 3.1 GET /api/composition/lessons/{lessonId}/full-details — полная информация по уроку

**Назначение:** получить одной ручкой все данные для экрана полной информации по уроку: информация о предмете, о группе (в т.ч. groupId), материалы урока, домашние задания урока, данные о проведении (здание, кабинет, преподаватель, дата и время), информация об офферинге и о конкретной реализации урока (lesson instance). Сокращает количество запросов фронтенда в сложных сценариях.

**Метод и путь:** `GET /api/composition/lessons/{lessonId}/full-details`

**Права доступа:** любой аутентифицированный пользователь. Требуется валидный JWT (или иной принятый в проекте механизм аутентификации).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Query-параметры:** нет.

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK`

Тело ответа — объект **LessonFullDetailsDto**:

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object | Базовая информация об уроке — **LessonDto** (см. п. 2.1) |
| `subject` | object | Информация о предмете — **SubjectDto** (см. п. 2.2) |
| `group` | object | Информация о группе, которая посещает урок — **StudentGroupDto** (см. п. 2.3); ключевое поле — `id` (groupId) |
| `offering` | object | Информация об офферинге — **GroupSubjectOfferingDto** (см. п. 2.4) |
| `offeringSlot` | object \| null | Слот офферинга, из которого сгенерирован урок — **OfferingSlotDto** (см. п. 2.5); `null`, если у урока нет `offeringSlotId` или слот не найден |
| `curriculumSubject` | object | Элемент учебного плана — **CurriculumSubjectDto** (см. п. 2.6) |
| `room` | object \| null | Аудитория и здание — **RoomDto** (см. п. 2.7); `null`, если аудитория не назначена или не найдена |
| `mainTeacher` | object \| null | Основной преподаватель офферинга — **TeacherDto** (см. п. 2.8); `null`, если не назначен или не найден |
| `offeringTeachers` | array | Список преподавателей офферинга (выводится из основного преподавателя и слотов) — массив **OfferingTeacherItemDto** (см. п. 2.9); пустой массив, если нет назначений |
| `materials` | array | Все материалы урока — массив **LessonMaterialDto** (см. п. 2.11); пустой массив, если материалов нет |
| `homework` | array | Все домашние задания, привязанные к уроку — массив **HomeworkDto** (см. п. 2.12); пустой массив, если ДЗ нет |

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 403 | FORBIDDEN | Нет прав на просмотр материалов/домашних заданий урока (модуль document отклонил доступ) | Сообщение от модуля документов (например, доступ запрещён к уроку) |
| 404 | NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | NOT_FOUND | Офферинг не найден (по id из урока) | `"Offering not found: <offeringId>"` |
| 404 | NOT_FOUND | Элемент учебного плана не найден | `"Curriculum subject not found: <curriculumSubjectId>"` |
| 404 | NOT_FOUND | Предмет не найден | `"Subject not found: <subjectId>"` |
| 404 | NOT_FOUND | Группа не найдена | `"Group not found: <groupId>"` |

Примечание: поля `room`, `mainTeacher` и `offeringSlot` в успешном ответе могут быть `null`, если аудитория не назначена, основной преподаватель не назначен/удалён или урок не привязан к слоту офферинга; при этом ответ остаётся `200 OK`. Списки `materials` и `homework` при отсутствии данных приходят пустыми массивами.

---

## 3.2 GET /api/composition/lessons/{lessonId}/homework-submissions — отправленные домашние задания по уроку

**Назначение:** получить одной ручкой все данные для таблицы «Домашние задания по уроку»: список всех студентов группы, которая посещает урок; для каждого студента и каждого домашнего задания урока — отправка (если есть), выставленные баллы и список файлов. Если студент не отправил ДЗ — в ячейке пустой объект (null submission, null points, пустой массив files). Если по уроку задано несколько домашних заданий, порядок колонок совпадает с порядком в массиве `homeworks`.

**Метод и путь:** `GET /api/composition/lessons/{lessonId}/homework-submissions`

**Права доступа:** аутентифицированный пользователь с правом просмотра отправок и оценок (преподаватель урока или администратор). Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Query-параметры:** нет.

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK`

Тело ответа — объект **LessonHomeworkSubmissionsDto**:

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object | Информация об уроке — **LessonDto** (см. п. 2.1) |
| `group` | object | Группа, которая посещает урок — **StudentGroupDto** (см. п. 2.3) |
| `homeworks` | array | Все домашние задания урока (порядок задаёт порядок колонок в таблице) — массив **HomeworkDto** (см. п. 2.12) |
| `studentRows` | array | Строки таблицы: по одной на каждого студента группы — массив **StudentHomeworkRowDto** (см. ниже) |

**StudentHomeworkRowDto** (одна строка — один студент):

| Поле | Тип | Описание |
|------|-----|----------|
| `student` | object | Данные студента — **StudentDto** (см. п. 2.13; модуль student) |
| `items` | array | Ячейки по домашним заданиям (один элемент на каждое ДЗ из `homeworks`, в том же порядке) — массив **StudentHomeworkItemDto** (см. ниже) |

**StudentHomeworkItemDto** (одна ячейка — одно ДЗ для данного студента):

| Поле | Тип | Описание |
|------|-----|----------|
| `homeworkId` | UUID | Id домашнего задания |
| `submission` | object \| null | Отправка студента — **HomeworkSubmissionDto** (см. п. 2.14); `null`, если студент не отправил |
| `points` | number \| null | Баллы, выставленные за эту отправку; `null`, если нет отправки или оценка ещё не выставлена |
| `files` | array | Метаданные файлов отправки (массив **StoredFileDto**, см. п. 2.10); пустой массив, если нет отправки или нет файлов |

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 403 | FORBIDDEN | Нет прав на просмотр отправок или оценок (модуль submission/grades отклонил доступ) | Сообщение от модуля |
| 404 | NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | NOT_FOUND | Офферинг не найден | `"Offering not found: <offeringId>"` |
| 404 | NOT_FOUND | Группа не найдена | `"Group not found: <groupId>"` |

При отсутствии домашних заданий по уроку массив `homeworks` пустой, в каждой строке `items` — пустой массив. При отсутствии отправок у студента по конкретному ДЗ в соответствующем элементе `items`: `submission` = null, `points` = null, `files` = [].

---

## 4. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Требуется аутентификация (нет или невалидный JWT) |
| 403 | FORBIDDEN | Нет прав на просмотр материалов/домашних заданий урока; нет прав на просмотр отправок или оценок (homework-submissions) |
| 404 | NOT_FOUND | Урок, офферинг, элемент учебного плана, предмет или группа не найдены |

---

## 5. Примеры запросов и ответов

### 5.1 Успешный запрос полной информации по уроку

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/full-details
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
{
  "lesson": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "offeringId": "660e8400-e29b-41d4-a716-446655440001",
    "offeringSlotId": "770e8400-e29b-41d4-a716-446655440002",
    "date": "2025-02-19",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "timeslotId": "880e8400-e29b-41d4-a716-446655440003",
    "roomId": "990e8400-e29b-41d4-a716-446655440004",
    "topic": "Introduction to Algorithms",
    "status": "PLANNED",
    "createdAt": "2025-02-01T10:00:00",
    "updatedAt": "2025-02-01T10:00:00"
  },
  "subject": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "code": "CS101",
    "chineseName": "算法导论",
    "englishName": "Introduction to Algorithms",
    "description": null,
    "departmentId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "createdAt": "2024-09-01T00:00:00",
    "updatedAt": "2024-09-01T00:00:00"
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
    "curatorUserId": "f6a7b8c9-d0e1-2345-f012-456789012345",
    "createdAt": "2024-09-01T00:00:00",
    "updatedAt": "2024-09-01T00:00:00"
  },
  "offering": {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "groupId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "curriculumSubjectId": "a0b1c2d3-e4f5-6789-0123-456789abcdef",
    "teacherId": "12345678-1234-1234-1234-123456789abc",
    "roomId": "990e8400-e29b-41d4-a716-446655440004",
    "format": "offline",
    "notes": null,
    "createdAt": "2024-09-15T00:00:00",
    "updatedAt": "2024-09-15T00:00:00"
  },
  "offeringSlot": {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "offeringId": "660e8400-e29b-41d4-a716-446655440001",
    "dayOfWeek": 2,
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "timeslotId": "880e8400-e29b-41d4-a716-446655440003",
    "lessonType": "LECTURE",
    "roomId": "990e8400-e29b-41d4-a716-446655440004",
    "teacherId": "12345678-1234-1234-1234-123456789abc",
    "createdAt": "2024-09-15T00:00:00"
  },
  "curriculumSubject": {
    "id": "a0b1c2d3-e4f5-6789-0123-456789abcdef",
    "curriculumId": "e5f6a7b8-c9d0-1234-ef01-345678901234",
    "subjectId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "semesterNo": 1,
    "courseYear": 1,
    "durationWeeks": 16,
    "hoursTotal": 64,
    "hoursLecture": 32,
    "hoursPractice": 16,
    "hoursLab": 16,
    "hoursSeminar": null,
    "hoursSelfStudy": null,
    "hoursConsultation": null,
    "hoursCourseWork": null,
    "assessmentTypeId": "f1e2d3c4-b5a6-9870-5432-109876543210",
    "credits": 4.0,
    "createdAt": "2024-08-01T00:00:00",
    "updatedAt": "2024-08-01T00:00:00"
  },
  "room": {
    "id": "990e8400-e29b-41d4-a716-446655440004",
    "buildingId": "11111111-2222-3333-4444-555555555555",
    "buildingName": "Building A",
    "number": "208",
    "capacity": 30,
    "type": "lecture",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "mainTeacher": {
    "id": "12345678-1234-1234-1234-123456789abc",
    "userId": "22222222-3333-4444-5555-666666666666",
    "teacherId": "T001",
    "faculty": "Computer Science",
    "englishName": "John Smith",
    "position": "Associate Professor",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "offeringTeachers": [
    {
      "teacherId": "12345678-1234-1234-1234-123456789abc",
      "role": "LECTURE"
    }
  ],
  "materials": [
    {
      "id": "bb0e8400-e29b-41d4-a716-446655440006",
      "lessonId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Lecture Slides - Introduction",
      "description": null,
      "authorId": "22222222-3333-4444-5555-666666666666",
      "publishedAt": "2025-02-18T10:00:00",
      "files": [
        {
          "id": "cc0e8400-e29b-41d4-a716-446655440007",
          "size": 2411520,
          "contentType": "application/pdf",
          "originalName": "Lecture Slides.pdf",
          "uploadedAt": "2025-02-18T10:00:00",
          "uploadedBy": "22222222-3333-4444-5555-666666666666"
        }
      ]
    }
  ],
  "homework": [
    {
      "id": "dd0e8400-e29b-41d4-a716-446655440008",
      "lessonId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Homework 1",
      "description": "Complete exercises 1-5",
      "points": 10,
      "file": null,
      "createdAt": "2025-02-18T12:00:00",
      "updatedAt": "2025-02-18T12:00:00"
    }
  ]
}
```

### 5.2 Ответ при отсутствии аутентификации

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/full-details
```

**Ответ:** `401 Unauthorized`

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.3 Ответ при отсутствии урока

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/full-details
Authorization: Bearer <JWT>
```

**Ответ:** `404 Not Found`

```json
{
  "code": "NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.4 Ответ с пустыми материалами и домашними заданиями

При успешном ответе `200 OK` поля `materials` и `homework` могут быть пустыми массивами; `room` и `mainTeacher` — `null`. Фрагмент:

```json
{
  "lesson": { ... },
  "subject": { ... },
  "group": { ... },
  "offering": { ... },
  "offeringSlot": null,
  "curriculumSubject": { ... },
  "room": null,
  "mainTeacher": null,
  "offeringTeachers": [],
  "materials": [],
  "homework": []
}
```

Фронтенд может использовать эти данные для отображения пустых состояний блоков «Материалы урока» и «Домашние задания».

### 5.5 Успешный запрос отправленных домашних заданий по уроку

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/homework-submissions
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

Урок с двумя домашними заданиями; в группе два студента: один отправил оба ДЗ с оценками и файлами, второй — только первое ДЗ.

```json
{
  "lesson": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "offeringId": "660e8400-e29b-41d4-a716-446655440001",
    "offeringSlotId": null,
    "date": "2025-02-19",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "timeslotId": null,
    "roomId": "990e8400-e29b-41d4-a716-446655440004",
    "topic": "Algorithms",
    "status": "DONE",
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
  "homeworks": [
    {
      "id": "dd0e8400-e29b-41d4-a716-446655440008",
      "lessonId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Homework 1",
      "description": "Exercises 1-5",
      "points": 10,
      "file": null,
      "createdAt": "2025-02-18T12:00:00",
      "updatedAt": "2025-02-18T12:00:00"
    },
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440009",
      "lessonId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Homework 2",
      "description": "Project draft",
      "points": 20,
      "file": null,
      "createdAt": "2025-02-18T14:00:00",
      "updatedAt": "2025-02-18T14:00:00"
    }
  ],
  "studentRows": [
    {
      "student": {
        "id": "11111111-2222-3333-4444-555555555551",
        "userId": "aaaa1111-2222-3333-4444-555555555551",
        "studentId": "S001",
        "chineseName": "张三",
        "faculty": "CS",
        "course": "1",
        "enrollmentYear": 2024,
        "groupName": "Group A",
        "createdAt": "2024-09-01T00:00:00",
        "updatedAt": "2024-09-01T00:00:00"
      },
      "items": [
        {
          "homeworkId": "dd0e8400-e29b-41d4-a716-446655440008",
          "submission": {
            "id": "sub1111-2222-3333-4444-555555555551",
            "homeworkId": "dd0e8400-e29b-41d4-a716-446655440008",
            "authorId": "11111111-2222-3333-4444-555555555551",
            "submittedAt": "2025-02-19T10:00:00",
            "description": "Done",
            "storedFileIds": ["file-aaa-111"]
          },
          "points": 8,
          "files": [
            {
              "id": "file-aaa-111",
              "size": 1024,
              "contentType": "application/pdf",
              "originalName": "hw1.pdf",
              "uploadedAt": "2025-02-19T10:00:00",
              "uploadedBy": "aaaa1111-2222-3333-4444-555555555551"
            }
          ]
        },
        {
          "homeworkId": "ee0e8400-e29b-41d4-a716-446655440009",
          "submission": {
            "id": "sub2222-3333-4444-5555-666666666662",
            "homeworkId": "ee0e8400-e29b-41d4-a716-446655440009",
            "authorId": "11111111-2222-3333-4444-555555555551",
            "submittedAt": "2025-02-20T11:00:00",
            "description": null,
            "storedFileIds": ["file-bbb-222"]
          },
          "points": 15,
          "files": [
            {
              "id": "file-bbb-222",
              "size": 2048,
              "contentType": "application/pdf",
              "originalName": "draft.pdf",
              "uploadedAt": "2025-02-20T11:00:00",
              "uploadedBy": "aaaa1111-2222-3333-4444-555555555551"
            }
          ]
        }
      ]
    },
    {
      "student": {
        "id": "11111111-2222-3333-4444-555555555552",
        "userId": "aaaa1111-2222-3333-4444-555555555552",
        "studentId": "S002",
        "chineseName": "李四",
        "faculty": "CS",
        "course": "1",
        "enrollmentYear": 2024,
        "groupName": "Group A",
        "createdAt": "2024-09-01T00:00:00",
        "updatedAt": "2024-09-01T00:00:00"
      },
      "items": [
        {
          "homeworkId": "dd0e8400-e29b-41d4-a716-446655440008",
          "submission": {
            "id": "sub3333-4444-5555-6666-777777777773",
            "homeworkId": "dd0e8400-e29b-41d4-a716-446655440008",
            "authorId": "11111111-2222-3333-4444-555555555552",
            "submittedAt": "2025-02-19T12:00:00",
            "description": null,
            "storedFileIds": []
          },
          "points": null,
          "files": []
        },
        {
          "homeworkId": "ee0e8400-e29b-41d4-a716-446655440009",
          "submission": null,
          "points": null,
          "files": []
        }
      ]
    }
  ]
}
```

В первом студенте оба ДЗ отправлены (есть submission, баллы и файлы где есть). У второго студента: по первому ДЗ — отправка без файлов и без выставленных баллов (`points`: null); по второму ДЗ — нет отправки (`submission`: null, `points`: null, `files`: []).
