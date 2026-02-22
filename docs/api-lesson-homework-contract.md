# Контракт API: таблица домашних заданий по уроку (преподаватель)

Документ-контракт для фронтенда. Описывает эндпоинты, необходимые для реализации экрана «Таблица домашних заданий по уроку» для преподавателя: загрузка данных таблицы (студенты × домашние задания), скачивание документа (файл студента), скачивание архива всех отправок по ДЗ, выставление и редактирование оценок. Задействованы модули **Composition**, **Document**, **Grades**, **Submission**.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/composition` | Агрегирующие эндпоинты (данные таблицы по уроку) |
| `/api/documents` | Метаданные и скачивание файлов (document) |
| `/api/grades` | Оценки: создание, обновление, просмотр |
| `/api/homework` | Отправки по домашнему заданию и архив |
| `/api/submissions` | Отправка по id (просмотр, удаление) |

Хост и порт задаются окружением. В описании ниже указаны путь и query.

### 1.2 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (или принятым в проекте механизмом).
- **Таблица по уроку и скачивание архива:** преподаватель этого урока или администратор/модератор.
- **Скачивание файла (документа):** владелец файла (uploadedBy) или администратор/модератор.
- **Оценки (создание, обновление, удаление):** роль **TEACHER** или **ADMIN**.
- При отсутствии или невалидном JWT возвращается **401 Unauthorized**, при недостатке прав — **403 Forbidden**.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID.
- **Даты и время** — **ISO-8601** (дата-время `"2025-02-21T12:00:00"`, дата `"2025-02-21"`). Время урока — **HH:mm:ss** (например, `"13:00:00"`).
- **Числа** — JSON number; баллы могут быть с дробной частью (BigDecimal).
- **null** — явное отсутствие значения; поля могут отсутствовать в JSON или быть `null` по контракту.
- **Массивы** — всегда массив (например, `[]` при отсутствии элементов).
- **Enum** в JSON передаётся строкой (например, `"HOMEWORK"`, `"SEMINAR"`).

### 1.4 Формат ошибок

При ошибке (4xx, 5xx) сервер возвращает JSON **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `NOT_FOUND`, `UNAUTHORIZED`, `GRADE_ENTRY_NOT_FOUND`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент ошибки (ISO-8601, UTC) |
| `details` | object \| null | Для валидации — объект «поле → сообщение»; иначе может отсутствовать или быть `null` |

Пример (404):

```json
{
  "code": "STORED_FILE_NOT_FOUND",
  "message": "Stored file not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-21T12:00:00.123Z",
  "details": null
}
```

Пример валидации (400, в `details` — поля запроса):

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "timestamp": "2025-02-21T12:00:00.123Z",
  "details": { "points": "points must be at least -9999.99" }
}
```

---

## 2. Модели данных (переиспользуемые DTO)

### 2.1 LessonDto (урок)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id урока |
| `offeringId` | UUID | Id офферинга |
| `offeringSlotId` | UUID \| null | Id слота офферинга |
| `date` | string | Дата урока (ISO-8601 date) |
| `startTime` | string | Время начала (HH:mm:ss) |
| `endTime` | string | Время окончания (HH:mm:ss) |
| `timeslotId` | UUID \| null | Id шаблона времени |
| `roomId` | UUID \| null | Id аудитории |
| `topic` | string \| null | Тема урока |
| `status` | string \| null | `PLANNED`, `CANCELLED`, `DONE` или `null` |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.2 StudentGroupDto (группа)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id группы |
| `programId` | UUID | Id программы |
| `curriculumId` | UUID | Id учебного плана |
| `code` | string \| null | Код группы |
| `name` | string \| null | Название группы |
| `description` | string \| null | Описание |
| `startYear` | integer | Год начала |
| `graduationYear` | integer \| null | Год выпуска |
| `curatorUserId` | UUID \| null | Id куратора |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.3 StudentDto (студент)

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

### 2.4 HomeworkDto (домашнее задание)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id домашнего задания |
| `lessonId` | UUID | Id урока |
| `title` | string \| null | Заголовок |
| `description` | string \| null | Описание |
| `points` | integer \| null | Максимальное количество баллов |
| `files` | array | Прикреплённые файлы — массив **StoredFileDto** (порядок сохранён) |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.5 StoredFileDto (файл в хранилище)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла |
| `size` | integer | Размер в байтах |
| `contentType` | string \| null | MIME-тип |
| `originalName` | string \| null | Оригинальное имя файла |
| `uploadedAt` | string | Дата загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 2.6 HomeworkSubmissionDto (отправка домашнего задания)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id отправки |
| `homeworkId` | UUID | Id домашнего задания |
| `authorId` | UUID | Id студента (автора) |
| `submittedAt` | string | Дата и время отправки (ISO-8601) |
| `description` | string \| null | Текстовое описание |
| `storedFileIds` | array of UUID | Id файлов в хранилище (порядок сохранён) |

### 2.7 GradeEntryDto (запись об оценке)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id записи |
| `studentId` | UUID | Id студента |
| `offeringId` | UUID | Id офферинга |
| `points` | number | Баллы |
| `typeCode` | string | Код типа: `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM` |
| `typeLabel` | string \| null | Подпись типа (обязательна для `CUSTOM`) |
| `description` | string \| null | Описание |
| `lessonSessionId` | UUID \| null | Id урока (сессии) |
| `homeworkSubmissionId` | UUID \| null | Id отправки ДЗ (если оценка за конкретную отправку) |
| `gradedBy` | UUID | Id пользователя, выставившего оценку |
| `gradedAt` | string | Дата и время выставления (ISO-8601) |
| `status` | string | `ACTIVE` или `VOIDED` |

### 2.8 GradeTypeCode (тип оценки)

Значения: `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM`. Для `CUSTOM` в запросе нужно передать `typeLabel`.

### 2.9 LessonHomeworkSubmissionsDto (данные таблицы по уроку)

Ответ агрегирующего эндпоинта таблицы «студенты × домашние задания».

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object | **LessonDto** (п. 2.1) |
| `group` | object | **StudentGroupDto** (п. 2.2) |
| `homeworks` | array | Список ДЗ урока (порядок = порядок колонок) — массив **HomeworkDto** (п. 2.4) |
| `studentRows` | array | Строки таблицы — массив **StudentHomeworkRowDto** (см. ниже) |

**StudentHomeworkRowDto** (одна строка — один студент):

| Поле | Тип | Описание |
|------|-----|----------|
| `student` | object | **StudentDto** (п. 2.3) |
| `items` | array | Ячейки по каждому ДЗ (тот же порядок, что и `homeworks`) — массив **StudentHomeworkItemDto** |

**StudentHomeworkItemDto** (одна ячейка — одно ДЗ для студента):

| Поле | Тип | Описание |
|------|-----|----------|
| `homeworkId` | UUID | Id домашнего задания |
| `submission` | object \| null | **HomeworkSubmissionDto** (п. 2.6); `null`, если студент не отправил |
| `points` | number \| null | Баллы за эту отправку; `null`, если нет отправки или оценка не выставлена |
| `files` | array | Метаданные файлов отправки — массив **StoredFileDto** (п. 2.5); пустой массив, если нет отправки или нет файлов |

---

## 3. Данные таблицы домашних заданий по уроку

### 3.1 GET /api/composition/lessons/{lessonId}/homework-submissions — данные для таблицы

**Назначение:** одна ручка для экрана «Таблица домашних заданий по уроку»: урок, группа, список ДЗ урока, по каждому студенту группы — по каждому ДЗ отправка (если есть), баллы и список файлов. Если студент не отправил — в ячейке `submission: null`, `points: null`, `files: []`.

**Метод и путь:** `GET /api/composition/lessons/{lessonId}/homework-submissions`

**Права доступа:** аутентифицированный пользователь с правом просмотра отправок и оценок (преподаватель урока или администратор).

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|-----------------|----------|
| path | `lessonId` | UUID | да | Id урока |

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK`

Тело — объект **LessonHomeworkSubmissionsDto** (п. 2.9).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Нет прав на просмотр отправок/оценок | Сообщение от модуля |
| 404 | NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | NOT_FOUND | Офферинг или группа не найдены | Соответствующее сообщение |

---

## 4. Скачивание документа (файл студента)

Используется для открытия/скачивания одного файла из ячейки таблицы (файл из отправки студента). Файлы в таблице заданы через **StoredFileDto** (п. 2.5); для скачивания используется `id` файла.

### 4.1 GET /api/documents/stored/{id} — метаданные файла

**Назначение:** получить метаданные хранимого файла (имя, размер, contentType). Нужны для отображения ссылки «скачать» с правильным именем.

**Метод и путь:** `GET /api/documents/stored/{id}`

**Права доступа:** аутентифицированный пользователь (доступ к содержимому проверяется при скачивании).

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `id` | UUID | да | Id хранимого файла (stored file) |

**Успешный ответ:** `200 OK`  
Тело — объект **StoredFileDto** (п. 2.5).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |

### 4.2 GET /api/documents/stored/{id}/download — скачивание файла (stream)

**Назначение:** скачать содержимое файла. Ответ — бинарный поток с заголовками `Content-Type` и `Content-Disposition` (имя файла).

**Метод и путь:** `GET /api/documents/stored/{id}/download`

**Права доступа:** владелец файла (`uploadedBy`) или администратор/модератор. Иначе **403 Forbidden**.

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `id` | UUID | да | Id хранимого файла |

**Успешный ответ:** `200 OK`  
- `Content-Type` — MIME-тип файла (или `application/octet-stream`).  
- `Content-Disposition: attachment; filename*=UTF-8''<encoded-name>`  
- Тело — бинарное содержимое файла.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | ACCESS_DENIED | Нет прав на доступ к файлу |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 404 | FILE_NOT_IN_STORAGE | Метаданные есть, файл в хранилище отсутствует |

### 4.3 GET /api/documents/stored/{id}/download-url — URL для скачивания

**Назначение:** получить одноразовый (presigned) URL для прямого скачивания файла (например, для открытия в новой вкладке).

**Метод и путь:** `GET /api/documents/stored/{id}/download-url`

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `id` | UUID | да | Id хранимого файла |
| query | `expires` | integer | нет | Время жизни URL в секундах (по умолчанию 3600) |

**Успешный ответ:** `200 OK`  
Тело: `{ "url": "<presigned URL>" }`.

**Права и ошибки:** те же, что для скачивания (п. 4.2).

---

## 5. Скачивание архива отправок по домашнему заданию

### 5.1 GET /api/homework/{homeworkId}/submissions/archive — архив всех отправок по ДЗ

**Назначение:** скачать ZIP-архив со всеми файлами всех отправок по указанному домашнему заданию. Для кнопки «Скачать все отправки» по одному ДЗ в таблице.

**Метод и путь:** `GET /api/homework/{homeworkId}/submissions/archive`

**Права доступа:** преподаватель урока, к которому привязано ДЗ, или администратор/модератор.

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `homeworkId` | UUID | да | Id домашнего задания |

**Успешный ответ:** `200 OK`  
- `Content-Type: application/zip`  
- `Content-Disposition: attachment; filename*=UTF-8''<имя архива>.zip`  
- Тело — бинарный ZIP (все прикреплённые файлы отправок по этому ДЗ, структура имён в архиве определяется бэкендом).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | SUBMISSION_PERMISSION_DENIED | Нет прав на просмотр отправок по этому ДЗ | `"You don't have permission for this action"` |
| 404 | SUBMISSION_HOMEWORK_NOT_FOUND | Домашнее задание не найдено | `"Homework not found: <homeworkId>"` |

---

## 6. Оценки: выставление и редактирование

Для таблицы домашних заданий типичные сценарии:  
- **Выставить баллы за одну ячейку (студент + ДЗ):** создать запись об оценке с привязкой к отправке (`homeworkSubmissionId`) и к уроку (`lessonSessionId`), тип `HOMEWORK`.  
- **Изменить баллы:** обновить существующую запись (PUT по id записи; id можно получить, например, из списка оценок студента по офферингу).  
- **Одна оценка на урок (без разбивки по ДЗ):** использовать ручку «установить баллы по уроку» (п. 6.4).

### 6.1 POST /api/grades/entries — создать запись об оценке

**Назначение:** создать одну запись об оценке. Для ячейки таблицы «студент × ДЗ» передать `lessonSessionId` (id урока), `homeworkSubmissionId` (id отправки из ячейки), `typeCode: "HOMEWORK"`.

**Метод и путь:** `POST /api/grades/entries`

**Права доступа:** TEACHER или ADMIN.

**Тело запроса — CreateGradeEntryRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | да | Id студента |
| `offeringId` | UUID | да | Id офферинга (есть в `lesson.offeringId` из таблицы) |
| `points` | number | да | Баллы (-9999.99 … 9999.99) |
| `typeCode` | string | да | Один из: `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM` |
| `typeLabel` | string | нет | До 255 символов; обязателен при `typeCode: "CUSTOM"` |
| `description` | string | нет | До 2000 символов |
| `lessonSessionId` | UUID | нет | Id урока (сессии) |
| `homeworkSubmissionId` | UUID | нет | Id отправки ДЗ (для привязки оценки к ячейке) |
| `gradedAt` | string (ISO-8601) | нет | Дата/время выставления; по умолчанию — текущее |

**Успешный ответ:** `201 Created`  
Тело — объект **GradeEntryDto** (п. 2.7).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | GRADE_VALIDATION_FAILED | Нарушение правил (например, CUSTOM без typeLabel, points вне диапазона) |
| 400 | VALIDATION_FAILED | Ошибки Bean Validation (см. `details`) |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Роль не TEACHER/ADMIN |
| 404 | GRADE_OFFERING_NOT_FOUND | Офферинг не найден |
| 404 | GRADE_STUDENT_NOT_FOUND | Студент не найден |

### 6.2 PUT /api/grades/entries/{id} — обновить запись об оценке

**Назначение:** изменить баллы, тип, описание или привязки у существующей записи. Запись должна быть в статусе ACTIVE. Для редактирования баллов в ячейке таблицы фронтенд должен знать id записи (например, из GET оценок студента по офферингу).

**Метод и путь:** `PUT /api/grades/entries/{id}`

**Права доступа:** TEACHER или ADMIN.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id записи об оценке (GradeEntry) |

**Тело запроса — UpdateGradeEntryRequest** (все поля опциональны; непустые перезаписывают значение):

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `points` | number | нет | Баллы (-9999.99 … 9999.99) |
| `typeCode` | string | нет | Тип (см. п. 2.8) |
| `typeLabel` | string | нет | До 255 символов |
| `description` | string | нет | До 2000 символов |
| `lessonSessionId` | UUID | нет | Id урока |
| `homeworkSubmissionId` | UUID | нет | Id отправки ДЗ |
| `gradedAt` | string (ISO-8601) | нет | Дата/время выставления |

**Успешный ответ:** `200 OK`  
Тело — объект **GradeEntryDto** (п. 2.7).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | GRADE_ENTRY_VOIDED | Запись в статусе VOIDED, обновлять нельзя |
| 400 | GRADE_VALIDATION_FAILED / VALIDATION_FAILED | Валидация полей |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Нет прав |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись не найдена |

### 6.3 GET /api/grades/entries/{id} — получить запись об оценке

**Назначение:** получить одну запись по id (например, перед редактированием или для отображения деталей).

**Метод и путь:** `GET /api/grades/entries/{id}`

**Права доступа:** аутентифицированный пользователь с правом просмотра оценок (TEACHER/ADMIN).

**Параметры пути:** `id` — UUID записи.

**Успешный ответ:** `200 OK` — **GradeEntryDto** (п. 2.7).  
**Ошибки:** 401, 403, 404 (NOT_FOUND при отсутствии записи).

### 6.4 PUT /api/grades/lessons/{lessonId}/students/{studentId}/points — установить баллы по уроку (одна ячейка на урок)

**Назначение:** установить или заменить баллы для одного студента по одному уроку «целиком» (без привязки к конкретному ДЗ). Все предыдущие ACTIVE-записи по этой паре (урок, студент) заменяются одной записью с новыми баллами. Удобно для экрана «одна оценка за урок», а не за каждое ДЗ отдельно.

**Метод и путь:** `PUT /api/grades/lessons/{lessonId}/students/{studentId}/points`

**Права доступа:** TEACHER или ADMIN.

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `lessonId` | UUID | да | Id урока |
| path | `studentId` | UUID | да | Id студента |
| body | `points` | number | да | Баллы (-9999.99 … 9999.99) |

**Тело запроса — SetLessonPointsRequest:**

```json
{ "points": 10 }
```

**Успешный ответ:** `200 OK`  
Тело — объект **GradeEntryDto** (п. 2.7) (созданная или обновлённая запись).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | GRADE_STUDENT_NOT_IN_GROUP | Студент не в группе офферинга этого урока |
| 400 | VALIDATION_FAILED | points вне диапазона (см. `details`) |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | GRADE_FORBIDDEN | Нет прав |
| 404 | GRADE_LESSON_NOT_FOUND | Урок не найден |
| 404 | GRADE_STUDENT_NOT_FOUND | Студент не найден |

### 6.5 DELETE /api/grades/entries/{id} — аннулировать запись (soft-delete)

**Назначение:** перевести запись в статус VOIDED; она перестаёт учитываться в итогах и не может быть изменена через PUT.

**Метод и путь:** `DELETE /api/grades/entries/{id}`

**Права доступа:** TEACHER или ADMIN.

**Успешный ответ:** `204 No Content` (тела нет).

**Ошибки:** 401, 403, 404 (GRADE_ENTRY_NOT_FOUND).

### 6.6 GET /api/grades/students/{studentId}/offerings/{offeringId} — оценки студента по офферингу

**Назначение:** получить все записи об оценках студента по офферингу (в т.ч. привязанные к урокам и к отправкам ДЗ), итог и разбивку по типу. Нужно для получения id записей (например, для привязки к ячейке таблицы при редактировании баллов).

**Метод и путь:** `GET /api/grades/students/{studentId}/offerings/{offeringId}`

**Параметры:**

| Место | Параметр | Тип | Обязательность | Описание |
|-------|----------|-----|----------------|----------|
| path | `studentId` | UUID | да | Id студента |
| path | `offeringId` | UUID | да | Id офферинга |
| query | `from` | string (ISO-8601) | нет | Фильтр по дате выставления (начало) |
| query | `to` | string (ISO-8601) | нет | Фильтр по дате выставления (конец) |
| query | `includeVoided` | boolean | нет | Включать аннулированные записи (по умолчанию false) |

**Успешный ответ:** `200 OK`  
Тело — **StudentOfferingGradesDto**:

| Поле | Тип | Описание |
|------|-----|----------|
| `studentId` | UUID | Id студента |
| `offeringId` | UUID | Id офферинга |
| `entries` | array | Список **GradeEntryDto** (п. 2.7) |
| `totalPoints` | number | Сумма баллов (только ACTIVE) |
| `breakdownByType` | object | Сумма баллов по каждому `typeCode` (ключ — строка типа) |

**Ошибки:** 401, 403, 404 (GRADE_STUDENT_NOT_FOUND, GRADE_OFFERING_NOT_FOUND и т.д.).

---

## 7. Отправки домашних заданий (дополнительно для таблицы)

Данные по отправкам уже приходят в **LessonHomeworkSubmissionsDto**. Отдельные ручки полезны для просмотра одной отправки или списка по одному ДЗ.

### 7.1 GET /api/homework/{homeworkId}/submissions — список отправок по ДЗ

**Назначение:** список всех отправок по указанному домашнему заданию (для преподавателя).

**Метод и путь:** `GET /api/homework/{homeworkId}/submissions`

**Права доступа:** TEACHER или ADMIN.

**Параметры пути:** `homeworkId` — UUID домашнего задания.

**Успешный ответ:** `200 OK`  
Тело — массив **HomeworkSubmissionDto** (п. 2.6).

**Ошибки:** 401, 403 (SUBMISSION_PERMISSION_DENIED), 404 (SUBMISSION_HOMEWORK_NOT_FOUND).

### 7.2 GET /api/submissions/{submissionId} — одна отправка по id

**Назначение:** получить одну отправку по id (например, из ячейки таблицы).

**Метод и путь:** `GET /api/submissions/{submissionId}`

**Права доступа:** TEACHER или ADMIN.

**Успешный ответ:** `200 OK` — **HomeworkSubmissionDto** (п. 2.6).  
**Ошибки:** 401, 403, 404 (SUBMISSION_NOT_FOUND).

---

## 8. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | VALIDATION_FAILED, GRADE_VALIDATION_FAILED, GRADE_ENTRY_VOIDED, GRADE_STUDENT_NOT_IN_GROUP, GRADE_OFFERING_NOT_FOR_GROUP | Валидация запроса, аннулированная запись, студент не в группе |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN, ACCESS_DENIED, GRADE_FORBIDDEN, SUBMISSION_PERMISSION_DENIED | Нет прав на действие |
| 404 | NOT_FOUND, STORED_FILE_NOT_FOUND, FILE_NOT_IN_STORAGE, GRADE_ENTRY_NOT_FOUND, GRADE_OFFERING_NOT_FOUND, GRADE_STUDENT_NOT_FOUND, GRADE_GROUP_NOT_FOUND, GRADE_LESSON_NOT_FOUND, SUBMISSION_NOT_FOUND, SUBMISSION_HOMEWORK_NOT_FOUND, SUBMISSION_FILE_NOT_FOUND | Сущность не найдена |
| 409 | FILE_IN_USE | Файл используется, удаление невозможно |
| 422 | SUBMISSION_SAVE_FAILED | Ошибка сохранения отправки |
| 500 | INTERNAL_ERROR, UPLOAD_FAILED, SAVE_FAILED | Внутренняя ошибка сервера |
| 503 | STORAGE_UNAVAILABLE | Хранилище файлов недоступно |

---

## 9. Примеры запросов и ответов

### 9.1 Загрузка таблицы домашних заданий по уроку

**Запрос:**

```
GET /api/composition/lessons/550e8400-e29b-41d4-a716-446655440000/homework-submissions
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK` (фрагмент)

```json
{
  "lesson": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "offeringId": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2025-02-21",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "topic": "Algorithms",
    "status": "DONE"
  },
  "group": { "id": "...", "name": "Group A", "code": "CS-2024-1" },
  "homeworks": [
    { "id": "hw-uuid-1", "lessonId": "550e8400-...", "title": "Task 1", "points": 10, "files": [] }
  ],
  "studentRows": [
    {
      "student": { "id": "st-1", "userId": "...", "studentId": "S001", "chineseName": "张三" },
      "items": [
        {
          "homeworkId": "hw-uuid-1",
          "submission": {
            "id": "sub-uuid-1",
            "homeworkId": "hw-uuid-1",
            "authorId": "st-1",
            "submittedAt": "2025-02-20T18:00:00",
            "description": null,
            "storedFileIds": ["file-uuid-1"]
          },
          "points": 8,
          "files": [
            { "id": "file-uuid-1", "size": 1024, "contentType": "application/pdf", "originalName": "solution.pdf", "uploadedAt": "2025-02-20T18:00:00", "uploadedBy": "..." }
          ]
        }
      ]
    }
  ]
}
```

### 9.2 Скачивание файла из ячейки

**Запрос:**

```
GET /api/documents/stored/file-uuid-1/download
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`  
Заголовки: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename*=UTF-8''solution.pdf`  
Тело: бинарное содержимое файла.

### 9.3 Скачивание архива отправок по ДЗ

**Запрос:**

```
GET /api/homework/hw-uuid-1/submissions/archive
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`  
`Content-Type: application/zip`, `Content-Disposition: attachment; filename*=UTF-8''homework-...-submissions.zip`  
Тело: ZIP-архив.

### 9.4 Выставить баллы за отправку (ячейка таблицы)

**Запрос:**

```
POST /api/grades/entries
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "studentId": "st-1",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "points": 8,
  "typeCode": "HOMEWORK",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "homeworkSubmissionId": "sub-uuid-1"
}
```

**Ответ:** `201 Created`

```json
{
  "id": "grade-entry-uuid-1",
  "studentId": "st-1",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "points": 8,
  "typeCode": "HOMEWORK",
  "typeLabel": null,
  "description": null,
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "homeworkSubmissionId": "sub-uuid-1",
  "gradedBy": "teacher-user-uuid",
  "gradedAt": "2025-02-21T14:00:00",
  "status": "ACTIVE"
}
```

### 9.5 Изменить баллы (редактирование записи)

**Запрос:**

```
PUT /api/grades/entries/grade-entry-uuid-1
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "points": 9
}
```

**Ответ:** `200 OK` — объект **GradeEntryDto** с обновлёнными `points` (и при необходимости другими полями).

### 9.6 Установить баллы по уроку (одна оценка на студента за урок)

**Запрос:**

```
PUT /api/grades/lessons/550e8400-e29b-41d4-a716-446655440000/students/st-1/points
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "points": 10
}
```

**Ответ:** `200 OK` — **GradeEntryDto** (созданная или заменённая запись).
