# Контракт API: Lesson Details (Teacher view)

Документ-контракт для фронтенда страницы Lesson Details с точки зрения преподавателя. Описывает эндпоинты для получения полной информации об уроке (header страницы), управления материалами урока и домашними заданиями. Документ ориентирован на реализацию UI без углубления в технические детали хранения файлов и внутреннюю архитектуру.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/schedule` | Расписание: уроки, аудитории, здания |
| `/api/lessons` | Материалы урока и домашние задания |
| `/api/homework` | Операции с домашними заданиями по ID |
| `/api/documents` | Загрузка и скачивание файлов |

Хост и порт задаются окружением развёртывания. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы к описанным эндпоинтам выполняются с заголовком: **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- Эндпоинты для **создания и изменения** материалов и домашних заданий требуют роли **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.
- Эндпоинты только для чтения требуют аутентификации (валидный JWT).

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки в формате **ISO-8601**: дата-время вида `"2025-10-08T13:00:00"`, дата вида `"2025-10-08"`, время вида `"13:00:00"`.
- **Числа** — JSON number; для целых полей (баллы, размер файла) — целое число.
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`, `VALIDATION_FAILED`, `UNAUTHORIZED`, `FORBIDDEN`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для ошибок валидации — объект «имя поля → сообщение»; иначе может отсутствовать |

Пример ответа с ошибкой валидации (400):

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Ошибка проверки данных. Проверьте заполненные поля.",
  "timestamp": "2025-10-08T12:00:00.123Z",
  "details": {
    "title": "title is required",
    "points": "Points must not be negative"
  }
}
```

Пример ответа с ошибкой «не найдено» (404):

```json
{
  "code": "NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-10-08T12:00:00.123Z",
  "details": null
}
```

---

## 2. Lesson (полная информация об уроке)

Для отображения header страницы Lesson Details требуется полная информация об уроке: предмет, дата и время, аудитория, преподаватель, группа, статус урока. Также необходимо определить права текущего пользователя на управление материалами и редактирование домашнего задания.

### 2.1 GET /api/schedule/lessons/{id} — получить информацию об уроке

**Назначение:** получить базовую информацию об уроке (дата, время, аудитория, статус, тема).

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
| `id` | UUID | Уникальный идентификатор урока |
| `offeringId` | UUID | ID offering (предложения занятия) |
| `offeringSlotId` | UUID \| null | ID слота offering, если урок сгенерирован из слота |
| `date` | string (ISO-8601 date) | Дата урока (формат: `YYYY-MM-DD`) |
| `startTime` | string (HH:mm:ss) | Время начала (например, `"13:00:00"`) |
| `endTime` | string (HH:mm:ss) | Время окончания (например, `"14:30:00"`) |
| `timeslotId` | UUID \| null | ID шаблона времени (опционально) |
| `roomId` | UUID \| null | ID аудитории (если назначена) |
| `topic` | string \| null | Тема урока (опционально) |
| `status` | string \| null | Статус урока (`"PLANNED"`, `"CANCELLED"`, `"DONE"` или `null`) |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Урок не найден |

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
  "status": "PLANNED",
  "createdAt": "2025-10-01T10:00:00",
  "updatedAt": "2025-10-01T10:00:00"
}
```

**Примечание:** Для получения расширенной информации (предмет, группа, преподаватель) требуется дополнительный запрос к эндпоинту offering (см. раздел 2.2).

---

### 2.2 GET /api/schedule/rooms/{id} — получить информацию об аудитории

**Назначение:** получить информацию об аудитории для отображения в header урока (если `roomId` в LessonDto не `null`).

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
| `capacity` | integer \| null | Вместимость аудитории |
| `type` | string \| null | Тип аудитории |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 404 | SCHEDULE_ROOM_NOT_FOUND | Аудитория не найдена |

**Пример ответа:**

```json
{
  "id": "990e8400-e29b-41d4-a716-446655440004",
  "buildingId": "aa0e8400-e29b-41d4-a716-446655440005",
  "buildingName": "Main Building",
  "number": "208",
  "capacity": 30,
  "type": "LAB",
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:00"
}
```

---

### 2.3 Определение прав доступа

Права доступа на управление материалами и редактирование домашнего задания определяются на основе роли текущего пользователя:

- **Управление материалами:** пользователь должен иметь роль **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**. При попытке создать материал без необходимых прав возвращается ошибка `403 FORBIDDEN` с кодом `LESSON_MATERIAL_CREATE_PERMISSION_DENIED`.
- **Редактирование домашнего задания:** пользователь должен иметь роль **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**. При попытке создать или изменить домашнее задание без необходимых прав возвращается ошибка `403 FORBIDDEN` с кодом `HOMEWORK_PERMISSION_DENIED`.

**Рекомендация:** Фронтенд может скрывать кнопки управления материалами и редактирования домашнего задания, если у пользователя нет роли TEACHER или административной роли. Однако окончательная проверка прав выполняется на сервере.

---

## 3. Lesson Materials (материалы урока)

Материалы урока — это файлы, прикреплённые к уроку. Один урок может иметь несколько материалов; каждый материал может содержать несколько файлов. Материалы отображаются в разделе "Lesson Materials" на странице Lesson Details.

### 3.1 GET /api/lessons/{lessonId}/materials — получить список материалов урока

**Назначение:** получить список всех материалов урока для отображения в разделе "Lesson Materials".

**Метод и путь:** `GET /api/lessons/{lessonId}/materials`

**Права доступа:** аутентифицированный пользователь.

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
| `description` | string \| null | Описание материала |
| `authorId` | UUID | ID пользователя, создавшего материал |
| `publishedAt` | string (ISO-8601 datetime) | Дата и время публикации |
| `files` | array of **StoredFileDto** | Список файлов материала (упорядоченный) |

**Структура StoredFileDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID сохранённого файла |
| `size` | integer | Размер файла в байтах |
| `contentType` | string | MIME-тип файла (например, `"application/pdf"`) |
| `originalName` | string | Оригинальное имя файла |
| `uploadedAt` | string (ISO-8601 datetime) | Дата и время загрузки |
| `uploadedBy` | UUID | ID пользователя, загрузившего файл |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден |

**Пример ответа:**

```json
[
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Lecture Slides - Introduction to Algorithms",
    "description": "Slides for the first lecture",
    "authorId": "bb0e8400-e29b-41d4-a716-446655440011",
    "publishedAt": "2025-10-08T10:00:00",
    "files": [
      {
        "id": "cc0e8400-e29b-41d4-a716-446655440012",
        "size": 2411520,
        "contentType": "application/pdf",
        "originalName": "lecture-slides.pdf",
        "uploadedAt": "2025-10-08T09:30:00",
        "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
      }
    ]
  },
  {
    "id": "dd0e8400-e29b-41d4-a716-446655440013",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Code Examples",
    "description": null,
    "authorId": "bb0e8400-e29b-41d4-a716-446655440011",
    "publishedAt": "2025-10-08T11:00:00",
    "files": [
      {
        "id": "ee0e8400-e29b-41d4-a716-446655440014",
        "size": 467456,
        "contentType": "application/zip",
        "originalName": "code-examples.zip",
        "uploadedAt": "2025-10-08T10:45:00",
        "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
      }
    ]
  }
]
```

---

### 3.2 POST /api/lessons/{lessonId}/materials — создать материал урока

**Назначение:** создать новый материал урока с прикреплёнными файлами.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials`

**Права доступа:** **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.

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
| `publishedAt` | string (ISO-8601 datetime) | обязательное | Дата и время публикации |
| `storedFileIds` | array of UUID | опционально | Список ID файлов, которые должны быть прикреплены к материалу (файлы должны быть загружены заранее через `POST /api/documents/upload`) |

**Успешный ответ:** `201 Created` — объект **LessonMaterialDto** (см. раздел 3.1).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | VALIDATION_FAILED | Ошибка валидации (пустое название, превышение длины) |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | LESSON_MATERIAL_CREATE_PERMISSION_DENIED | Пользователь не имеет прав на создание материалов |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из указанных файлов не найден |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения в БД |

**Пример запроса:**

```json
{
  "name": "Lecture Slides - Introduction to Algorithms",
  "description": "Slides for the first lecture",
  "publishedAt": "2025-10-08T10:00:00",
  "storedFileIds": ["cc0e8400-e29b-41d4-a716-446655440012"]
}
```

**Примечание:** Перед созданием материала файлы должны быть загружены через `POST /api/documents/upload` (см. раздел 3.5). Полученные ID файлов передаются в поле `storedFileIds`.

---

### 3.3 DELETE /api/lessons/{lessonId}/materials/{materialId} — удалить материал урока

**Назначение:** удалить материал урока и все связанные с ним файлы. Файлы удаляются из хранилища только если они не используются в других местах (другие материалы, домашние задания, отправки студентов).

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** автор материала или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |
| `materialId` | UUID | ID материала |

**Успешный ответ:** `204 No Content` (тело ответа отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | Пользователь не имеет прав на удаление материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |

---

### 3.4 POST /api/lessons/{lessonId}/materials/{materialId}/files — добавить файлы к материалу

**Назначение:** добавить файлы к существующему материалу урока.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials/{materialId}/files`

**Права доступа:** автор материала или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |
| `materialId` | UUID | ID материала |

**Тело запроса:** объект **AddLessonMaterialFilesRequest**.

**Структура AddLessonMaterialFilesRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `storedFileIds` | array of UUID | обязательное | Список ID файлов для добавления (файлы должны быть загружены заранее) |

**Успешный ответ:** `204 No Content` (тело ответа отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL | Файл уже прикреплён к материалу |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | Пользователь не имеет прав на изменение материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из указанных файлов не найден |

**Пример запроса:**

```json
{
  "storedFileIds": ["ff0e8400-e29b-41d4-a716-446655440015"]
}
```

---

### 3.5 DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId} — удалить файл из материала

**Назначение:** удалить файл из материала. Файл удаляется из хранилища только если он не используется в других местах.

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}`

**Права доступа:** автор материала или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |
| `materialId` | UUID | ID материала |
| `storedFileId` | UUID | ID файла для удаления |

**Успешный ответ:** `204 No Content` (тело ответа отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | Пользователь не имеет прав на изменение материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | LESSON_MATERIAL_FILE_LINK_NOT_FOUND | Файл не прикреплён к данному материалу |

---

### 3.6 POST /api/documents/upload — загрузить файл

**Назначение:** загрузить файл в хранилище. Возвращает метаданные файла, включая ID, который затем используется при создании материала или домашнего задания.

**Метод и путь:** `POST /api/documents/upload`

**Права доступа:** аутентифицированный пользователь.

**Content-Type:** `multipart/form-data`

**Параметры формы:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `file` | file | обязательное | Файл для загрузки |

**Успешный ответ:** `201 Created` — объект **StoredFileDto** (см. раздел 3.1, структура StoredFileDto).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Файл пустой или невалиден |
| 400 | FORBIDDEN_FILE_TYPE | Тип файла не разрешён |
| 400 | FILE_TOO_LARGE | Размер файла превышает максимально допустимый |
| 400 | MALWARE_DETECTED | Файл содержит вредоносное ПО |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 503 | AV_UNAVAILABLE | Сервис проверки на вирусы недоступен |

**Пример ответа:**

```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440012",
  "size": 2411520,
  "contentType": "application/pdf",
  "originalName": "lecture-slides.pdf",
  "uploadedAt": "2025-10-08T09:30:00",
  "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
}
```

**Примечание:** После успешной загрузки файла используйте поле `id` из ответа в запросах создания материала (`storedFileIds`) или домашнего задания (`storedFileId`).

---

### 3.7 GET /api/documents/stored/{id}/download — скачать файл

**Назначение:** скачать файл по его ID. Возвращает поток данных файла с соответствующими заголовками Content-Type и Content-Disposition.

**Метод и путь:** `GET /api/documents/stored/{id}/download`

**Права доступа:** владелец файла или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID сохранённого файла |

**Успешный ответ:** `200 OK` — поток данных файла.

**Заголовки ответа:**
- `Content-Type`: MIME-тип файла
- `Content-Disposition`: `attachment; filename*=UTF-8''<имя_файла>`

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | FORBIDDEN | Пользователь не имеет прав на скачивание файла |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |

**Альтернатива:** Для получения URL для прямого скачивания (presigned URL) используйте `GET /api/documents/stored/{id}/download-url` (см. раздел 3.8).

---

### 3.8 GET /api/documents/stored/{id}/download-url — получить URL для скачивания

**Назначение:** получить временный URL для прямого скачивания файла (presigned URL). URL действителен в течение указанного времени.

**Метод и путь:** `GET /api/documents/stored/{id}/download-url`

**Права доступа:** владелец файла или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | ID сохранённого файла |

**Query-параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `expires` | integer | опционально | Время жизни URL в секундах (по умолчанию 3600) |

**Успешный ответ:** `200 OK` — объект с полем `url`.

**Структура ответа:**

| Поле | Тип | Описание |
|------|-----|----------|
| `url` | string | Временный URL для скачивания файла |

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | FORBIDDEN | Пользователь не имеет прав на скачивание файла |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |

**Пример ответа:**

```json
{
  "url": "https://storage.example.com/files/cc0e8400-e29b-41d4-a716-446655440012?signature=..."
}
```

---

## 4. Homework Assignment (домашнее задание)

Домашнее задание привязано к уроку. На урок может быть назначено одно домашнее задание (или ни одного). Домашнее задание может содержать опциональный прикреплённый файл.

### 4.1 GET /api/lessons/{lessonId}/homework — получить домашнее задание урока

**Назначение:** получить домашнее задание для урока. Возвращает список домашних заданий (обычно одно или пустой список).

**Метод и путь:** `GET /api/lessons/{lessonId}/homework`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Успешный ответ:** `200 OK` — массив объектов **HomeworkDto**.

**Структура HomeworkDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | ID домашнего задания |
| `lessonId` | UUID | ID урока |
| `title` | string | Заголовок домашнего задания |
| `description` | string \| null | Описание домашнего задания |
| `points` | integer \| null | Максимальное количество баллов |
| `file` | object (**StoredFileDto**) \| null | Прикреплённый файл (если есть) |
| `createdAt` | string (ISO-8601 datetime) | Дата создания |
| `updatedAt` | string (ISO-8601 datetime) | Дата последнего обновления |

**Примечание:** Поле `file` имеет тип `Optional<StoredFileDto>` в API, но в JSON оно может быть `null` или объектом **StoredFileDto** (см. раздел 3.1).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден |

**Пример ответа (домашнее задание существует):**

```json
[
  {
    "id": "110e8400-e29b-41d4-a716-446655440020",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Algorithm Analysis Assignment",
    "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
    "points": 100,
    "file": {
      "id": "120e8400-e29b-41d4-a716-446655440021",
      "size": 153600,
      "contentType": "application/pdf",
      "originalName": "assignment.pdf",
      "uploadedAt": "2025-10-08T12:00:00",
      "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
    },
    "createdAt": "2025-10-08T12:00:00",
    "updatedAt": "2025-10-08T12:00:00"
  }
]
```

**Пример ответа (домашнее задание отсутствует):**

```json
[]
```

**Рекомендация для фронтенда:** Если список пустой, отображать пустое состояние или кнопку "Create Assignment". Если список не пустой, взять первый элемент для отображения (обычно на урок назначается одно домашнее задание).

---

### 4.2 POST /api/lessons/{lessonId}/homework — создать домашнее задание

**Назначение:** создать новое домашнее задание для урока.

**Метод и путь:** `POST /api/lessons/{lessonId}/homework`

**Права доступа:** **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | ID урока |

**Тело запроса:** объект **CreateHomeworkRequest**.

**Структура CreateHomeworkRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | обязательное | Заголовок домашнего задания (макс. 500 символов) |
| `description` | string \| null | опционально | Описание (макс. 5000 символов) |
| `points` | integer \| null | опционально | Максимальное количество баллов (положительное число) |
| `storedFileId` | UUID \| null | опционально | ID прикреплённого файла (файл должен быть загружен заранее) |

**Успешный ответ:** `201 Created` — объект **HomeworkDto** (см. раздел 4.1).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | HOMEWORK_VALIDATION_FAILED | Ошибка валидации (пустой заголовок, превышение длины, отрицательные баллы) |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | HOMEWORK_PERMISSION_DENIED | Пользователь не имеет прав на создание домашнего задания |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден |
| 404 | HOMEWORK_FILE_NOT_FOUND | Указанный файл не найден |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения в БД |

**Пример запроса:**

```json
{
  "title": "Algorithm Analysis Assignment",
  "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
  "points": 100,
  "storedFileId": "120e8400-e29b-41d4-a716-446655440021"
}
```

---

### 4.3 GET /api/homework/{homeworkId} — получить домашнее задание по ID

**Назначение:** получить домашнее задание по его ID (альтернатива получению через список по lessonId).

**Метод и путь:** `GET /api/homework/{homeworkId}`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Успешный ответ:** `200 OK` — объект **HomeworkDto** (см. раздел 4.1).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |

---

### 4.4 PUT /api/homework/{homeworkId} — обновить домашнее задание

**Назначение:** обновить домашнее задание. Все поля опциональны; изменяются только переданные поля. Для удаления файла используйте `clearFile: true`.

**Метод и путь:** `PUT /api/homework/{homeworkId}`

**Права доступа:** **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Тело запроса:** объект **UpdateHomeworkRequest**.

**Структура UpdateHomeworkRequest:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string \| null | опционально | Новый заголовок (макс. 500 символов; `null` = без изменений) |
| `description` | string \| null | опционально | Новое описание (макс. 5000 символов; `null` = без изменений) |
| `points` | integer \| null | опционально | Новое количество баллов (`null` = без изменений) |
| `clearFile` | boolean \| null | опционально | Если `true`, удалить ссылку на файл (файл в хранилище не удаляется) |
| `storedFileId` | UUID \| null | опционально | Новый ID файла (используется только если `clearFile` не `true`; `null` = без изменений) |

**Успешный ответ:** `200 OK` — объект **HomeworkDto** (см. раздел 4.1).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | HOMEWORK_VALIDATION_FAILED | Ошибка валидации (превышение длины, отрицательные баллы) |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | HOMEWORK_PERMISSION_DENIED | Пользователь не имеет прав на изменение домашнего задания |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |
| 404 | HOMEWORK_FILE_NOT_FOUND | Указанный новый файл не найден |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения в БД |

**Пример запроса (обновить заголовок и описание):**

```json
{
  "title": "Updated Algorithm Analysis Assignment",
  "description": "Updated description with new requirements."
}
```

**Пример запроса (удалить файл):**

```json
{
  "clearFile": true
}
```

**Пример запроса (изменить файл):**

```json
{
  "storedFileId": "130e8400-e29b-41d4-a716-446655440022"
}
```

**Примечание:** При обновлении домашнего задания не используется механизм оптимистической блокировки (version/locking). Если два пользователя одновременно редактируют одно задание, последнее изменение перезапишет предыдущее.

---

### 4.5 DELETE /api/homework/{homeworkId} — удалить домашнее задание

**Назначение:** удалить домашнее задание. Прикреплённый файл не удаляется из хранилища.

**Метод и путь:** `DELETE /api/homework/{homeworkId}`

**Права доступа:** **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | ID домашнего задания |

**Успешный ответ:** `204 No Content` (тело ответа отсутствует).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден |
| 403 | HOMEWORK_PERMISSION_DENIED | Пользователь не имеет прав на удаление домашнего задания |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено |

---

## 5. Последовательность загрузки страницы

Для загрузки страницы Lesson Details (Teacher view) рекомендуется следующая последовательность запросов:

### 5.1 Начальная загрузка

1. **GET /api/schedule/lessons/{lessonId}** — получить базовую информацию об уроке (дата, время, статус, тема, `offeringId`, `roomId`).
2. **Параллельно:**
   - **GET /api/lessons/{lessonId}/materials** — получить список материалов урока.
   - **GET /api/lessons/{lessonId}/homework** — получить домашнее задание урока.
   - **GET /api/schedule/rooms/{roomId}** (если `roomId` не `null`) — получить информацию об аудитории.

### 5.2 Дополнительная информация (если требуется)

Для получения расширенной информации (предмет, группа, преподаватель) требуется дополнительный запрос к модулю offering:

- **GET /api/offerings/{offeringId}** — получить информацию об offering (включая `groupId`, `curriculumSubjectId`, `teacherId`).

Затем можно получить:
- Информацию о группе через `GET /api/groups/{groupId}`.
- Информацию о предмете через цепочку `curriculumSubjectId` → `subjectId` → `GET /api/subjects/{subjectId}`.
- Информацию о преподавателе через `GET /api/account/teachers` или профиль пользователя.

**Примечание:** Для упрощения фронтенда рекомендуется создать агрегированный эндпоинт `GET /api/schedule/lessons/{id}/details`, который возвращает всю необходимую информацию одним запросом. Однако в текущей версии API такого эндпоинта нет.

### 5.3 Действия пользователя

- **Добавление материала:** сначала `POST /api/documents/upload` (загрузить файл), затем `POST /api/lessons/{lessonId}/materials` (создать материал с `storedFileIds`).
- **Удаление материала:** `DELETE /api/lessons/{lessonId}/materials/{materialId}`.
- **Скачивание файла:** `GET /api/documents/stored/{id}/download` или `GET /api/documents/stored/{id}/download-url`.
- **Создание домашнего задания:** сначала `POST /api/documents/upload` (если нужен файл), затем `POST /api/lessons/{lessonId}/homework`.
- **Редактирование домашнего задания:** `PUT /api/homework/{homeworkId}`.
- **Удаление домашнего задания:** `DELETE /api/homework/{homeworkId}`.

---

## 6. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает | Модуль |
|------|------|----------------|--------|
| 400 | VALIDATION_FAILED | Ошибка валидации полей запроса | Общий |
| 400 | LESSON_MATERIAL_INVALID_NAME | Пустое название материала или превышение длины | Document |
| 400 | LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL | Файл уже прикреплён к материалу | Document |
| 400 | HOMEWORK_VALIDATION_FAILED | Ошибка валидации домашнего задания | Document |
| 400 | FORBIDDEN_FILE_TYPE | Тип файла не разрешён | Document |
| 400 | FILE_TOO_LARGE | Размер файла превышает максимально допустимый | Document |
| 400 | MALWARE_DETECTED | Файл содержит вредоносное ПО | Document |
| 401 | UNAUTHORIZED | Нет JWT или токен невалиден | Общий |
| 403 | FORBIDDEN | Общий код отказа в доступе | Общий |
| 403 | LESSON_MATERIAL_CREATE_PERMISSION_DENIED | Нет прав на создание материалов | Document |
| 403 | LESSON_MATERIAL_PERMISSION_DENIED | Нет прав на изменение/удаление материала | Document |
| 403 | HOMEWORK_PERMISSION_DENIED | Нет прав на управление домашним заданием | Document |
| 404 | NOT_FOUND | Общий код "не найдено" | Общий |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Урок не найден | Schedule |
| 404 | SCHEDULE_ROOM_NOT_FOUND | Аудитория не найдена | Schedule |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | Document |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден при работе с материалами | Document |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Файл не найден | Document |
| 404 | LESSON_MATERIAL_FILE_LINK_NOT_FOUND | Файл не прикреплён к материалу | Document |
| 404 | HOMEWORK_NOT_FOUND | Домашнее задание не найдено | Document |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден при работе с домашним заданием | Document |
| 404 | HOMEWORK_FILE_NOT_FOUND | Файл не найден | Document |
| 404 | STORED_FILE_NOT_FOUND | Сохранённый файл не найден | Document |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения материала в БД | Document |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения домашнего задания в БД | Document |
| 503 | AV_UNAVAILABLE | Сервис проверки на вирусы недоступен | Document |

---

## 7. Примеры запросов и ответов

### 7.1 Получение информации об уроке и материалах

**Запрос 1: Получить урок**

```http
GET /api/schedule/lessons/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <JWT>
```

**Ответ:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "offeringSlotId": "770e8400-e29b-41d4-a716-446655440002",
  "date": "2025-10-08",
  "startTime": "13:00:00",
  "endTime": "14:30:00",
  "timeslotId": null,
  "roomId": "990e8400-e29b-41d4-a716-446655440004",
  "topic": "Introduction to Algorithms",
  "status": "PLANNED",
  "createdAt": "2025-10-01T10:00:00",
  "updatedAt": "2025-10-01T10:00:00"
}
```

**Запрос 2: Получить материалы урока**

```http
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
```

**Ответ:**

```json
[
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440010",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Lecture Slides - Introduction to Algorithms",
    "description": "Slides for the first lecture",
    "authorId": "bb0e8400-e29b-41d4-a716-446655440011",
    "publishedAt": "2025-10-08T10:00:00",
    "files": [
      {
        "id": "cc0e8400-e29b-41d4-a716-446655440012",
        "size": 2411520,
        "contentType": "application/pdf",
        "originalName": "lecture-slides.pdf",
        "uploadedAt": "2025-10-08T09:30:00",
        "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
      }
    ]
  }
]
```

---

### 7.2 Создание материала с файлом

**Шаг 1: Загрузить файл**

```http
POST /api/documents/upload
Authorization: Bearer <JWT>
Content-Type: multipart/form-data

file: <файл>
```

**Ответ:**

```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440012",
  "size": 2411520,
  "contentType": "application/pdf",
  "originalName": "lecture-slides.pdf",
  "uploadedAt": "2025-10-08T09:30:00",
  "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
}
```

**Шаг 2: Создать материал**

```http
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Lecture Slides - Introduction to Algorithms",
  "description": "Slides for the first lecture",
  "publishedAt": "2025-10-08T10:00:00",
  "storedFileIds": ["cc0e8400-e29b-41d4-a716-446655440012"]
}
```

**Ответ:** `201 Created` — объект LessonMaterialDto (см. раздел 3.1).

---

### 7.3 Получение и создание домашнего задания

**Запрос: Получить домашнее задание**

```http
GET /api/lessons/550e8400-e29b-41d4-a716-446655440000/homework
Authorization: Bearer <JWT>
```

**Ответ (домашнее задание существует):**

```json
[
  {
    "id": "110e8400-e29b-41d4-a716-446655440020",
    "lessonId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Algorithm Analysis Assignment",
    "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
    "points": 100,
    "file": {
      "id": "120e8400-e29b-41d4-a716-446655440021",
      "size": 153600,
      "contentType": "application/pdf",
      "originalName": "assignment.pdf",
      "uploadedAt": "2025-10-08T12:00:00",
      "uploadedBy": "bb0e8400-e29b-41d4-a716-446655440011"
    },
    "createdAt": "2025-10-08T12:00:00",
    "updatedAt": "2025-10-08T12:00:00"
  }
]
```

**Запрос: Создать домашнее задание**

```http
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/homework
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Algorithm Analysis Assignment",
  "description": "Analyze the time complexity of the provided algorithms and implement optimized versions.",
  "points": 100,
  "storedFileId": "120e8400-e29b-41d4-a716-446655440021"
}
```

**Ответ:** `201 Created` — объект HomeworkDto (см. раздел 4.1).

---

### 7.4 Обновление домашнего задания

**Запрос: Обновить заголовок и описание**

```http
PUT /api/homework/110e8400-e29b-41d4-a716-446655440020
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Updated Algorithm Analysis Assignment",
  "description": "Updated description with new requirements."
}
```

**Ответ:** `200 OK` — обновлённый объект HomeworkDto.

---

### 7.5 Ошибка валидации

**Запрос: Создать материал с пустым названием**

```http
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "",
  "publishedAt": "2025-10-08T10:00:00"
}
```

**Ответ:** `400 Bad Request`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Ошибка проверки данных. Проверьте заполненные поля.",
  "timestamp": "2025-10-08T12:00:00.123Z",
  "details": {
    "name": "name is required"
  }
}
```

---

### 7.6 Ошибка "не найдено"

**Запрос: Получить несуществующий урок**

```http
GET /api/schedule/lessons/00000000-0000-0000-0000-000000000000
Authorization: Bearer <JWT>
```

**Ответ:** `404 Not Found`

```json
{
  "code": "SCHEDULE_LESSON_NOT_FOUND",
  "message": "Lesson not found: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2025-10-08T12:00:00.123Z",
  "details": null
}
```

---

### 7.7 Ошибка прав доступа

**Запрос: Создать материал без прав TEACHER**

```http
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Test Material",
  "publishedAt": "2025-10-08T10:00:00"
}
```

**Ответ:** `403 Forbidden`

```json
{
  "code": "LESSON_MATERIAL_CREATE_PERMISSION_DENIED",
  "message": "Only teachers and administrators can create lesson materials",
  "timestamp": "2025-10-08T12:00:00.123Z",
  "details": null
}
```

---

## 8. Заключение

Данный контракт описывает все эндпоинты, необходимые для реализации страницы Lesson Details (Teacher view):

- **Полная информация об уроке:** базовые данные через `GET /api/schedule/lessons/{id}` и дополнительную информацию через другие модули при необходимости.
- **Материалы урока:** список, создание, удаление, добавление/удаление файлов, загрузка и скачивание файлов.
- **Домашнее задание:** получение, создание, обновление, удаление.

Все операции требуют аутентификации через JWT. Операции изменения данных требуют роли TEACHER или административной роли. Ошибки возвращаются в едином формате ErrorResponse с кодами ошибок, понятными для фронтенда.
