# Контракт API: Lesson Details (детали урока преподавателя)

Документ-контракт для фронтенда. Описывает полный сценарий реализации страницы Lesson Details: шапка урока, блок "Lesson Materials", блок "Homework Assignment" (в карусели), и таблица "Class Work & Attendance" (в карусели). Все эндпоинты используют JWT текущего пользователя; для редактирования данных требуется роль преподавателя урока.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/schedule` | Уроки (расписание); получение данных урока |
| `/api/lessons` | Материалы урока и домашние задания |
| `/api/attendance` | Посещаемость студентов по урокам |
| `/api/grades` | Оценки (баллы) студентов |
| `/api/documents` | Загрузка, скачивание и управление файлами (stored files) |
| `/api/offerings` | Информация об offering (предмет для группы) |

Хост и порт задаются окружением. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы выполняются с **JWT в cookie** (или заголовок `Authorization: Bearer <JWT>` — в зависимости от настройки приложения). Токен выдаётся при входе.
- **Эндпоинты редактирования** (создание/обновление материалов, домашних заданий, отметок посещаемости, оценок): доступны только пользователям с ролью **TEACHER** (преподаватель урока) или **ADMIN/MODERATOR/SUPER_ADMIN**.
- **Эндпоинты чтения**: доступны любому аутентифицированному пользователю, который имеет доступ к уроку (преподаватель урока, студент группы или админ).

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки **ISO-8601**: дата-время `"2025-02-05T12:00:00"`, дата `"2025-02-05"`, время `"13:00:00"`.
- **Числа** — JSON number; для оценок может использоваться число с дробной частью (BigDecimal).
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.
- **Enum** — строковые значения (например, `"PRESENT"`, `"ABSENT"`, `"LATE"`, `"EXCUSED"` для посещаемости).

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `UNAUTHORIZED`, `NOT_FOUND`, `FORBIDDEN`, `LESSON_NOT_FOUND`, `VALIDATION_FAILED`) |
| `message` | string | Человекочитаемое сообщение (не локализовано, для отладки) |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально; для валидации — объект «поле → сообщение» |

Пример (401):

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

Пример (404):

```json
{
  "code": "LESSON_NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

Пример (400, валидация):

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Ошибка проверки данных. Проверьте заполненные поля.",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": {
    "title": "must not be blank",
    "dueDate": "must be a valid date"
  }
}
```

---

## 2. Module Overview & Scope

### 2.1 Entities & Terminology

- **Lesson** — занятие (урок) в расписании. Имеет дату, время начала/окончания, аудиторию, связь с offering (предмет для группы).
- **Lesson Session** — то же, что Lesson. В модуле attendance используется термин "session", где `sessionId` = `lessonId`.
- **Lesson Material** — материал урока (бизнес-сущность, связывающая урок с файлами). Один урок может иметь несколько материалов; один материал может содержать несколько файлов.
- **Homework** — домашнее задание, привязанное к уроку. Содержит заголовок, описание, опционально файл и баллы.
- **Attendance Record** — запись посещаемости студента на уроке. Статусы: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`. Одна запись на студента на урок (уникальное ограничение).
- **Grade Entry** — запись в журнале оценок (баллы студента по предмету). Может быть привязана к уроку через `lessonSessionId` (где `lessonSessionId` = `lessonId`). Типы: `SEMINAR`, `EXAM`, `COURSEWORK`, `HOMEWORK`, `OTHER`, `CUSTOM`.
- **Class Work** — комбинация посещаемости и оценки за работу на уроке для одного студента. В UI отображается как одна строка таблицы с полями: студент, посещаемость, оценка за урок (class grade), комментарий преподавателя.

### 2.2 Load Strategy (UI workflow)

**При открытии страницы Lesson Details:**

1. **Сразу загружаются (параллельно):**
   - `GET /api/schedule/lessons/{lessonId}` — данные урока (базовая информация)
   - `GET /api/lessons/{lessonId}/materials` — список материалов урока
   - Дополнительные запросы для расширенной информации (см. раздел 3.1)

2. **Ленивая загрузка карусели:**
   - По умолчанию активна вкладка "Class Work"
   - При первом открытии вкладки "Class Work": `GET /api/attendance/sessions/{sessionId}` (где `sessionId` = `lessonId`) + запрос оценок по offering
   - При переключении на вкладку "Homework": `GET /api/lessons/{lessonId}/homework` (возвращает список; если пустой, домашнего задания нет)

**При редактировании:**

- **Материалы:** добавление/удаление — отдельные запросы, обновление UI после успешного ответа
- **Домашнее задание:** редактирование через `PUT /api/homework/{homeworkId}` или создание через `POST /api/lessons/{lessonId}/homework`
- **Class Work:** обновление посещаемости по одному студенту через `PUT /api/attendance/sessions/{sessionId}/students/{studentId}` или батчем через `POST /api/attendance/sessions/{sessionId}/records/bulk`; оценки создаются/обновляются через `/api/grades/entries`

---

## 3. Endpoints

### 3.1 Lesson Header

#### 3.1.1 GET /api/schedule/lessons/{id} — получить данные урока

**Назначение:** получить базовую информацию об уроке: ID, offeringId, дата, время начала/окончания, аудитория (roomId), тема, статус.

**Метод и путь:** `GET /api/schedule/lessons/{id}`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID урока |

**Успешный ответ:** `200 OK` — объект **LessonDto**.

**Структура LessonDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID урока |
| `offeringId` | UUID | ID offering (предмет для группы) |
| `offeringSlotId` | UUID \| null | ID слота offering (опционально) |
| `date` | string (ISO-8601 date) | Дата урока (например, `"2025-10-08"`) |
| `startTime` | string (HH:mm:ss) | Время начала (например, `"13:00:00"`) |
| `endTime` | string (HH:mm:ss) | Время окончания (например, `"14:30:00"`) |
| `timeslotId` | UUID \| null | ID временного слота (опционально) |
| `roomId` | UUID \| null | ID аудитории (если назначена) |
| `topic` | string \| null | Тема урока (опционально) |
| `status` | string | Статус урока (`"planned"`, `"cancelled"`, `"done"`) |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | LESSON_NOT_FOUND | Урок не найден |

**Пример ответа:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "offeringSlotId": "770e8400-e29b-41d4-a716-446655440002",
  "date": "2025-10-08",
  "startTime": "13:00:00",
  "endTime": "14:30:00",
  "timeslotId": "880e8400-e29b-41d4-a716-446655440003",
  "roomId": "990e8400-e29b-41d4-a716-446655440004",
  "topic": "Introduction to Algorithms",
  "status": "planned",
  "createdAt": "2025-10-01T10:00:00",
  "updatedAt": "2025-10-01T10:00:00"
}
```

**GAP:** Для отображения полной шапки урока фронтенду нужно дополнительно получить:
- Информацию о предмете (subject) через offering → curriculumSubject → subject
- Информацию о группе через offering → group
- Информацию о преподавателе через offering → teachers
- Информацию об аудитории через `GET /api/schedule/rooms/{roomId}` (если `roomId` не null)

**Рекомендуемая последовательность запросов для шапки:**

1. `GET /api/schedule/lessons/{lessonId}` → получить `offeringId`, `roomId`
2. Параллельно:
   - `GET /api/offerings/{offeringId}` → получить `groupId`, `curriculumSubjectId`, `teacherId`
   - `GET /api/schedule/rooms/{roomId}` (если `roomId` не null) → получить информацию об аудитории
3. Параллельно:
   - `GET /api/offerings/{offeringId}/teachers` → получить список преподавателей
   - `GET /api/groups/{groupId}` → получить информацию о группе
   - `GET /api/programs/curriculum-subjects/{curriculumSubjectId}` → получить `subjectId`
4. `GET /api/subjects/{subjectId}` → получить информацию о предмете

**Примечание:** Для упрощения фронтенда рекомендуется создать агрегированный эндпоинт `GET /api/schedule/lessons/{id}/details`, который возвращает всю необходимую информацию одним запросом (см. раздел 10.1).

---

#### 3.1.2 GET /api/schedule/rooms/{id} — получить информацию об аудитории

**Назначение:** получить информацию об аудитории для отображения в шапке урока.

**Метод и путь:** `GET /api/schedule/rooms/{id}`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID аудитории |

**Успешный ответ:** `200 OK` — объект **RoomDto**.

**Структура RoomDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID аудитории |
| `buildingId` | UUID | ID здания |
| `buildingName` | string | Название здания |
| `number` | string | Номер аудитории (например, `"208"`) |
| `capacity` | integer \| null | Вместимость |
| `type` | string \| null | Тип аудитории |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | ROOM_NOT_FOUND | Аудитория не найдена |

---

### 3.2 Lesson Materials

#### 3.2.1 GET /api/lessons/{lessonId}/materials — получить список материалов урока

**Назначение:** получить список всех материалов урока для отображения в блоке "Lesson Materials".

**Метод и путь:** `GET /api/lessons/{lessonId}/materials`

**Права доступа:** аутентифицированный пользователь с доступом к уроку.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Успешный ответ:** `200 OK` — массив объектов **LessonMaterialDto**.

**Структура LessonMaterialDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID материала |
| `lessonId` | UUID | ID урока |
| `name` | string | Название материала |
| `description` | string \| null | Описание (опционально) |
| `authorId` | UUID | ID автора материала |
| `publishedAt` | string (ISO-8601 datetime) | Дата публикации |
| `files` | array of **StoredFileDto** | Список файлов материала (упорядоченный) |

**StoredFileDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID файла |
| `size` | integer | Размер файла в байтах |
| `contentType` | string | MIME-тип файла |
| `originalName` | string | Оригинальное имя файла |
| `uploadedAt` | string (ISO-8601 datetime) | Дата загрузки |
| `uploadedBy` | UUID | ID пользователя, загрузившего файл |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | LESSON_NOT_FOUND | Урок не найден |

**Пример ответа:**

```json
[
  {
    "id": "bb0e8400-e29b-41d4-a716-446655440006",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Lecture Slides - Introduction to Algorithms",
    "description": null,
    "authorId": "990e8400-e29b-41d4-a716-446655440004",
    "publishedAt": "2025-10-07T10:00:00",
    "files": [
      {
        "id": "cc0e8400-e29b-41d4-a716-446655440007",
        "size": 2411520,
        "contentType": "application/pdf",
        "originalName": "Lecture Slides - Introduction to Algorithms.pdf",
        "uploadedAt": "2025-10-07T10:00:00",
        "uploadedBy": "990e8400-e29b-41d4-a716-446655440004"
      }
    ]
  }
]
```

---

#### 3.2.2 POST /api/lessons/{lessonId}/materials — создать материал урока

**Назначение:** создать новый материал урока с прикреплёнными файлами.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Тело запроса:** объект **CreateLessonMaterialRequest**.

**Структура CreateLessonMaterialRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `name` | string | обязательное | Название материала (макс. 500 символов) |
| `description` | string \| null | опционально | Описание материала (макс. 5000 символов) |
| `publishedAt` | string (ISO-8601 datetime) | обязательное | Дата публикации |
| `storedFileIds` | array of UUID | опционально | Список ID файлов для прикрепления (файлы должны быть загружены заранее через `POST /api/documents/upload`) |

**Успешный ответ:** `201 Created` — объект **LessonMaterialDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | LESSON_NOT_FOUND | Урок не найден |
| 404 | STORED_FILE_NOT_FOUND | Один из файлов не найден |
| 400 | VALIDATION_FAILED | Ошибка валидации (пустое название, неверный формат даты) |

**Пример запроса:**

```json
{
  "name": "Additional Reading",
  "description": "Supplementary materials for this lesson",
  "publishedAt": "2025-10-08T09:00:00",
  "storedFileIds": ["ff0e8400-e29b-41d4-a716-446655440010"]
}
```

---

#### 3.2.3 DELETE /api/lessons/{lessonId}/materials/{materialId} — удалить материал урока

**Назначение:** удалить материал урока. Файлы удаляются из хранилища, если они не используются в других материалах.

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** автор материала или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |
| `materialId` | UUID | ID материала |

**Успешный ответ:** `204 No Content` (тело отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является автором материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |

---

#### 3.2.4 DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId} — удалить файл из материала

**Назначение:** удалить файл из материала урока. Файл удаляется из хранилища, если он не используется в других материалах.

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}`

**Права доступа:** автор материала или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |
| `materialId` | UUID | ID материала |
| `storedFileId` | UUID | ID файла |

**Успешный ответ:** `204 No Content` (тело отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является автором материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |

---

#### 3.2.5 POST /api/documents/upload — загрузить файл

**Назначение:** загрузить файл в хранилище перед прикреплением к материалу. Возвращает метаданные файла, включая `id`, который используется в `storedFileIds` при создании материала.

**Метод и путь:** `POST /api/documents/upload`

**Права доступа:** аутентифицированный пользователь.

**Тело запроса:** `multipart/form-data` с полем `file` (тип `MultipartFile`).

**Успешный ответ:** `200 OK` — объект **StoredFileDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 413 | FILE_TOO_LARGE | Файл слишком большой |
| 400 | FORBIDDEN_FILE_TYPE | Тип файла запрещён |
| 400 | MALWARE_DETECTED | Обнаружен вредоносный код |

---

#### 3.2.6 GET /api/documents/stored/{id}/download — скачать файл (поток)

**Назначение:** скачать файл через бэкенд (сервер возвращает файл как поток). Используется для прямого скачивания в браузере.

**Метод и путь:** `GET /api/documents/stored/{id}/download`

**Права доступа:** владелец файла (`uploadedBy` == текущий пользователь) или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID файла (`file.id` из материала) |

**Успешный ответ:** `200 OK` — бинарный поток файла.

**Заголовки ответа:**
- `Content-Type`: MIME-тип файла (из метаданных)
- `Content-Disposition`: `attachment; filename*=UTF-8''<encoded_filename>` — имя файла для сохранения

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Доступ запрещён |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |

**Альтернатива:** использовать `GET /api/documents/stored/{id}/download-url` для получения presigned URL для прямого скачивания из хранилища.

---

### 3.3 Homework Assignment

#### 3.3.1 GET /api/lessons/{lessonId}/homework — получить домашнее задание урока

**Назначение:** получить список домашних заданий для урока (если они есть). Используется для ленивой загрузки вкладки "Homework" в карусели.

**Метод и путь:** `GET /api/lessons/{lessonId}/homework`

**Права доступа:** аутентифицированный пользователь с доступом к уроку.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Успешный ответ:** `200 OK` — массив объектов **HomeworkDto** (может быть пустым, если домашнего задания нет).

**Структура HomeworkDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID домашнего задания |
| `lessonId` | UUID | ID урока |
| `title` | string | Заголовок домашнего задания |
| `description` | string \| null | Описание (многострочный текст) |
| `points` | integer \| null | Максимальное количество баллов (опционально) |
| `file` | object **StoredFileDto** \| null | Прикреплённый файл (опционально, обёрнут в Optional) |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Примечание:** В текущей реализации `file` имеет тип `Optional<StoredFileDto>`, что в JSON сериализуется как `null` или объект.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | LESSON_NOT_FOUND | Урок не найден |

**Пример ответа (есть домашнее задание):**

```json
[
  {
    "id": "110e8400-e29b-41d4-a716-446655440011",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Algorithm Analysis Assignment",
    "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
    "points": 100,
    "file": {
      "id": "120e8400-e29b-41d4-a716-446655440012",
      "size": 512000,
      "contentType": "application/pdf",
      "originalName": "assignment.pdf",
      "uploadedAt": "2025-10-08T12:00:00",
      "uploadedBy": "990e8400-e29b-41d4-a716-446655440004"
    },
    "createdAt": "2025-10-08T12:00:00",
    "updatedAt": "2025-10-08T12:00:00"
  }
]
```

**Пример ответа (нет домашнего задания):**

```json
[]
```

**GAP:** В текущей реализации API возвращает список домашних заданий. Для UI экрана Lesson Details предполагается, что на урок может быть одно домашнее задание (или ни одного). Фронтенд должен взять первый элемент списка (если список не пустой) или показать пустое состояние, если список пустой.

**Рекомендация:** Рассмотреть создание эндпоинта `GET /api/lessons/{lessonId}/homework/current`, который возвращает одно домашнее задание или `null` (см. раздел 10.1).

---

#### 3.3.2 POST /api/lessons/{lessonId}/homework — создать домашнее задание

**Назначение:** создать новое домашнее задание для урока.

**Метод и путь:** `POST /api/lessons/{lessonId}/homework`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Тело запроса:** объект **CreateHomeworkRequest**.

**Структура CreateHomeworkRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | обязательное | Заголовок домашнего задания (макс. 500 символов) |
| `description` | string \| null | опционально | Описание (многострочный текст, макс. 5000 символов) |
| `points` | integer \| null | опционально | Максимальное количество баллов (положительное число) |
| `storedFileId` | UUID \| null | опционально | ID прикреплённого файла (файл должен быть загружен заранее) |

**Успешный ответ:** `201 Created` — объект **HomeworkDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | LESSON_NOT_FOUND | Урок не найден |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 400 | VALIDATION_FAILED | Ошибка валидации (пустой заголовок, отрицательные баллы) |

**Пример запроса:**

```json
{
  "title": "Algorithm Analysis Assignment",
  "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
  "points": 100,
  "storedFileId": null
}
```

---

#### 3.3.3 GET /api/homework/{homeworkId} — получить домашнее задание по ID

**Назначение:** получить одно домашнее задание по его ID (используется после создания или для обновления).

**Метод и путь:** `GET /api/homework/{homeworkId}`

**Права доступа:** аутентифицированный пользователь с доступом к уроку.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Успешный ответ:** `200 OK` — объект **HomeworkDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |

---

#### 3.3.4 PUT /api/homework/{homeworkId} — обновить домашнее задание

**Назначение:** обновить существующее домашнее задание. Используется при нажатии кнопки "Edit" в блоке "Homework Assignment".

**Метод и путь:** `PUT /api/homework/{homeworkId}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Тело запроса:** объект **UpdateHomeworkRequest**.

**Структура UpdateHomeworkRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string \| null | опционально | Заголовок (макс. 500 символов) |
| `description` | string \| null | опционально | Описание (макс. 5000 символов) |
| `points` | integer \| null | опционально | Максимальное количество баллов |
| `clearFile` | boolean \| null | опционально | Если `true`, удалить ссылку на файл (файл в хранилище не удаляется) |
| `storedFileId` | UUID \| null | опционально | Новый ID файла (используется только если `clearFile` не `true`) |

**Успешный ответ:** `200 OK` — объект **HomeworkDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 400 | VALIDATION_FAILED | Ошибка валидации |

**Пример запроса:**

```json
{
  "title": "Algorithm Analysis Assignment (Updated)",
  "description": "Analyze the time complexity of the provided algorithms and implement optimized versions. Submit your solution by email.",
  "points": 100,
  "clearFile": false,
  "storedFileId": null
}
```

---

#### 3.3.5 DELETE /api/homework/{homeworkId} — удалить домашнее задание

**Назначение:** удалить домашнее задание. Прикреплённый файл не удаляется из хранилища.

**Метод и путь:** `DELETE /api/homework/{homeworkId}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Успешный ответ:** `204 No Content` (тело отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |

---

### 3.4 Class Work & Attendance

#### 3.4.1 GET /api/attendance/sessions/{sessionId} — получить данные посещаемости для урока

**Назначение:** получить таблицу студентов с их посещаемостью для урока. Используется для ленивой загрузки вкладки "Class Work" в карусели.

**Важно:** `sessionId` = `lessonId` (урок и есть сессия посещаемости).

**Метод и путь:** `GET /api/attendance/sessions/{sessionId}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | ID урока (sessionId = lessonId) |

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `includeCanceled` | boolean | опционально | Включать отменённые записи (по умолчанию `false`) |

**Успешный ответ:** `200 OK` — объект **SessionAttendanceDto**.

**Структура SessionAttendanceDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `sessionId` | UUID | ID сессии (урока) |
| `counts` | object (Map<AttendanceStatus, Integer>) | Количество студентов по статусам посещаемости |
| `unmarkedCount` | integer | Количество студентов без отметки посещаемости |
| `students` | array of **SessionAttendanceStudentDto** | Список студентов с данными посещаемости |

**SessionAttendanceStudentDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `studentId` | UUID | ID студента |
| `status` | string (enum) \| null | Статус посещаемости: `"PRESENT"`, `"ABSENT"`, `"LATE"`, `"EXCUSED"` или `null` (если не отмечена) |
| `minutesLate` | integer \| null | Минуты опоздания (только для статуса `"LATE"`) |
| `teacherComment` | string \| null | Комментарий преподавателя |
| `markedAt` | string (ISO-8601 datetime) \| null | Дата и время отметки |
| `markedBy` | UUID \| null | ID пользователя, поставившего отметку |
| `absenceNoticeId` | UUID \| null | ID уведомления об отсутствии (если прикреплено) |
| `notices` | array of **StudentNoticeDto** | Список уведомлений об отсутствии для этого студента и сессии |

**StudentNoticeDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID уведомления |
| `type` | string (enum) | Тип уведомления |
| `status` | string (enum) | Статус уведомления |
| `reasonText` | string \| null | Текст причины отсутствия |
| `submittedAt` | string (ISO-8601 datetime) | Дата подачи |
| `fileIds` | array of string | Список ID файлов |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | LESSON_NOT_FOUND | Урок не найден |

**Пример ответа:**

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "counts": {
    "PRESENT": 3,
    "ABSENT": 0,
    "LATE": 1,
    "EXCUSED": 0
  },
  "unmarkedCount": 0,
  "students": [
    {
      "studentId": "220e8400-e29b-41d4-a716-446655440012",
      "status": "PRESENT",
      "minutesLate": null,
      "teacherComment": null,
      "markedAt": "2025-10-08T13:05:00",
      "markedBy": "990e8400-e29b-41d4-a716-446655440004",
      "absenceNoticeId": null,
      "notices": []
    },
    {
      "studentId": "330e8400-e29b-41d4-a716-446655440013",
      "status": "LATE",
      "minutesLate": 15,
      "teacherComment": null,
      "markedAt": "2025-10-08T13:15:00",
      "markedBy": "990e8400-e29b-41d4-a716-446655440004",
      "absenceNoticeId": null,
      "notices": []
    }
  ]
}
```

**GAP:** Данный эндпоинт возвращает только данные посещаемости. Для полной таблицы "Class Work" фронтенду нужно дополнительно получить оценки за урок (class grades) для каждого студента. Оценки можно получить через модуль grades, используя `lessonSessionId` = `lessonId` и фильтруя по типу `SEMINAR` или другому типу оценки за работу на уроке.

**Рекомендация:** Рассмотреть создание агрегированного эндпоинта `GET /api/lessons/{lessonId}/classwork`, который возвращает объединённые данные посещаемости и оценок (см. раздел 10.1).

---

#### 3.4.2 PUT /api/attendance/sessions/{sessionId}/students/{studentId} — обновить посещаемость студента

**Назначение:** отметить или обновить посещаемость одного студента на уроке.

**Метод и путь:** `PUT /api/attendance/sessions/{sessionId}/students/{studentId}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | ID урока (sessionId = lessonId) |
| `studentId` | UUID | ID студента |

**Тело запроса:** объект **MarkAttendanceRequest**.

**Структура MarkAttendanceRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `status` | string (enum) | обязательное | Статус: `"PRESENT"`, `"ABSENT"`, `"LATE"`, `"EXCUSED"` |
| `minutesLate` | integer \| null | опционально | Минуты опоздания (обязательно для `"LATE"`, должно быть `null` для других статусов) |
| `teacherComment` | string \| null | опционально | Комментарий преподавателя (макс. 2000 символов) |
| `absenceNoticeId` | UUID \| null | опционально | ID уведомления об отсутствии для прикрепления (взаимоисключающе с `autoAttachLastNotice`) |
| `autoAttachLastNotice` | boolean \| null | опционально | Если `true`, автоматически прикрепить последнее поданное уведомление для этого студента и сессии (взаимоисключающе с `absenceNoticeId`) |

**Успешный ответ:** `200 OK` — объект **AttendanceRecordDto**.

**Структура AttendanceRecordDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID записи посещаемости |
| `lessonSessionId` | UUID | ID урока |
| `studentId` | UUID | ID студента |
| `status` | string (enum) | Статус посещаемости |
| `minutesLate` | integer \| null | Минуты опоздания |
| `teacherComment` | string \| null | Комментарий преподавателя |
| `markedBy` | UUID | ID пользователя, поставившего отметку |
| `markedAt` | string (ISO-8601 datetime) | Дата и время отметки |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |
| `absenceNoticeId` | UUID \| null | ID уведомления об отсутствии |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | LESSON_NOT_FOUND | Урок не найден |
| 404 | STUDENT_NOT_FOUND | Студент не найден или не состоит в группе урока |
| 400 | VALIDATION_FAILED | Ошибка валидации (неверный статус посещаемости, minutesLate для не-LATE статуса) |

**Пример запроса:**

```json
{
  "status": "LATE",
  "minutesLate": 10,
  "teacherComment": null,
  "absenceNoticeId": null,
  "autoAttachLastNotice": false
}
```

---

#### 3.4.3 POST /api/attendance/sessions/{sessionId}/records/bulk — батч-обновление посещаемости

**Назначение:** отметить посещаемость для нескольких студентов одновременно (транзакция "всё или ничего").

**Метод и путь:** `POST /api/attendance/sessions/{sessionId}/records/bulk`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `sessionId` | UUID | ID урока (sessionId = lessonId) |

**Тело запроса:** объект **BulkMarkAttendanceRequest**.

**Структура BulkMarkAttendanceRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `items` | array of **MarkAttendanceItem** | обязательное | Список записей посещаемости |

**MarkAttendanceItem:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | обязательное | ID студента |
| `status` | string (enum) | обязательное | Статус: `"PRESENT"`, `"ABSENT"`, `"LATE"`, `"EXCUSED"` |
| `minutesLate` | integer \| null | опционально | Минуты опоздания (обязательно для `"LATE"`) |
| `teacherComment` | string \| null | опционально | Комментарий преподавателя (макс. 2000 символов) |
| `absenceNoticeId` | UUID \| null | опционально | ID уведомления об отсутствии |
| `autoAttachLastNotice` | boolean \| null | опционально | Автоматически прикрепить последнее уведомление |

**Успешный ответ:** `201 Created` — массив объектов **AttendanceRecordDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем урока |
| 404 | LESSON_NOT_FOUND | Урок не найден |
| 400 | VALIDATION_FAILED | Ошибка валидации (неверные данные в одном из элементов) |

---

#### 3.4.4 POST /api/grades/entries — создать оценку за урок

**Назначение:** создать запись оценки (баллов) для студента за работу на уроке. Используется для выставления "class grade" в таблице Class Work.

**Метод и путь:** `POST /api/grades/entries`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Тело запроса:** объект **CreateGradeEntryRequest**.

**Структура CreateGradeEntryRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `studentId` | UUID | обязательное | ID студента |
| `offeringId` | UUID | обязательное | ID offering (предмет для группы) |
| `points` | number (decimal) | обязательное | Баллы (может быть отрицательным для корректировок) |
| `typeCode` | string (enum) | обязательное | Тип оценки: `"SEMINAR"`, `"EXAM"`, `"COURSEWORK"`, `"HOMEWORK"`, `"OTHER"`, `"CUSTOM"` |
| `typeLabel` | string \| null | опционально | Метка типа (обязательно для `"CUSTOM"`) |
| `description` | string \| null | опционально | Описание оценки |
| `lessonSessionId` | UUID \| null | опционально | ID урока (для привязки оценки к уроку) |
| `homeworkSubmissionId` | UUID \| null | опционально | ID решения домашнего задания (если оценка за домашку) |
| `gradedAt` | string (ISO-8601 datetime) \| null | опционально | Дата выставления оценки (по умолчанию текущее время) |

**Успешный ответ:** `201 Created` — объект **GradeEntryDto**.

**Структура GradeEntryDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID записи оценки |
| `studentId` | UUID | ID студента |
| `offeringId` | UUID | ID offering |
| `points` | number (decimal) | Баллы |
| `typeCode` | string (enum) | Тип оценки |
| `typeLabel` | string \| null | Метка типа |
| `description` | string \| null | Описание |
| `lessonSessionId` | UUID \| null | ID урока |
| `homeworkSubmissionId` | UUID \| null | ID решения домашнего задания |
| `status` | string (enum) | Статус: `"ACTIVE"` или `"VOIDED"` |
| `gradedAt` | string (ISO-8601 datetime) | Дата выставления оценки |
| `gradedBy` | UUID | ID пользователя, выставившего оценку |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем |
| 404 | STUDENT_NOT_FOUND | Студент не найден |
| 404 | OFFERING_NOT_FOUND | Offering не найден |
| 400 | VALIDATION_FAILED | Ошибка валидации |

**Пример запроса:**

```json
{
  "studentId": "220e8400-e29b-41d4-a716-446655440012",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "points": 8.0,
  "typeCode": "SEMINAR",
  "typeLabel": null,
  "description": "Class participation",
  "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "homeworkSubmissionId": null,
  "gradedAt": null
}
```

---

#### 3.4.5 PUT /api/grades/entries/{id} — обновить оценку за урок

**Назначение:** обновить существующую оценку за урок (изменить баллы, тип, описание).

**Метод и путь:** `PUT /api/grades/entries/{id}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID записи оценки |

**Тело запроса:** объект **UpdateGradeEntryRequest**.

**Структура UpdateGradeEntryRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `points` | number (decimal) \| null | опционально | Новые баллы |
| `typeCode` | string (enum) \| null | опционально | Новый тип оценки |
| `typeLabel` | string \| null | опционально | Новая метка типа |
| `description` | string \| null | опционально | Новое описание |
| `lessonSessionId` | UUID \| null | опционально | Новый ID урока |
| `homeworkSubmissionId` | UUID \| null | опционально | Новый ID решения домашнего задания |
| `gradedAt` | string (ISO-8601 datetime) \| null | опционально | Новая дата выставления |

**Успешный ответ:** `200 OK` — объект **GradeEntryDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись оценки не найдена |
| 400 | VALIDATION_FAILED | Ошибка валидации |

---

#### 3.4.6 DELETE /api/grades/entries/{id} — удалить оценку за урок

**Назначение:** удалить оценку за урок (soft delete: статус меняется на `VOIDED`, оценка исключается из итоговых сумм).

**Метод и путь:** `DELETE /api/grades/entries/{id}`

**Права доступа:** преподаватель урока или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID записи оценки |

**Успешный ответ:** `204 No Content` (тело отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | FORBIDDEN | Пользователь не является преподавателем |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись оценки не найдена |

---

#### 3.4.7 GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary — получить оценки группы по offering

**Назначение:** получить сводку оценок всех студентов группы по offering. Используется для получения оценок за урок (фильтровать по `lessonSessionId` = `lessonId` и типу `SEMINAR`).

**Метод и путь:** `GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary`

**Права доступа:** преподаватель группы или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `groupId` | UUID | ID группы |
| `offeringId` | UUID | ID offering |

**Параметры query:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `from` | string (ISO-8601 datetime) | опционально | Начало периода фильтрации по `gradedAt` |
| `to` | string (ISO-8601 datetime) | опционально | Конец периода фильтрации по `gradedAt` |
| `includeVoided` | boolean | опционально | Включать удалённые оценки (по умолчанию `false`) |

**Успешный ответ:** `200 OK` — объект **GroupOfferingSummaryDto**.

**Структура GroupOfferingSummaryDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `groupId` | UUID | ID группы |
| `offeringId` | UUID | ID offering |
| `rows` | array of **GroupOfferingSummaryRow** | Список студентов с их оценками |

**GroupOfferingSummaryRow:**

| Поле | Тип | Описание |
|------|-----|----------|
| `studentId` | UUID | ID студента |
| `totalPoints` | number (decimal) | Итоговые баллы студента |
| `breakdownByType` | object (Map<string, BigDecimal>) | Разбивка баллов по типам |

**GAP:** Данный эндпоинт не позволяет фильтровать по `lessonSessionId` напрямую. Для получения оценок за конкретный урок фронтенду нужно:
1. Получить все оценки группы по offering
2. Отфильтровать на клиенте по `lessonSessionId` = `lessonId` и типу `SEMINAR`

**Рекомендация:** Рассмотреть добавление параметра `lessonSessionId` в query для фильтрации оценок по уроку (см. раздел 10.1).

---

## 4. Error Model

### 4.1 Единая структура ошибок

Все ошибки возвращаются в формате **ErrorResponse**:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message (not localized, for debugging)",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null | { ... }
}
```

### 4.2 Типовые коды ошибок

| HTTP | code | Когда возникает | Пример message |
|------|------|----------------|----------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | "Authentication required" |
| 403 | FORBIDDEN | Пользователь не имеет прав доступа | "Insufficient permissions" |
| 404 | LESSON_NOT_FOUND | Урок не найден | "Lesson not found: {id}" |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал урока не найден | "Lesson material not found: {id}" |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден | "Stored file not found: {id}" |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено | "Homework not found: {id}" |
| 404 | STUDENT_NOT_FOUND | Студент не найден | "Student not found: {id}" |
| 404 | OFFERING_NOT_FOUND | Offering не найден | "Offering not found: {id}" |
| 404 | GRADE_ENTRY_NOT_FOUND | Запись оценки не найдена | "Grade entry not found: {id}" |
| 400 | VALIDATION_FAILED | Ошибка валидации данных | "Ошибка проверки данных. Проверьте заполненные поля." |
| 400 | BAD_REQUEST | Неверный запрос (неверные параметры пагинации, формат даты) | "Invalid request parameters" |
| 413 | FILE_TOO_LARGE | Файл слишком большой | "File size exceeds maximum allowed size" |
| 500 | INTERNAL_SERVER_ERROR | Внутренняя ошибка сервера | "An unexpected error occurred" |

### 4.3 Field Validation Errors

При ошибке валидации (`VALIDATION_FAILED`) в поле `details` передаётся объект «поле → сообщение»:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Ошибка проверки данных. Проверьте заполненные поля.",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": {
    "title": "must not be blank",
    "dueDate": "must be a valid date",
    "points": "must be greater than 0"
  }
}
```

### 4.4 i18n (интернационализация)

- **Backend** возвращает только коды ошибок (`code`) и технические сообщения (`message`).
- **Frontend** локализует текст ошибок по коду (`code`).
- Сообщения в `message` предназначены для отладки и логирования, не для отображения пользователю.

---

## 5. Full Sequence Examples

### 5.1 Открытие страницы Lesson Details

**Последовательность запросов:**

1. **Параллельно:**
   ```
   GET /api/schedule/lessons/{lessonId}
   GET /api/lessons/{lessonId}/materials
   ```

2. **После получения `offeringId` и `roomId` из урока:**
   ```
   GET /api/offerings/{offeringId}
   GET /api/schedule/rooms/{roomId}  (если roomId не null)
   ```

3. **После получения `groupId`, `curriculumSubjectId`, `teacherId` из offering:**
   ```
   GET /api/offerings/{offeringId}/teachers
   GET /api/groups/{groupId}
   GET /api/programs/curriculum-subjects/{curriculumSubjectId}
   ```

4. **После получения `subjectId` из curriculumSubject:**
   ```
   GET /api/subjects/{subjectId}
   ```

5. **При открытии вкладки "Class Work" (ленивая загрузка):**
   ```
   GET /api/attendance/sessions/{lessonId}?includeCanceled=false
   GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary?includeVoided=false
   ```
   Затем фильтровать оценки на клиенте по `lessonSessionId` = `lessonId` и типу `SEMINAR`.

6. **При переключении на вкладку "Homework" (ленивая загрузка):**
   ```
   GET /api/lessons/{lessonId}/homework
   ```
   Взять первый элемент списка (если список не пустой) или показать пустое состояние.

---

### 5.2 Добавление материала урока

**Последовательность:**

1. Пользователь нажимает кнопку "Add" в блоке "Lesson Materials".
2. Открывается диалог загрузки файла (если файл ещё не загружен):
   ```
   POST /api/documents/upload
   Content-Type: multipart/form-data
   Body: file=<file>
   ```
   Ответ: `{ "id": "ff0e8400-e29b-41d4-a716-446655440010", ... }`

3. Создание материала с прикреплённым файлом:
   ```
   POST /api/lessons/{lessonId}/materials
   Body: {
     "name": "Additional Reading",
     "description": "Supplementary materials",
     "publishedAt": "2025-10-08T09:00:00",
     "storedFileIds": ["ff0e8400-e29b-41d4-a716-446655440010"]
   }
   ```
   Ответ: `201 Created` → `LessonMaterialDto`

4. Обновление UI: добавить новый материал в список.

---

### 5.3 Удаление материала урока

**Последовательность:**

1. Пользователь нажимает кнопку удаления (X) у материала.
2. Подтверждение удаления (опционально).
3. Запрос:
   ```
   DELETE /api/lessons/{lessonId}/materials/{materialId}
   ```
   Ответ: `204 No Content`

4. Обновление UI: удалить материал из списка.

---

### 5.4 Редактирование домашнего задания

**Последовательность:**

1. Пользователь нажимает кнопку "Edit" в блоке "Homework Assignment".
2. Открывается форма редактирования с текущими данными (из `GET /api/lessons/{lessonId}/homework`, взять первый элемент).
3. Пользователь изменяет данные и нажимает "Save".
4. Запрос:
   ```
   PUT /api/homework/{homeworkId}
   Body: {
     "title": "Algorithm Analysis Assignment (Updated)",
     "description": "Analyze the time complexity...",
     "points": 100,
     "clearFile": false,
     "storedFileId": null
   }
   ```
   Ответ: `200 OK` → `HomeworkDto`

5. Обновление UI: отобразить обновлённые данные домашнего задания.

---

### 5.5 Редактирование посещаемости студента

**Последовательность:**

1. Пользователь изменяет значение посещаемости в таблице "Class Work & Attendance" (например, статус с "PRESENT" на "LATE").
2. При изменении значения отправляется запрос:
   ```
   PUT /api/attendance/sessions/{lessonId}/students/{studentId}
   Body: {
     "status": "LATE",
     "minutesLate": 15,
     "teacherComment": null,
     "absenceNoticeId": null,
     "autoAttachLastNotice": false
   }
   ```
   Ответ: `200 OK` → `AttendanceRecordDto`

3. Обновление UI: отобразить обновлённые данные в строке таблицы.

---

### 5.6 Выставление оценки за урок студенту

**Последовательность:**

1. Пользователь вводит оценку в поле "Class Grade" для студента в таблице "Class Work & Attendance".
2. При сохранении отправляется запрос:
   ```
   POST /api/grades/entries
   Body: {
     "studentId": "220e8400-e29b-41d4-a716-446655440012",
     "offeringId": "660e8400-e29b-41d4-a716-446655440001",
     "points": 8.0,
     "typeCode": "SEMINAR",
     "typeLabel": null,
     "description": "Class participation",
     "lessonSessionId": "550e8400-e29b-41d4-a716-446655440000",
     "homeworkSubmissionId": null,
     "gradedAt": null
   }
   ```
   Ответ: `201 Created` → `GradeEntryDto`

3. Обновление UI: отобразить оценку в строке таблицы.

**При обновлении существующей оценки:**

1. Найти существующую оценку по `studentId`, `offeringId`, `lessonSessionId` = `lessonId`, `typeCode` = `SEMINAR`.
2. Отправить запрос:
   ```
   PUT /api/grades/entries/{gradeEntryId}
   Body: {
     "points": 9.0,
     "typeCode": "SEMINAR",
     "description": "Class participation"
   }
   ```
   Ответ: `200 OK` → `GradeEntryDto`

---

### 5.7 Скачивание файла материала

**Последовательность:**

1. Пользователь нажимает кнопку скачивания (↓) у файла материала.
2. Запрос:
   ```
   GET /api/documents/stored/{fileId}/download
   ```
   Ответ: `200 OK` — бинарный поток файла с заголовками `Content-Type` и `Content-Disposition`.

3. Браузер автоматически скачивает файл с оригинальным именем.

**Альтернатива (presigned URL):**

1. Запрос:
   ```
   GET /api/documents/stored/{fileId}/download-url?expires=3600
   ```
   Ответ: `{ "url": "https://storage.example.com/file?signature=..." }`

2. Редирект браузера на полученный URL для прямого скачивания из хранилища.

---

## 6. Сводная таблица эндпоинтов

| Метод | Путь | Назначение | Права доступа |
|-------|------|------------|---------------|
| GET | `/api/schedule/lessons/{id}` | Получить данные урока (базовая информация) | Аутентифицированный пользователь |
| GET | `/api/schedule/rooms/{id}` | Получить информацию об аудитории | Аутентифицированный пользователь |
| GET | `/api/offerings/{id}` | Получить информацию об offering | Аутентифицированный пользователь |
| GET | `/api/offerings/{offeringId}/teachers` | Получить список преподавателей offering | Аутентифицированный пользователь |
| GET | `/api/lessons/{lessonId}/materials` | Получить список материалов | Аутентифицированный пользователь |
| POST | `/api/lessons/{lessonId}/materials` | Создать материал | Преподаватель урока или админ |
| DELETE | `/api/lessons/{lessonId}/materials/{materialId}` | Удалить материал | Автор материала или админ |
| DELETE | `/api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}` | Удалить файл из материала | Автор материала или админ |
| POST | `/api/documents/upload` | Загрузить файл | Аутентифицированный пользователь |
| GET | `/api/documents/stored/{id}/download` | Скачать файл (поток) | Владелец файла или админ |
| GET | `/api/lessons/{lessonId}/homework` | Получить список домашних заданий | Аутентифицированный пользователь |
| POST | `/api/lessons/{lessonId}/homework` | Создать домашнее задание | Преподаватель урока или админ |
| GET | `/api/homework/{homeworkId}` | Получить домашнее задание по ID | Аутентифицированный пользователь |
| PUT | `/api/homework/{homeworkId}` | Обновить домашнее задание | Преподаватель урока или админ |
| DELETE | `/api/homework/{homeworkId}` | Удалить домашнее задание | Преподаватель урока или админ |
| GET | `/api/attendance/sessions/{sessionId}` | Получить данные посещаемости для урока | Преподаватель урока или админ |
| PUT | `/api/attendance/sessions/{sessionId}/students/{studentId}` | Обновить посещаемость студента | Преподаватель урока или админ |
| POST | `/api/attendance/sessions/{sessionId}/records/bulk` | Батч-обновление посещаемости | Преподаватель урока или админ |
| POST | `/api/grades/entries` | Создать оценку за урок | Преподаватель или админ |
| PUT | `/api/grades/entries/{id}` | Обновить оценку за урок | Преподаватель или админ |
| DELETE | `/api/grades/entries/{id}` | Удалить оценку за урок | Преподаватель или админ |
| GET | `/api/grades/groups/{groupId}/offerings/{offeringId}/summary` | Получить оценки группы по offering | Преподаватель группы или админ |

---

## 7. GAP Analysis & Recommendations

### 7.1 GAP-лист: недостающие данные для UI

1. **Lesson Header — расширенная информация:**
   - **UI needs:** Полная информация об уроке одним запросом: subject (название, код), group (название), teacher (имя), room (номер, здание), permissions (можно ли редактировать материалы/homework/attendance/grades).
   - **Currently API provides:** Базовую информацию об уроке (`GET /api/schedule/lessons/{id}`), но требуется 5-7 дополнительных запросов для получения полной информации.
   - **Propose:** Создать эндпоинт `GET /api/schedule/lessons/{id}/details`, который возвращает агрегированные данные одним запросом.

2. **Homework — одно задание вместо списка:**
   - **UI needs:** Одно домашнее задание для урока или `null` (если нет).
   - **Currently API provides:** Список домашних заданий (`GET /api/lessons/{lessonId}/homework`), фронтенд должен брать первый элемент.
   - **Propose:** Создать эндпоинт `GET /api/lessons/{lessonId}/homework/current`, который возвращает одно домашнее задание или `null`.

3. **Class Work — объединённые данные посещаемости и оценок:**
   - **UI needs:** Таблица студентов с посещаемостью, оценкой за урок (class grade) и комментарием преподавателя одним запросом.
   - **Currently API provides:** Отдельно посещаемость (`GET /api/attendance/sessions/{sessionId}`) и оценки (`GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary`), фронтенд должен объединять данные на клиенте и фильтровать оценки по `lessonSessionId`.
   - **Propose:** Создать эндпоинт `GET /api/lessons/{lessonId}/classwork`, который возвращает объединённые данные посещаемости и оценок за урок.

4. **Class Work — фильтрация оценок по уроку:**
   - **UI needs:** Получить оценки за конкретный урок (`lessonSessionId` = `lessonId`) и тип `SEMINAR`.
   - **Currently API provides:** Все оценки группы по offering, фронтенд должен фильтровать на клиенте.
   - **Propose:** Добавить параметр `lessonSessionId` в query эндпоинта `GET /api/grades/groups/{groupId}/offerings/{offeringId}/summary` для фильтрации оценок по уроку.

5. **Permissions — определение прав доступа:**
   - **UI needs:** Информация о том, может ли текущий пользователь редактировать материалы, домашнее задание, ставить отметки посещаемости, выставлять оценки.
   - **Currently API provides:** Нет явного эндпоинта для получения permissions. Права определяются на основе роли пользователя и проверяются на бэкенде при попытке редактирования.
   - **Propose:** Добавить поле `permissions` в ответ `GET /api/schedule/lessons/{id}/details` (см. пункт 1).

---

## 8. Примечания и допущения

### 8.1 ASSUMPTIONS

1. **Lesson Session = Lesson:** В модуле attendance используется термин "session", где `sessionId` = `lessonId`. Урок и есть сессия посещаемости.

2. **Homework:** Предполагается, что на урок может быть одно домашнее задание (или ни одного). В текущей реализации API возвращает список; фронтенд должен взять первый элемент или показать пустое состояние.

3. **Class Grade:** Оценка за работу на уроке имеет тип `SEMINAR` (или другой тип, определяемый бизнес-правилами) и привязана к уроку через `lessonSessionId` = `lessonId`.

4. **Статусы посещаемости:** `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`. Если посещаемость не отмечена, статус представлен как `null` в `status`.

5. **Объединение данных Class Work:** Фронтенд должен объединять данные посещаемости и оценок на клиенте для отображения полной таблицы "Class Work & Attendance".

### 8.2 Адаптация контракта

Если какие-то предположения неверны, контракт легко адаптируется:
- Изменить структуру DTO (добавить/удалить поля).
- Изменить коды ошибок или HTTP-статусы.
- Добавить/удалить эндпоинты.

---

**Версия документа:** 1.0  
**Дата:** 2025-02-19  
**Автор:** Enterprise Architect Agent
