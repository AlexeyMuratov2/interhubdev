# Контракт API: страница полной информации по уроку (дашборд учителя)

Документ-контракт для фронтенда. Описывает эндпоинты, необходимые для реализации страницы полной информации по уроку на дашборде учителя: **изменение и удаление урока**, **загрузка и скачивание файлов**, **материалы к уроку**, **домашние задания**. Эндпоинт **получения** полной информации по уроку (агрегат) уже описан в **`docs/api-composition-contract.md`** (GET `/api/composition/lessons/{lessonId}/full-details`) и здесь не дублируется.

---

## 1. Общие сведения

### 1.1 Связь с другими контрактами

- **Получение данных для страницы урока** (предмет, группа, урок, материалы, домашние задания, аудитория, преподаватели и т.д.) — один запрос: **GET `/api/composition/lessons/{lessonId}/full-details`**. Полное описание запроса и ответа **LessonFullDetailsDto** см. в **`docs/api-composition-contract.md`**.
- В этом документе описаны только **изменяющие** и **дополнительные read** эндпоинты: изменение/удаление урока (Schedule), работа с файлами (Document), CRUD материалов к уроку и домашних заданий (Document).

### 1.2 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/schedule` | Уроки: изменение, удаление (а также создание, получение по id — при необходимости) |
| `/api/documents` | Загрузка, скачивание, превью и удаление файлов (stored files) |
| `/api/lessons` | Материалы к уроку и домашние задания (список, создание); вложенные пути под конкретный урок |
| `/api/lessons/{lessonId}/materials` | Конкретный материал: получение, удаление, добавление/удаление файлов |
| `/api/homework` | Домашнее задание по id: получение, обновление, удаление |

Хост и порт задаются окружением развёртывания.

### 1.3 Авторизация

- Все запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (или с учётом cookie-механизма проекта).
- **Чтение** (списки материалов/домашних заданий, метаданные файла, скачивание): аутентифицированный пользователь; для скачивания/превью/удаления файла — дополнительно проверяется право доступа (владелец файла или роль ADMIN/MODERATOR).
- **Изменение/удаление урока**: роли **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.
- **Создание/редактирование материалов и домашних заданий**: роли **TEACHER**, **ADMIN** (для изменения/удаления материала — автор материала или **ADMIN**/**MODERATOR**).
- При отсутствии JWT или невалидном токене возвращается **401 Unauthorized**; при недостатке прав — **403 Forbidden**.

### 1.4 Типы данных

- **UUID** — строка в формате UUID.
- **Даты и время** — **ISO-8601** (дата-время, дата, время HH:mm:ss при необходимости).
- **Числа** — JSON number; размер файла — integer (байты).
- **null** — явное отсутствие значения где допускается контрактом.

### 1.5 Формат ошибок

Единый формат **ErrorResponse** (как в `api-composition-contract.md`):

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `SCHEDULE_LESSON_NOT_FOUND`, `UPLOAD_FILE_TOO_LARGE`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент ошибки (ISO-8601, UTC) |
| `details` | object \| null | Для валидации — объект «поле → сообщение»; иначе может отсутствовать или быть `null` |

---

## 2. Модели данных (краткая справка)

Полные структуры **LessonDto**, **LessonMaterialDto**, **HomeworkDto**, **StoredFileDto** приведены в **`docs/api-composition-contract.md`** (разделы 2.1, 2.10, 2.11, 2.12). Ниже — только то, что нужно для **запросов** и для однозначной реализации.

### 2.1 StoredFileDto (метаданные файла)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла в хранилище |
| `size` | integer | Размер в байтах |
| `contentType` | string \| null | MIME-тип |
| `originalName` | string \| null | Оригинальное имя файла |
| `uploadedAt` | string | Дата загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 2.2 LessonMaterialDto

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id материала |
| `lessonId` | UUID | Id урока |
| `name` | string \| null | Название |
| `description` | string \| null | Описание |
| `authorId` | UUID | Id автора |
| `publishedAt` | string | Дата публикации (ISO-8601) |
| `files` | array of StoredFileDto | Упорядоченный список файлов |

### 2.3 HomeworkDto

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id домашнего задания |
| `lessonId` | UUID | Id урока |
| `title` | string \| null | Заголовок |
| `description` | string \| null | Описание |
| `points` | integer \| null | Максимальные баллы |
| `file` | StoredFileDto \| null | Прикреплённый файл (может отсутствовать) |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

### 2.4 LessonDto (ответ при обновлении урока)

Структура совпадает с п. 2.1 в `api-composition-contract.md`: `id`, `offeringId`, `offeringSlotId`, `date`, `startTime`, `endTime`, `timeslotId`, `roomId`, `topic`, `status`, `createdAt`, `updatedAt`. Дата — строка ISO-8601 date; время — строка **HH:mm:ss**.

---

## 3. Изменение и удаление урока (модуль Schedule)

Уроки изменяются и удаляются через модуль **Schedule**. Получение одного урока по id: **GET `/api/schedule/lessons/{id}`** (возвращает **LessonDto**).

### 3.1 PUT /api/schedule/lessons/{id} — изменить урок

**Назначение:** обновить время, аудиторию, тему и статус урока (для экрана редактирования урока на дашборде учителя). Доступно только пользователям с ролями MODERATOR, ADMIN, SUPER_ADMIN.

**Метод и путь:** `PUT /api/schedule/lessons/{id}`

**Права доступа:** **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id урока |

**Query-параметры:** нет.

**Тело запроса:** объект **UpdateLessonRequest**. Все поля опциональны (передаются только изменяемые).

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `startTime` | string | нет | Время начала в формате HH:mm:ss |
| `endTime` | string | нет | Время окончания в формате HH:mm:ss |
| `roomId` | UUID \| null | нет | Id аудитории; `null` — снять назначение |
| `topic` | string \| null | нет | Тема урока |
| `status` | string \| null | нет | Статус: `PLANNED`, `CANCELLED`, `DONE` или `null` |

**Успешный ответ:** `200 OK`. Тело — объект **LessonDto** (обновлённый урок).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Нет роли MODERATOR/ADMIN/SUPER_ADMIN | Сообщение от системы авторизации |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <id>"` |
| 400 | (валидация) | Неверный формат времени/полей | Сообщение в `message` или `details` |

**Логика:** сервер обновляет только переданные поля; остальные не меняются. Время задаётся в формате HH:mm:ss. Если передан `roomId: null`, аудитория у урока снимается.

---

### 3.2 DELETE /api/schedule/lessons/{id} — удалить урок

**Назначение:** удалить урок из расписания. Используется при необходимости отменить/удалить конкретный урок на странице урока. Каскадно могут удаляться или обнуляться связанные сущности (в зависимости от реализации бэкенда); материалы и домашние задания, привязанные к уроку, обрабатываются по правилам модуля document (например, материалы/ДЗ могут оставаться с orphan-ссылкой или удаляться — уточнять по реализации). Для дашборда учителя достаточно знать: урок после вызова перестаёт существовать по данному id.

**Метод и путь:** `DELETE /api/schedule/lessons/{id}`

**Права доступа:** **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id урока |

**Query-параметры:** нет. **Тело запроса:** не используется.

**Успешный ответ:** `204 No Content`. Тело пустое.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Нет роли MODERATOR/ADMIN/SUPER_ADMIN | — |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <id>"` |

---

## 4. Загрузка и скачивание файлов (модуль Document)

Файлы сначала **загружаются** через общий эндпоинт загрузки; в ответ приходит **StoredFileDto** с `id`. Этот `id` затем используется при создании/редактировании **материала к уроку** или **домашнего задания** (привязка файла к материалу или к ДЗ). Скачивание и превью доступны по `id` хранимого файла.

### 4.1 POST /api/documents/upload — загрузить файл

**Назначение:** загрузить файл в хранилище и получить метаданные (StoredFileDto). Полученный `id` передаётся в запросах создания материала (поле `storedFileIds`) или домашнего задания (поле `storedFileId`). Один и тот же загруженный файл можно привязать к нескольким материалам/ДЗ в рамках правил бэкенда.

**Метод и путь:** `POST /api/documents/upload`

**Права доступа:** любой аутентифицированный пользователь (для загрузки); дальнейшее использование файла (привязка к материалу/ДЗ) проверяется при вызове соответствующих эндпоинтов.

**Параметры:** не в path/query. Тело — **multipart/form-data**.

| Part | Имя | Тип | Обязательность | Описание |
|------|-----|-----|----------------|----------|
| file | `file` | binary (file) | да | Файл для загрузки |

**Успешный ответ:** `201 Created`. Тело — объект **StoredFileDto** (п. 2.1). Поле `id` обязательно использовать для привязки к материалу или домашнему заданию.

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 400 | (bad request) | Тело пустое или файл не передан | `"File is empty"` |
| 400 | UPLOAD_EMPTY_FILE | Размер файла ≤ 0 | `"File size must be positive"` |
| 400 | UPLOAD_FORBIDDEN_FILE_TYPE | MIME-тип не разрешён политикой | `"Content type not allowed: ..."` |
| 400 | UPLOAD_EXTENSION_MISMATCH | Расширение не соответствует MIME | Сообщение о несоответствии |
| 400 | UPLOAD_SUSPICIOUS_FILENAME | Подозрительное имя (двойное расширение, path traversal и т.п.) | Сообщение от сервера |
| 400 | UPLOAD_CONTENT_TYPE_MISMATCH | Magic bytes не соответствуют заявленному MIME | Сообщение от сервера |
| 400 | UPLOAD_MALWARE_DETECTED | Антивирус обнаружил угрозу | `"File rejected"` |
| 413 | UPLOAD_FILE_TOO_LARGE | Размер файла превышает лимит | `"File size exceeds maximum allowed size of 50 MB"` (по умолчанию лимит 50 MB, задаётся конфигурацией `app.document.max-file-size-bytes`) |
| 503 | UPLOAD_AV_UNAVAILABLE | Антивирус недоступен (политика fail-closed) | Сообщение от сервера |
| 500 | INTERNAL_ERROR / UPLOAD_FAILED | Ошибка сохранения или хранилища | `"Failed to upload file. Please try again."` |

**Разрешённые типы файлов (политика по умолчанию):** PDF, DOC, DOCX, XLS, XLSX, TXT, LOG, CSV, JPEG, PNG, GIF, WEBP. Конкретный список MIME и расширений проверяется на бэкенде; при несоответствии возвращается **UPLOAD_FORBIDDEN_FILE_TYPE** или **UPLOAD_EXTENSION_MISMATCH**.

**Логика:** после успешной загрузки файл сохраняется в хранилище (например S3), метаданные — в БД. Фронт сохраняет `id` и передаёт его при создании/редактировании материала или домашнего задания.

---

### 4.2 GET /api/documents/stored/{id} — метаданные хранимого файла

**Назначение:** получить метаданные файла по его `id` (размер, имя, тип, дата загрузки). Используется для отображения информации о прикреплённом файле без скачивания.

**Метод и путь:** `GET /api/documents/stored/{id}`

**Права доступа:** аутентифицированный пользователь (метаданные доступны при наличии доступа к контексту, где файл используется).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id хранимого файла |

**Успешный ответ:** `200 OK`. Тело — объект **StoredFileDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден | `"Stored file not found: <id>"` |

---

### 4.3 GET /api/documents/stored/{id}/download — скачать файл (поток)

**Назначение:** получить содержимое файла потоком через бэкенд. Ответ — бинарное тело с заголовком `Content-Disposition: attachment` и именем файла. Подходит для кнопки «Скачать» на странице урока (материалы, ДЗ).

**Метод и путь:** `GET /api/documents/stored/{id}/download`

**Права доступа:** аутентифицированный пользователь, являющийся **владельцем файла** (`uploadedBy`) или имеющий роль **ADMIN** / **MODERATOR**. Иначе — 403.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id хранимого файла |

**Успешный ответ:** `200 OK`. Тело — бинарный поток; заголовки: `Content-Type` (MIME файла), `Content-Disposition: attachment; filename*=UTF-8''<encoded-name>`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | ACCESS_DENIED | Нет права на скачивание (не владелец и не админ/модератор) | `"You don't have permission to access this file"` |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 404 | FILE_NOT_IN_STORAGE | Метаданные есть, файл в хранилище отсутствует |

---

### 4.4 GET /api/documents/stored/{id}/download-url — URL для прямого скачивания

**Назначение:** получить одноразовый (presigned) URL для скачивания файла напрямую из хранилища, без проксирования через бэкенд. Удобно для открытия скачивания в новой вкладке или для больших файлов.

**Метод и путь:** `GET /api/documents/stored/{id}/download-url`

**Права доступа:** те же, что и для скачивания (владелец или ADMIN/MODERATOR).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id хранимого файла |

**Query-параметры:**

| Параметр | Тип | Обязательность | По умолчанию | Описание |
|----------|-----|----------------|--------------|----------|
| `expires` | integer | нет | 3600 | Время жизни URL в секундах |

**Успешный ответ:** `200 OK`. Тело — JSON: `{ "url": "<presigned-url>" }`. Клиент может перенаправить пользователя по этому URL для скачивания.

**Ошибки:** те же, что для GET download (401, 403, 404).

---

### 4.5 GET /api/documents/stored/{id}/preview — URL для превью

**Назначение:** получить presigned URL для просмотра/превью файла (например, изображения или PDF в новой вкладке). Права доступа те же, что у скачивания.

**Метод и путь:** `GET /api/documents/stored/{id}/preview`

**Query-параметры:**

| Параметр | Тип | Обязательность | По умолчанию | Описание |
|----------|-----|----------------|--------------|----------|
| `expires` | integer | нет | 3600 | Время жизни URL в секундах |

**Успешный ответ:** `200 OK`. Тело — JSON: `{ "url": "<presigned-url>" }`.

**Ошибки:** те же, что для download (401, 403, 404).

---

### 4.6 DELETE /api/documents/stored/{id} — удалить хранимый файл

**Назначение:** удалить файл из хранилища и запись метаданных. Вызывать только если файл **не привязан** ни к одному материалу и ни к одному домашнему заданию (иначе сервер вернёт 409). Используется, например, при отмене загрузки до привязки к материалу/ДЗ или после явного открепления везде.

**Метод и путь:** `DELETE /api/documents/stored/{id}`

**Права доступа:** владелец файла или **ADMIN** / **MODERATOR**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id хранимого файла |

**Успешный ответ:** `204 No Content`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | ACCESS_DENIED | Нет права на удаление |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 409 | FILE_IN_USE | Файл используется (привязан к материалу или ДЗ) | `"Cannot delete file: file is currently in use"` |

---

## 5. Материалы к уроку (модуль Document)

Один урок может иметь несколько **материалов**; каждый материал имеет название, описание, дату публикации и **список привязанных файлов** (StoredFileDto). Файлы к материалу добавляются по `storedFileIds` (id получают из POST `/api/documents/upload`). Список материалов по уроку входит также в ответ **GET /api/composition/lessons/{lessonId}/full-details**; ниже описаны отдельные эндпоинты для создания, изменения (добавление/удаление файлов) и удаления материала.

### 5.1 GET /api/lessons/{lessonId}/materials — список материалов урока

**Назначение:** получить все материалы урока (например, для отображения списка на странице урока, если не использовать только агрегат full-details).

**Метод и путь:** `GET /api/lessons/{lessonId}/materials`

**Права доступа:** любой аутентифицированный пользователь, имеющий доступ к просмотру урока/материалов (проверка на бэкенде).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Успешный ответ:** `200 OK`. Тело — массив **LessonMaterialDto** (п. 2.2); порядок по реализации бэкенда (например, по дате публикации).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |

---

### 5.2 POST /api/lessons/{lessonId}/materials — создать материал к уроку

**Назначение:** создать новый материал к уроку (название, описание, дата публикации, опционально список id уже загруженных файлов). Файлы должны быть загружены заранее через **POST /api/documents/upload**; их `id` передаются в `storedFileIds`.

**Метод и путь:** `POST /api/lessons/{lessonId}/materials`

**Права доступа:** **TEACHER** или **ADMIN** (и при необходимости MODERATOR/SUPER_ADMIN по реализации).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Тело запроса:** объект **CreateLessonMaterialRequest**.

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `name` | string | да | Название материала; макс. 500 символов |
| `description` | string | нет | Описание; макс. 5000 символов |
| `publishedAt` | string (ISO-8601 date-time) | да | Дата публикации |
| `storedFileIds` | array of UUID | нет | Список id файлов из хранилища (из upload); пустой массив или отсутствие — без файлов |

**Успешный ответ:** `201 Created`. Тело — объект **LessonMaterialDto** (созданный материал, включая поле `files`).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Нет роли TEACHER/ADMIN | `"Only teachers and administrators can create lesson materials"` |
| 404 | LESSON_MATERIAL_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из `storedFileIds` не найден | `"Stored file not found: <id>"` |
| 400 | (валидация) | Пустое имя, превышение длины | Сообщение в `message` или `details` |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения | `"Failed to save lesson material. Please try again."` |

**Логика:** создаётся запись материала, привязываемая к уроку; автор — текущий пользователь. Файлы по `storedFileIds` связываются с материалом в указанном порядке (если порядок поддерживается бэкендом).

---

### 5.3 GET /api/lessons/{lessonId}/materials/{materialId} — получить материал по id

**Назначение:** получить один материал урока по его id (например, для формы редактирования или детального просмотра).

**Метод и путь:** `GET /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** аутентифицированный пользователь с доступом к уроку/материалам.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока (контекст пути) |
| `materialId` | UUID | Id материала |

**Успешный ответ:** `200 OK`. Тело — объект **LessonMaterialDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден | `"Lesson material not found: <materialId>"` |

---

### 5.4 DELETE /api/lessons/{lessonId}/materials/{materialId} — удалить материал

**Назначение:** удалить материал урока и связи с файлами. Файлы в хранилище удаляются только если они больше нигде не используются (иначе остаются в хранилище).

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}`

**Права доступа:** **автор материала** или **ADMIN** / **MODERATOR**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |
| `materialId` | UUID | Id материала |

**Успешный ответ:** `204 No Content`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Нет права (не автор и не админ/модератор) | `"You don't have permission to modify this lesson material"` |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |

---

### 5.5 POST /api/lessons/{lessonId}/materials/{materialId}/files — добавить файлы к материалу

**Назначение:** привязать к существующему материалу дополнительные файлы по их id (файлы должны быть уже загружены через POST /api/documents/upload).

**Метод и путь:** `POST /api/lessons/{lessonId}/materials/{materialId}/files`

**Права доступа:** автор материала или **ADMIN** / **MODERATOR**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |
| `materialId` | UUID | Id материала |

**Тело запроса:** объект **AddLessonMaterialFilesRequest**.

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `storedFileIds` | array of UUID | да | Список id хранимых файлов |

**Успешный ответ:** `204 No Content`. Тело пустое. Обновлённый состав файлов можно получить через GET материала или GET full-details.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Нет права на изменение материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | LESSON_MATERIAL_STORED_FILE_NOT_FOUND | Один из файлов не найден |
| 400 | LESSON_MATERIAL_FILE_ALREADY_IN_MATERIAL | Файл уже привязан к этому материалу | `"File already attached to this material: <storedFileId>"` |
| 422 | LESSON_MATERIAL_SAVE_FAILED | Ошибка сохранения |

**Логика:** дубликаты в `storedFileIds` не допускаются (один файл — одна связь с материалом). Новые файлы добавляются к существующему списку (порядок по реализации бэкенда).

---

### 5.6 DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId} — открепить файл от материала

**Назначение:** убрать привязку файла к материалу. Сам файл в хранилище удаляется только если он больше нигде не используется; иначе остаётся (например, для повторного использования).

**Метод и путь:** `DELETE /api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}`

**Права доступа:** автор материала или **ADMIN** / **MODERATOR**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |
| `materialId` | UUID | Id материала |
| `storedFileId` | UUID | Id хранимого файла |

**Успешный ответ:** `204 No Content`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Нет права на изменение материала |
| 404 | LESSON_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | LESSON_MATERIAL_FILE_LINK_NOT_FOUND | Файл не привязан к этому материалу | `"File is not attached to this material: ..."` |

---

## 6. Домашние задания (модуль Document)

Домашние задания привязаны к уроку; у каждого ДЗ может быть один прикреплённый файл (или ни одного). Список домашних заданий урока входит в **GET /api/composition/lessons/{lessonId}/full-details**. Ниже — отдельные эндпоинты для списка, создания, получения по id, обновления и удаления.

### 6.1 GET /api/lessons/{lessonId}/homework — список домашних заданий урока

**Назначение:** получить все домашние задания урока (если не использовать только агрегат full-details).

**Метод и путь:** `GET /api/lessons/{lessonId}/homework`

**Права доступа:** аутентифицированный пользователь с доступом к уроку.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Успешный ответ:** `200 OK`. Тело — массив **HomeworkDto** (п. 2.3).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |

---

### 6.2 POST /api/lessons/{lessonId}/homework — создать домашнее задание

**Назначение:** создать домашнее задание к уроку (заголовок, описание, баллы, опционально один прикреплённый файл). Файл должен быть загружен заранее через **POST /api/documents/upload**; его `id` передаётся в `storedFileId`.

**Метод и путь:** `POST /api/lessons/{lessonId}/homework`

**Права доступа:** **TEACHER** или **ADMIN** (и при необходимости MODERATOR/SUPER_ADMIN по реализации).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `lessonId` | UUID | Id урока |

**Тело запроса:** объект **CreateHomeworkRequest**.

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | да | Заголовок; макс. 500 символов |
| `description` | string | нет | Описание; макс. 5000 символов |
| `points` | integer | нет | Максимальное количество баллов |
| `storedFileId` | UUID | нет | Id файла из хранилища (из upload); при отсутствии ДЗ без файла |

**Успешный ответ:** `201 Created`. Тело — объект **HomeworkDto** (созданное ДЗ, включая `file` при наличии).

**Ошибки:**

| HTTP | code | Когда возникает | Пример сообщения |
|------|------|------------------|-------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT | `"Authentication required"` |
| 403 | FORBIDDEN | Нет роли TEACHER/ADMIN | `"You don't have permission to manage homework"` |
| 404 | HOMEWORK_LESSON_NOT_FOUND | Урок не найден | `"Lesson not found: <lessonId>"` |
| 404 | HOMEWORK_FILE_NOT_FOUND | Указанный storedFileId не найден | `"File not found: <fileId>"` |
| 400 | HOMEWORK_VALIDATION_FAILED | Валидация полей | Сообщение в `message` |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения | `"Failed to save homework. Please try again."` |

---

### 6.3 GET /api/homework/{homeworkId} — получить домашнее задание по id

**Назначение:** получить одно домашнее задание по его id (для формы редактирования или детального просмотра). Путь не привязан к lessonId — идентификация только по homeworkId.

**Метод и путь:** `GET /api/homework/{homeworkId}`

**Права доступа:** аутентифицированный пользователь с доступом к контексту этого ДЗ (урок/группа).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | Id домашнего задания |

**Успешный ответ:** `200 OK`. Тело — объект **HomeworkDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 404 | HOMEWORK_NOT_FOUND | ДЗ не найдено | `"Homework not found: <homeworkId>"` |

---

### 6.4 PUT /api/homework/{homeworkId} — обновить домашнее задание

**Назначение:** изменить заголовок, описание, баллы и/или прикреплённый файл. Можно снять привязку файла (`clearFile: true`) или установить новый файл (`storedFileId`). Файл в хранилище при снятии привязки не удаляется.

**Метод и путь:** `PUT /api/homework/{homeworkId}`

**Права доступа:** **TEACHER** или **ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | Id домашнего задания |

**Тело запроса:** объект **UpdateHomeworkRequest**. Все поля опциональны (передаются только изменяемые).

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `title` | string | нет | Заголовок; макс. 500 символов |
| `description` | string | нет | Описание; макс. 5000 символов |
| `points` | integer | нет | Максимальные баллы |
| `clearFile` | boolean | нет | Если `true` — снять привязку файла (файл в хранилище не удаляется). Если одновременно передан `storedFileId`, приоритет у `storedFileId` (новый файл) |
| `storedFileId` | UUID | нет | Установить или заменить прикреплённый файл (используется, если `clearFile` не true) |

**Успешный ответ:** `200 OK`. Тело — объект **HomeworkDto** (обновлённое ДЗ).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Нет роли TEACHER/ADMIN |
| 404 | HOMEWORK_NOT_FOUND | ДЗ не найдено |
| 404 | HOMEWORK_FILE_NOT_FOUND | Указанный storedFileId не найден |
| 400 | HOMEWORK_VALIDATION_FAILED | Валидация полей |
| 422 | HOMEWORK_SAVE_FAILED | Ошибка сохранения |

**Логика:** обновляются только переданные поля. Для файла: `clearFile: true` без `storedFileId` — убрать привязку; `storedFileId` (и не `clearFile: true`) — установить или заменить файл.

---

### 6.5 DELETE /api/homework/{homeworkId} — удалить домашнее задание

**Назначение:** удалить домашнее задание. Прикреплённый файл из хранилища **не удаляется** (остаётся в stored files на случай повторного использования или ручной очистки).

**Метод и путь:** `DELETE /api/homework/{homeworkId}`

**Права доступа:** **TEACHER** или **ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `homeworkId` | UUID | Id домашнего задания |

**Успешный ответ:** `204 No Content`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Нет роли TEACHER/ADMIN |
| 404 | HOMEWORK_NOT_FOUND | ДЗ не найдено |

---

## 7. Сводная таблица эндпоинтов (страница урока, дашборд учителя)

| Действие | Метод | Путь | Модуль |
|----------|--------|------|--------|
| Полная информация по уроку (данные для страницы) | GET | `/api/composition/lessons/{lessonId}/full-details` | Composition (см. api-composition-contract.md) |
| Получить урок по id | GET | `/api/schedule/lessons/{id}` | Schedule |
| Изменить урок | PUT | `/api/schedule/lessons/{id}` | Schedule |
| Удалить урок | DELETE | `/api/schedule/lessons/{id}` | Schedule |
| Загрузить файл | POST | `/api/documents/upload` | Document |
| Метаданные файла | GET | `/api/documents/stored/{id}` | Document |
| Скачать файл (поток) | GET | `/api/documents/stored/{id}/download` | Document |
| URL для скачивания | GET | `/api/documents/stored/{id}/download-url` | Document |
| URL для превью | GET | `/api/documents/stored/{id}/preview` | Document |
| Удалить файл | DELETE | `/api/documents/stored/{id}` | Document |
| Список материалов урока | GET | `/api/lessons/{lessonId}/materials` | Document |
| Создать материал | POST | `/api/lessons/{lessonId}/materials` | Document |
| Получить материал | GET | `/api/lessons/{lessonId}/materials/{materialId}` | Document |
| Удалить материал | DELETE | `/api/lessons/{lessonId}/materials/{materialId}` | Document |
| Добавить файлы к материалу | POST | `/api/lessons/{lessonId}/materials/{materialId}/files` | Document |
| Удалить файл из материала | DELETE | `/api/lessons/{lessonId}/materials/{materialId}/files/{storedFileId}` | Document |
| Список ДЗ урока | GET | `/api/lessons/{lessonId}/homework` | Document |
| Создать ДЗ | POST | `/api/lessons/{lessonId}/homework` | Document |
| Получить ДЗ по id | GET | `/api/homework/{homeworkId}` | Document |
| Обновить ДЗ | PUT | `/api/homework/{homeworkId}` | Document |
| Удалить ДЗ | DELETE | `/api/homework/{homeworkId}` | Document |

---

## 8. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | UPLOAD_EMPTY_FILE, UPLOAD_FORBIDDEN_FILE_TYPE, UPLOAD_EXTENSION_MISMATCH, UPLOAD_SUSPICIOUS_FILENAME, UPLOAD_CONTENT_TYPE_MISMATCH, UPLOAD_MALWARE_DETECTED | Загрузка: пустой файл, неверный тип/расширение/имя, несоответствие контента, угроза |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN, ACCESS_DENIED, LESSON_MATERIAL_PERMISSION_DENIED, LESSON_MATERIAL_CREATE_PERMISSION_DENIED, HOMEWORK_PERMISSION_DENIED | Нет прав на действие |
| 404 | SCHEDULE_LESSON_NOT_FOUND, STORED_FILE_NOT_FOUND, FILE_NOT_IN_STORAGE, LESSON_MATERIAL_NOT_FOUND, LESSON_MATERIAL_LESSON_NOT_FOUND, LESSON_MATERIAL_STORED_FILE_NOT_FOUND, LESSON_MATERIAL_FILE_LINK_NOT_FOUND, HOMEWORK_NOT_FOUND, HOMEWORK_LESSON_NOT_FOUND, HOMEWORK_FILE_NOT_FOUND | Ресурс не найден |
| 409 | FILE_IN_USE | Удаление файла: файл используется (привязан к материалу/ДЗ) |
| 413 | UPLOAD_FILE_TOO_LARGE | Размер файла превышает лимит (напр. 50 MB) |
| 422 | LESSON_MATERIAL_SAVE_FAILED, HOMEWORK_SAVE_FAILED | Ошибка сохранения материала/ДЗ |
| 503 | UPLOAD_AV_UNAVAILABLE, STORAGE_UNAVAILABLE | Антивирус или хранилище недоступны |
| 500 | INTERNAL_ERROR, UPLOAD_FAILED, SAVE_FAILED | Внутренняя ошибка / сбой загрузки или сохранения |

---

## 9. Примеры запросов и ответов

### 9.1 Изменение урока (время и тема)

**Запрос:**

```
PUT /api/schedule/lessons/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "startTime": "14:00:00",
  "endTime": "15:30:00",
  "topic": "Algorithms: sorting",
  "status": "PLANNED"
}
```

**Ответ:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "offeringId": "660e8400-e29b-41d4-a716-446655440001",
  "offeringSlotId": "770e8400-e29b-41d4-a716-446655440002",
  "date": "2025-02-19",
  "startTime": "14:00:00",
  "endTime": "15:30:00",
  "timeslotId": "880e8400-e29b-41d4-a716-446655440003",
  "roomId": "990e8400-e29b-41d4-a716-446655440004",
  "topic": "Algorithms: sorting",
  "status": "PLANNED",
  "createdAt": "2025-02-01T10:00:00",
  "updatedAt": "2025-02-19T12:05:00"
}
```

---

### 9.2 Загрузка файла (multipart)

**Запрос:** (пример с curl)

```
POST /api/documents/upload
Authorization: Bearer <JWT>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="lecture-1.pdf"
Content-Type: application/pdf

<binary content>
------WebKitFormBoundary--
```

**Ответ:** `201 Created`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "size": 1048576,
  "contentType": "application/pdf",
  "originalName": "lecture-1.pdf",
  "uploadedAt": "2025-02-19T12:10:00",
  "uploadedBy": "12345678-1234-1234-1234-123456789abc"
}
```

Дальше `id` передаётся в `storedFileIds` при создании материала или в `storedFileId` при создании ДЗ.

---

### 9.3 Создание материала к уроку с файлами

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/materials
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "name": "Lecture slides",
  "description": "Slides for week 1",
  "publishedAt": "2025-02-19T12:00:00",
  "storedFileIds": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"]
}
```

**Ответ:** `201 Created`

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "lessonId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Lecture slides",
  "description": "Slides for week 1",
  "authorId": "12345678-1234-1234-1234-123456789abc",
  "publishedAt": "2025-02-19T12:00:00",
  "files": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "size": 1048576,
      "contentType": "application/pdf",
      "originalName": "lecture-1.pdf",
      "uploadedAt": "2025-02-19T12:10:00",
      "uploadedBy": "12345678-1234-1234-1234-123456789abc"
    }
  ]
}
```

---

### 9.4 Создание домашнего задания с файлом

**Запрос:**

```
POST /api/lessons/550e8400-e29b-41d4-a716-446655440000/homework
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Problem set 1",
  "description": "Complete exercises 1–5 from chapter 2",
  "points": 10,
  "storedFileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Ответ:** `201 Created`

```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "lessonId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Problem set 1",
  "description": "Complete exercises 1–5 from chapter 2",
  "points": 10,
  "file": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "size": 1048576,
    "contentType": "application/pdf",
    "originalName": "lecture-1.pdf",
    "uploadedAt": "2025-02-19T12:10:00",
    "uploadedBy": "12345678-1234-1234-1234-123456789abc"
  },
  "createdAt": "2025-02-19T12:15:00",
  "updatedAt": "2025-02-19T12:15:00"
}
```

---

### 9.5 Обновление домашнего задания (снять файл)

**Запрос:**

```
PUT /api/homework/c3d4e5f6-a7b8-9012-cdef-123456789012
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Problem set 1 (updated)",
  "clearFile": true
}
```

**Ответ:** `200 OK` — тело **HomeworkDto** с обновлённым `title` и без привязанного файла (`file`: null или отсутствует).

---

По этому контракту можно реализовать на фронтенде страницу полной информации по уроку для дашборда учителя: отображение данных (через full-details), изменение и удаление урока, загрузку и скачивание файлов, полный цикл работы с материалами к уроку и домашними заданиями.
