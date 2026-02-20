# Контракт API: домашние задания

Документ-контракт для фронтенда по эндпоинтам модуля **Document**, связанным с сущностью **домашнего задания** (homework). Описаны все эндпоинты для получения, создания, обновления и удаления домашних заданий, структуры запросов и ответов, а также возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/lessons` | Уроки: получение списка домашних заданий для урока, создание домашнего задания |
| `/api/homework` | Домашние задания: получение, обновление, удаление по ID |

Хост и порт задаются окружением развёртывания.

### 1.2 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- Эндпоинты, которые **изменяют данные** (POST, PUT, DELETE), доступны только пользователям с одной из ролей: **TEACHER**, **ADMIN**, **MODERATOR**, **SUPER_ADMIN**.
- Эндпоинты только для чтения (GET) доступны **любой аутентифицированной роли** (JWT обязателен).
- У каждого эндпоинта в описании указано поле **Роли:** — кто может вызывать ручку. При отсутствии прав возвращается `403 Forbidden` (код `FORBIDDEN`), при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID.
- **Даты и время** — строки **ISO-8601**: дата-время `"2025-02-05T12:00:00"`.
- **Числа** — JSON number; для целых (points, size) — integer; для длинных (size файла) — long.
- **null** — явное отсутствие значения.
- **Optional** — в JSON представлен как объект или `null` (если отсутствует).

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `BAD_REQUEST`, `NOT_FOUND`, `FORBIDDEN`, `HOMEWORK_NOT_FOUND`, `HOMEWORK_LESSON_NOT_FOUND`, `HOMEWORK_FILE_NOT_FOUND`, `HOMEWORK_PERMISSION_DENIED`, `HOMEWORK_VALIDATION_FAILED`, `HOMEWORK_SAVE_FAILED`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для валидации — объект «имя поля → сообщение» |

Пример ответа с ошибкой (404):

```json
{
  "code": "HOMEWORK_NOT_FOUND",
  "message": "Homework not found: 550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных

### 2.1 HomeworkDto

Основная модель домашнего задания. Домашнее задание связано с уроком через junction table. Файл опционален; при удалении ссылки на файл сам файл в хранилище не удаляется.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор домашнего задания |
| `lessonId` | UUID | Id урока, к которому привязано задание |
| `title` | string | Название задания (обязательно, максимум 500 символов) |
| `description` | string \| null | Описание задания (опционально, максимум 5000 символов) |
| `points` | integer \| null | Максимальное количество баллов (опционально, неотрицательное) |
| `file` | object (**StoredFileDto**) \| null | Метаданные прикреплённого файла (если есть) |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время обновления (ISO-8601) |

### 2.2 StoredFileDto

Метаданные хранимого файла (размер, тип контента, информация о загрузке). Используется в поле `file` объекта **HomeworkDto**.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор файла |
| `size` | integer (long) | Размер файла в байтах |
| `contentType` | string | MIME-тип файла (например, `"application/pdf"`, `"image/png"`) |
| `originalName` | string | Оригинальное имя файла при загрузке |
| `uploadedAt` | string | Дата и время загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

---

## 3. Получение домашних заданий

### 3.1 GET /api/lessons/{lessonId}/homework

**Назначение:** получить список всех домашних заданий для указанного урока. Удобно для отображения списка заданий на странице урока.

**Метод и путь:** `GET /api/lessons/{lessonId}/homework`

**Роли:** любая аутентифицированная роль (JWT обязателен).

**Параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `lessonId` | UUID | да (path) | Id урока |

**Успешный ответ:** `200 OK` — массив объектов **HomeworkDto**. Упорядочены по дате создания (от новых к старым, `createdAt DESC`). Если у урока нет домашних заданий, возвращается пустой массив `[]`.

**Ошибки:**  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `404 Not Found` — урок не найден (код `HOMEWORK_LESSON_NOT_FOUND`, сообщение вида `"Lesson not found: <lessonId>"`).

---

### 3.2 GET /api/homework/{homeworkId}

**Назначение:** получить домашнее задание по идентификатору. Удобно для формы редактирования задания или просмотра деталей.

**Метод и путь:** `GET /api/homework/{homeworkId}`

**Роли:** любая аутентифицированная роль (JWT обязателен).

**Параметр пути:** `homeworkId` — UUID домашнего задания.

**Успешный ответ:** `200 OK` — объект **HomeworkDto**.

**Ошибки:**  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `404 Not Found` — домашнее задание не найдено (код `HOMEWORK_NOT_FOUND`, сообщение вида `"Homework not found: <homeworkId>"`).

---

## 4. Создание домашнего задания

### 4.1 POST /api/lessons/{lessonId}/homework

**Назначение:** создать домашнее задание для указанного урока. Можно прикрепить файл (если файл уже загружен в систему через DocumentApi). Файл опционален.

**Метод и путь:** `POST /api/lessons/{lessonId}/homework`

**Роли:** TEACHER, ADMIN, MODERATOR, SUPER_ADMIN.

**Параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `lessonId` | UUID | да (path) | Id урока, к которому привязывается задание |

**Тело запроса:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | обязательно | Название задания (не пустое, максимум 500 символов) |
| `description` | string | нет | Описание задания (максимум 5000 символов) |
| `points` | integer | нет | Максимальное количество баллов (неотрицательное) |
| `storedFileId` | UUID | нет | Id файла в хранилище (если файл уже загружен через DocumentApi) |

**Успешный ответ:** `201 Created` — объект **HomeworkDto** (созданное задание).

**Ошибки:**  
- `400 Bad Request` — не указано обязательное поле `title`; `title` пустое или превышает 500 символов; `description` превышает 5000 символов; `points` отрицательное (код `BAD_REQUEST` или `HOMEWORK_VALIDATION_FAILED`). При валидации через `@Valid` в `details` приходит объект «поле → сообщение» (код `VALIDATION_FAILED`).  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `403 Forbidden` — у пользователя нет роли TEACHER, ADMIN, MODERATOR или SUPER_ADMIN (код `FORBIDDEN` или `HOMEWORK_PERMISSION_DENIED`).  
- `404 Not Found` — урок не найден (код `HOMEWORK_LESSON_NOT_FOUND`) или файл не найден, если указан `storedFileId` (код `HOMEWORK_FILE_NOT_FOUND`).  
- `422 Unprocessable Entity` — ошибка сохранения в БД (например, нарушение ограничений целостности, код `HOMEWORK_SAVE_FAILED`).

---

## 5. Обновление домашнего задания

### 5.1 PUT /api/homework/{homeworkId}

**Назначение:** обновить домашнее задание: изменить название, описание, баллы или прикреплённый файл. Можно обновить только часть полей (остальные остаются без изменений). Для удаления ссылки на файл используйте `clearFile: true` (сам файл в хранилище не удаляется).

**Метод и путь:** `PUT /api/homework/{homeworkId}`

**Роли:** TEACHER, ADMIN, MODERATOR, SUPER_ADMIN.

**Параметр пути:** `homeworkId` — UUID домашнего задания.

**Тело запроса:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | нет | Новое название задания (максимум 500 символов). Если `null` или не указано, название не изменяется. |
| `description` | string \| null | нет | Новое описание (максимум 5000 символов) или `null` (чтобы очистить). Если не указано, описание не изменяется. |
| `points` | integer \| null | нет | Новое количество баллов (неотрицательное) или `null` (чтобы очистить). Если не указано, баллы не изменяются. |
| `clearFile` | boolean | нет | Если `true`, удаляет ссылку на файл (файл в хранилище не удаляется). Игнорируется, если указан `storedFileId`. |
| `storedFileId` | UUID | нет | Id нового файла в хранилище (используется только если `clearFile` не `true`). Если `null` или не указано и `clearFile` не `true`, файл не изменяется. |

**Успешный ответ:** `200 OK` — объект **HomeworkDto** (обновлённое задание).

**Ошибки:**  
- `400 Bad Request` — `title` превышает 500 символов; `description` превышает 5000 символов; `points` отрицательное (код `BAD_REQUEST` или `HOMEWORK_VALIDATION_FAILED`).  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `403 Forbidden` — у пользователя нет роли TEACHER, ADMIN, MODERATOR или SUPER_ADMIN (код `FORBIDDEN` или `HOMEWORK_PERMISSION_DENIED`).  
- `404 Not Found` — домашнее задание не найдено (код `HOMEWORK_NOT_FOUND`) или файл не найден, если указан `storedFileId` (код `HOMEWORK_FILE_NOT_FOUND`).  
- `422 Unprocessable Entity` — ошибка сохранения в БД (код `HOMEWORK_SAVE_FAILED`).

---

## 6. Удаление домашнего задания

### 6.1 DELETE /api/homework/{homeworkId}

**Назначение:** удалить домашнее задание. Удаляется только запись задания; прикреплённый файл (если есть) в хранилище не удаляется.

**Метод и путь:** `DELETE /api/homework/{homeworkId}`

**Роли:** TEACHER, ADMIN, MODERATOR, SUPER_ADMIN.

**Параметр пути:** `homeworkId` — UUID домашнего задания.

**Успешный ответ:** `204 No Content` (тела ответа нет).

**Ошибки:**  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `403 Forbidden` — у пользователя нет роли TEACHER, ADMIN, MODERATOR или SUPER_ADMIN (код `FORBIDDEN` или `HOMEWORK_PERMISSION_DENIED`).  
- `404 Not Found` — домашнее задание не найдено (код `HOMEWORK_NOT_FOUND`).

---

## 7. Сводная таблица ошибок

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Не указаны обязательные поля; неверный формат данных |
| 400 | VALIDATION_FAILED | Ошибка валидации запроса через Bean Validation (в `details` — объект «поле → сообщение») |
| 400 | HOMEWORK_VALIDATION_FAILED | Ошибка валидации бизнес-правил (пустой title, превышение длины, отрицательные points) |
| 401 | UNAUTHORIZED | Не передан или невалидный JWT |
| 403 | FORBIDDEN | Нет роли TEACHER, ADMIN, MODERATOR или SUPER_ADMIN для операции изменения |
| 403 | HOMEWORK_PERMISSION_DENIED | Пользователь не имеет прав на управление домашними заданиями |
| 404 | NOT_FOUND | Общий «не найдено» |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден (при создании или получении списка) |
| 404 | HOMEWORK_FILE_NOT_FOUND | Файл в хранилище не найден (при прикреплении к заданию) |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения в БД (нарушение ограничений целостности) |

---

## 8. Примеры запросов и ответов

### Получение списка домашних заданий для урока

**Запрос:**  
`GET /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK`

```json
[
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "lessonId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "Решить задачи по алгоритмам",
    "description": "Выполнить задания из главы 5: сортировка, поиск, графы",
    "points": 10,
    "file": {
      "id": "bb0e8400-e29b-41d4-a716-446655440020",
      "size": 245760,
      "contentType": "application/pdf",
      "originalName": "homework_tasks.pdf",
      "uploadedAt": "2025-02-05T10:00:00",
      "uploadedBy": "cc0e8400-e29b-41d4-a716-446655440030"
    },
    "createdAt": "2025-02-05T10:05:00",
    "updatedAt": "2025-02-05T10:05:00"
  },
  {
    "id": "dd0e8400-e29b-41d4-a716-446655440040",
    "lessonId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "Подготовить презентацию",
    "description": null,
    "points": 5,
    "file": null,
    "createdAt": "2025-02-03T14:20:00",
    "updatedAt": "2025-02-03T14:20:00"
  }
]
```

---

### Получение домашнего задания по ID

**Запрос:**  
`GET /api/homework/aa0e8400-e29b-41d4-a716-446655440010`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK`

```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440010",
  "lessonId": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Решить задачи по алгоритмам",
  "description": "Выполнить задания из главы 5: сортировка, поиск, графы",
  "points": 10,
  "file": {
    "id": "bb0e8400-e29b-41d4-a716-446655440020",
    "size": 245760,
    "contentType": "application/pdf",
    "originalName": "homework_tasks.pdf",
    "uploadedAt": "2025-02-05T10:00:00",
    "uploadedBy": "cc0e8400-e29b-41d4-a716-446655440030"
  },
  "createdAt": "2025-02-05T10:05:00",
  "updatedAt": "2025-02-05T10:05:00"
}
```

---

### Создание домашнего задания

**Запрос:**  
`POST /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "title": "Решить задачи по алгоритмам",
  "description": "Выполнить задания из главы 5: сортировка, поиск, графы",
  "points": 10,
  "storedFileId": "bb0e8400-e29b-41d4-a716-446655440020"
}
```

**Ответ:** `201 Created`

```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440010",
  "lessonId": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Решить задачи по алгоритмам",
  "description": "Выполнить задания из главы 5: сортировка, поиск, графы",
  "points": 10,
  "file": {
    "id": "bb0e8400-e29b-41d4-a716-446655440020",
    "size": 245760,
    "contentType": "application/pdf",
    "originalName": "homework_tasks.pdf",
    "uploadedAt": "2025-02-05T10:00:00",
    "uploadedBy": "cc0e8400-e29b-41d4-a716-446655440030"
  },
  "createdAt": "2025-02-05T10:05:00",
  "updatedAt": "2025-02-05T10:05:00"
}
```

---

### Создание домашнего задания без файла

**Запрос:**  
`POST /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "title": "Подготовить презентацию",
  "description": "Тема: Современные технологии программирования",
  "points": 5
}
```

**Ответ:** `201 Created`

```json
{
  "id": "dd0e8400-e29b-41d4-a716-446655440040",
  "lessonId": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Подготовить презентацию",
  "description": "Тема: Современные технологии программирования",
  "points": 5,
  "file": null,
  "createdAt": "2025-02-05T11:00:00",
  "updatedAt": "2025-02-05T11:00:00"
}
```

---

### Обновление домашнего задания

**Запрос:**  
`PUT /api/homework/aa0e8400-e29b-41d4-a716-446655440010`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "title": "Решить задачи по алгоритмам (обновлено)",
  "points": 15,
  "clearFile": false,
  "storedFileId": "ee0e8400-e29b-41d4-a716-446655440050"
}
```

**Ответ:** `200 OK` — объект **HomeworkDto** с обновлёнными полями.

---

### Удаление ссылки на файл

**Запрос:**  
`PUT /api/homework/aa0e8400-e29b-41d4-a716-446655440010`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "clearFile": true
}
```

**Ответ:** `200 OK` — объект **HomeworkDto** с `file: null` (файл в хранилище не удалён).

---

### Удаление домашнего задания

**Запрос:**  
`DELETE /api/homework/aa0e8400-e29b-41d4-a716-446655440010`  
`Authorization: Bearer <JWT>`

**Ответ:** `204 No Content` (тела ответа нет).

---

### Ошибка: домашнее задание не найдено

**Запрос:** `GET /api/homework/00000000-0000-0000-0000-000000000000`

**Ответ:** `404 Not Found`

```json
{
  "code": "HOMEWORK_NOT_FOUND",
  "message": "Homework not found: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка: урок не найден

**Запрос:** `GET /api/lessons/00000000-0000-0000-0000-000000000000/homework`

**Ответ:** `404 Not Found`

```json
{
  "code": "HOMEWORK_LESSON_NOT_FOUND",
  "message": "Lesson not found: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка: нет прав доступа

**Запрос:** `POST /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework` от пользователя без роли TEACHER/ADMIN/MODERATOR/SUPER_ADMIN.

**Ответ:** `403 Forbidden`

```json
{
  "code": "HOMEWORK_PERMISSION_DENIED",
  "message": "You don't have permission to manage homework",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка валидации

**Запрос:** `POST /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework` с телом без обязательного поля `title`.

**Ответ:** `400 Bad Request`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": {
    "title": "title is required"
  }
}
```

---

### Ошибка: файл не найден

**Запрос:** `POST /api/lessons/550e8400-e29b-41d4-a716-446655440001/homework` с несуществующим `storedFileId`.

**Ответ:** `404 Not Found`

```json
{
  "code": "HOMEWORK_FILE_NOT_FOUND",
  "message": "File not found: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```
