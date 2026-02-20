# Контракт API: материалы к уроку

Документ-контракт для фронтенда. Описывает эндпоинты модуля Document для работы с материалами к уроку (lesson materials). Материал урока — это бизнес-сущность, связывающая урок с хранимыми файлами: один урок может иметь множество материалов, один материал может содержать множество файлов. В контракте приведены структуры запросов и ответов, а также возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/lessons` | Эндпоинты для работы с материалами урока |

Хост и порт задаются окружением развёртывания. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы к эндпоинтам модуля выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе) или с учётом cookie-механизма, принятого в проекте.
- **Чтение** (GET): доступ имеют **все аутентифицированные пользователи**.
- **Создание** (POST): требуются роли **TEACHER**, **ADMIN**, **MODERATOR** или **SUPER_ADMIN**.
- **Удаление и изменение** (DELETE, POST для добавления файлов): требуются права автора материала или роли **ADMIN**, **MODERATOR**, **SUPER_ADMIN**.
- При отсутствии JWT или невалидном токене возвращается `401 Unauthorized`.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки в формате **ISO-8601**: дата-время вида `"2025-02-05T12:00:00"`.
- **Числа** — JSON number; для целых полей — integer; для размера файла — integer (байты).
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.
- **Массивы** — всегда массив (например, `[]` при отсутствии элементов).

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `LESSON_MATERIAL_NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для ошибок валидации — объект «имя поля → сообщение»; иначе может отсутствовать или быть `null` |

Пример ответа с ошибкой «не найдено» (404):

```json
{
  "code": "LESSON_MATERIAL_NOT_FOUND",
  "message": "Lesson material not found: 550e8400-e29b-41d4-a716-446655440000",
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

Пример ответа с ошибкой валидации (400):

```json
{
  "code": "LESSON_MATERIAL_INVALID_NAME",
  "message": "name is required",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": {
    "name": "name is required"
  }
}
```

---

## 2. Модели данных (переиспользуемые DTO)

### 2.1 StoredFileDto (файл в хранилище)

Метаданные файла, загруженного в хранилище. Используется в поле `files` объекта **LessonMaterialDto**.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла в хранилище |
| `size` | integer | Размер файла в байтах |
| `contentType` | string \| null | MIME-тип файла (например, `"application/pdf"`, `"image/png"`) |
| `originalName` | string \| null | Оригинальное имя файла при загрузке |
| `uploadedAt` | string | Дата и время загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 2.2 LessonMaterialDto (материал урока)

Основная модель данных материала урока. Содержит метаданные материала и упорядоченный список файлов.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id материала |
| `lessonId` | UUID | Id урока, к которому привязан материал |
| `name` | string | Название материала (обязательное, максимум 500 символов) |
| `description` | string \| null | Описание материала (опционально, максимум 5000 символов) |
| `authorId` | UUID | Id пользователя-автора материала |
| `publishedAt` | string | Дата и время публикации (ISO-8601) |
| `files` | array of **StoredFileDto** | Упорядоченный список файлов материала (может быть пустым массивом) |

---

## 3. Эндпоинты

### 3.1 Список материалов урока

**Назначение:** получить список всех материалов для конкретного урока. Материалы возвращаются отсортированными по дате публикации в порядке убывания (самые новые первыми). Используется для отображения списка материалов на странице урока.

**Метод и путь:** `GET /api/lessons/{lessonId}/materials`

**Права доступа:** любой аутентифицированный пользователь. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Query-параметры:** нет.

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK`

Тело ответа — массив объектов **LessonMaterialDto** (см. п. 2.2). При отсутствии материалов возвращается пустой массив `[]`.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |

---

### 3.2 Создание материала урока

**Назначение:** создать новый материал для урока. Материал может быть создан с пустым списком файлов или с начальным набором файлов (файлы должны быть предварительно загружены через `POST /api/documents/upload`). Используется для добавления нового материала на странице урока.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials`

**Права доступа:** пользователи с ролями **TEACHER**, **ADMIN**, **MODERATOR** или **SUPER_ADMIN**. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока, для которого создаётся материал |

**Query-параметры:** нет.

**Тело запроса:**

Объект **CreateLessonMaterialRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `name` | string | обязательное | Название материала (не пустое, максимум 500 символов) |
| `description` | string \| null | опциональное | Описание материала (максимум 5000 символов) |
| `publishedAt` | string (ISO-8601) | обязательное | Дата и время публикации материала |
| `storedFileIds` | array of UUID | опциональное | Список Id файлов из хранилища для прикрепления к материалу (порядок сохраняется). Если не указано или `null`, материал создаётся без файлов |

**Успешный ответ:** `201 Created`

Тело ответа — объект **LessonMaterialDto** (см. п. 2.2) с данными созданного материала.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 400 | LESSON_MATERIAL_INVALID_NAME | Название не указано или превышает 500 символов | `"name is required"` или `"name must not exceed 500 characters"` |
| 400 | VALIDATION_FAILED | Ошибка валидации запроса (например, `publishedAt` не указан) | Сообщение в `details` с указанием поля |
| 400 | LESSON_MATERIAL_INVALID_NAME | В списке `storedFileIds` есть дубликаты | `"Duplicate file IDs in request"` |
| 403 | LESSON_MATERIAL_CREATE_PERMISSION_DENIED | У пользователя нет прав на создание материалов (нет роли TEACHER/ADMIN/MODERATOR/SUPER_ADMIN) | `"Only teachers and administrators can create lesson materials"` |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из указанных файлов не найден в хранилище | `"Stored file not found: <storedFileId>"` |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения в БД (например, нарушение ограничений целостности) | `"Failed to save lesson material. Please try again."` |

---

### 3.3 Получение материала урока

**Назначение:** получить информацию о конкретном материале урока по его Id. Используется для отображения детальной информации о материале или для редактирования.

**Метод и путь:** `GET /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** любой аутентифицированный пользователь. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (используется для консистентности URL, но проверяется только `materialId`) |
| `materialId` | UUID | Id материала |

**Query-параметры:** нет.

**Тело запроса:** не используется (GET).

**Успешный ответ:** `200 OK`

Тело ответа — объект **LessonMaterialDto** (см. п. 2.2).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | `"Lesson material not found: <materialId>"` |

---

### 3.4 Удаление материала урока

**Назначение:** удалить материал урока и все связи с файлами. Файлы из хранилища удаляются только если они не используются в других материалах, домашних заданиях или других сущностях системы. Используется для удаления материала со страницы урока.

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** автор материала или пользователи с ролями **ADMIN**, **MODERATOR**, **SUPER_ADMIN**. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (используется для консистентности URL) |
| `materialId` | UUID | Id материала для удаления |

**Query-параметры:** нет.

**Тело запроса:** не используется (DELETE).

**Успешный ответ:** `204 No Content`

Тело ответа отсутствует.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | У пользователя нет прав на удаление материала (не автор и нет роли ADMIN/MODERATOR/SUPER_ADMIN) | `"You don't have permission to modify this lesson material"` |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | `"Lesson material not found: <materialId>"` |

---

### 3.5 Добавление файлов к материалу

**Назначение:** добавить файлы к существующему материалу урока. Новые файлы добавляются в конец списка с сохранением порядка. Файлы должны быть предварительно загружены через `POST /api/documents/upload`. Используется для дополнения материала новыми файлами без пересоздания.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials/{materialId}/files`

**Права доступа:** автор материала или пользователи с ролями **ADMIN**, **MODERATOR**, **SUPER_ADMIN**. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (используется для консистентности URL) |
| `materialId` | UUID | Id материала, к которому добавляются файлы |

**Query-параметры:** нет.

**Тело запроса:**

Объект **AddLessonMaterialFilesRequest**:

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `storedFileIds` | array of UUID | обязательное | Список Id файлов из хранилища для добавления к материалу (порядок сохраняется, дубликаты игнорируются) |

**Успешный ответ:** `204 No Content`

Тело ответа отсутствует.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 400 | VALIDATION_FAILED | Поле `storedFileIds` не указано или `null` | Сообщение в `details` с указанием поля |
| 400 | LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL | Один из указанных файлов уже прикреплён к этому материалу | `"File already attached to this material: <storedFileId>"` |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | У пользователя нет прав на изменение материала (не автор и нет роли ADMIN/MODERATOR/SUPER_ADMIN) | `"You don't have permission to modify this lesson material"` |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | `"Lesson material not found: <materialId>"` |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из указанных файлов не найден в хранилище | `"Stored file not found: <storedFileId>"` |

**Примечание:** если `storedFileIds` пустой массив или содержит только дубликаты уже прикреплённых файлов, запрос завершается успешно без изменений (`204 No Content`).

---

### 3.6 Удаление файла из материала

**Назначение:** удалить файл из материала урока. Связь между материалом и файлом удаляется. Файл из хранилища удаляется только если он не используется в других материалах, домашних заданиях или других сущностях системы. Используется для удаления конкретного файла из материала.

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}`

**Права доступа:** автор материала или пользователи с ролями **ADMIN**, **MODERATOR**, **SUPER_ADMIN**. Требуется валидный JWT.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (используется для консистентности URL) |
| `materialId` | UUID | Id материала |
| `storedFileId` | UUID | Id файла в хранилище для удаления из материала |

**Query-параметры:** нет.

**Тело запроса:** не используется (DELETE).

**Успешный ответ:** `204 No Content`

Тело ответа отсутствует.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | `"Authentication required"` |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | У пользователя нет прав на изменение материала (не автор и нет роли ADMIN/MODERATOR/SUPER_ADMIN) | `"You don't have permission to modify this lesson material"` |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | `"Lesson material not found: <materialId>"` |
| 404 | LESSON_MATERIAL_FILE_LINK_NOT_FOUND | Файл не прикреплён к этому материалу | `"File is not attached to this material: <materialId>, file: <storedFileId>"` |

---

## 4. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Требуется аутентификация (нет или невалидный JWT) |
| 400 | LESSON_MATERIAL_INVALID_NAME | Название материала не указано, превышает 500 символов или в списке файлов есть дубликаты |
| 400 | VALIDATION_FAILED | Ошибка валидации запроса (обязательные поля не указаны, неверный формат) |
| 403 | LESSON_MATERIAL_CREATE_PERMISSION_DENIED | Нет прав на создание материалов (нет роли TEACHER/ADMIN/MODERATOR/SUPER_ADMIN) |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | Нет прав на изменение/удаление материала (не автор и нет роли ADMIN/MODERATOR/SUPER_ADMIN) |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал урока не найден |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Файл в хранилище не найден |
| 404 | LESSON_MATERIAL_FILE_LINK_NOT_FOUND | Файл не прикреплён к материалу |
| 400 | LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL | Файл уже прикреплён к материалу |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения в БД |

---

## 5. Примеры запросов и ответов

### 5.1 Успешный запрос списка материалов урока

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
[
  {
    "id": "bb0e8400-e29b-41d4-a716-446655440006",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Lecture Slides - Introduction",
    "description": "Slides for the first lecture on algorithms",
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
  },
  {
    "id": "dd0e8400-e29b-41d4-a716-446655440008",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Practice Exercises",
    "description": null,
    "authorId": "22222222-3333-4444-5555-666666666666",
    "publishedAt": "2025-02-17T14:00:00",
    "files": []
  }
]
```

### 5.2 Успешное создание материала урока

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Lecture Notes - Chapter 1",
  "description": "Detailed notes for Chapter 1",
  "publishedAt": "2025-02-20T09:00:00",
  "storedFileIds": [
    "cc0e8400-e29b-41d4-a716-446655440007",
    "ee0e8400-e29b-41d4-a716-446655440009"
  ]
}
```

**Ответ:** `201 Created`

```json
{
  "id": "ff0e8400-e29b-41d4-a716-446655440010",
  "lessonId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Lecture Notes - Chapter 1",
  "description": "Detailed notes for Chapter 1",
  "authorId": "22222222-3333-4444-5555-666666666666",
  "publishedAt": "2025-02-20T09:00:00",
  "files": [
    {
      "id": "cc0e8400-e29b-41d4-a716-446655440007",
      "size": 2411520,
      "contentType": "application/pdf",
      "originalName": "Lecture Slides.pdf",
      "uploadedAt": "2025-02-18T10:00:00",
      "uploadedBy": "22222222-3333-4444-5555-666666666666"
    },
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440009",
      "size": 1536000,
      "contentType": "application/pdf",
      "originalName": "Chapter1.pdf",
      "uploadedAt": "2025-02-19T11:00:00",
      "uploadedBy": "22222222-3333-4444-5555-666666666666"
    }
  ]
}
```

### 5.3 Успешное получение материала урока

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials/bb0e8400-e29b-41d4-a716-446655440006
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
{
  "id": "bb0e8400-e29b-41d4-a716-446655440006",
  "lessonId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Lecture Slides - Introduction",
  "description": "Slides for the first lecture on algorithms",
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
```

### 5.4 Успешное добавление файлов к материалу

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials/bb0e8400-e29b-41d4-a716-446655440006/files
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "storedFileIds": [
    "ff0e8400-e29b-41d4-a716-446655440011"
  ]
}
```

**Ответ:** `204 No Content`

(Тело ответа отсутствует)

### 5.5 Успешное удаление файла из материала

**Запрос:**

```
DELETE /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials/bb0e8400-e29b-41d4-a716-446655440006/files/cc0e8400-e29b-41d4-a716-446655440007
Authorization: Bearer <JWT>
```

**Ответ:** `204 No Content`

(Тело ответа отсутствует)

### 5.6 Ответ при отсутствии аутентификации

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
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

### 5.7 Ответ при отсутствии урока

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
```

**Ответ:** `404 Not Found`

```json
{
  "code": "LESSON_MATERIAL_LESSON_NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.8 Ответ при отсутствии материала

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials/bb0e8400-e29b-41d4-a716-446655440006
Authorization: Bearer <JWT>
```

**Ответ:** `404 Not Found`

```json
{
  "code": "LESSON_MATERIAL_NOT_FOUND",
  "message": "Lesson material not found: bb0e8400-e29b-41d4-a716-446655440006",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.9 Ответ при отсутствии прав на создание

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "New Material",
  "publishedAt": "2025-02-20T09:00:00"
}
```

**Ответ:** `403 Forbidden`

```json
{
  "code": "LESSON_MATERIAL_CREATE_PERMISSION_DENIED",
  "message": "Only teachers and administrators can create lesson materials",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.10 Ответ при ошибке валидации (не указано название)

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "description": "Some description",
  "publishedAt": "2025-02-20T09:00:00"
}
```

**Ответ:** `400 Bad Request`

```json
{
  "code": "LESSON_MATERIAL_INVALID_NAME",
  "message": "name is required",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": {
    "name": "name is required"
  }
}
```

### 5.11 Ответ при попытке добавить уже прикреплённый файл

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials/bb0e8400-e29b-41d4-a716-446655440006/files
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "storedFileIds": [
    "cc0e8400-e29b-41d4-a716-446655440007"
  ]
}
```

(Файл `cc0e8400-e29b-41d4-a716-446655440007` уже прикреплён к материалу)

**Ответ:** `400 Bad Request`

```json
{
  "code": "LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL",
  "message": "File already attached to this material: cc0e8400-e29b-41d4-a716-446655440007",
  "timestamp": "2025-02-19T12:00:00.123Z",
  "details": null
}
```

### 5.12 Ответ при пустом списке материалов

При успешном ответе `200 OK` поле `materials` может быть пустым массивом, если у урока нет материалов:

**Запрос:**

```
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
```

**Ответ:** `200 OK`

```json
[]
```

Фронтенд может использовать это для отображения пустого состояния блока «Материалы урока».

---

## 6. Примечания для фронтенда

### 6.1 Загрузка файлов

Перед созданием материала или добавлением файлов к существующему материалу файлы должны быть загружены через эндпоинт `POST /api/documents/upload`. После успешной загрузки фронтенд получает `StoredFileDto` с полем `id`, которое затем используется в `storedFileIds` при создании или изменении материала.

### 6.2 Порядок файлов

Файлы в материале упорядочены по полю `sort_order` (порядок сохраняется при создании и добавлении). Фронтенд должен отображать файлы в том порядке, в котором они приходят в массиве `files` объекта `LessonMaterialDto`.

### 6.3 Удаление файлов

При удалении файла из материала или удалении самого материала файлы из хранилища удаляются автоматически только если они не используются в других сущностях системы (других материалах, домашних заданиях и т.д.). Фронтенд не должен беспокоиться о явном удалении файлов из хранилища — это обрабатывается сервером.

### 6.4 Права доступа

- **Создание материалов:** только TEACHER, ADMIN, MODERATOR, SUPER_ADMIN.
- **Удаление/изменение материалов:** автор материала или ADMIN, MODERATOR, SUPER_ADMIN.
- **Просмотр материалов:** все аутентифицированные пользователи.

Фронтенд должен скрывать кнопки создания/редактирования/удаления для пользователей без соответствующих прав, но сервер также проверяет права и вернёт `403 Forbidden` при попытке несанкционированного доступа.
