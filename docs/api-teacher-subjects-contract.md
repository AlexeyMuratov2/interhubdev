# Контракт API: предметы преподавателя и семестры

Документ-контракт для фронтенда. Описывает полный сценарий: получение списка семестров, списка предметов преподавателя (с фильтром по семестру) и детальной информации по выбранному предмету. Все эндпоинты используют JWT текущего пользователя; для ручек предметов требуется роль преподавателя (по токену определяется teacherId).

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/subjects` | Каталог предметов; эндпоинты «мои предметы» преподавателя |
| `/api/academic` | Учебные годы и семестры (для выбора семестра в UI) |
| `/api/documents` | Загрузка, скачивание и управление файлами (stored files) |
| `/api/offerings` | Материалы к предметам (course materials) по offering |
| `/api/materials` | Материалы к предметам по ID материала |

Хост и порт задаются окружением. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы выполняются с **JWT в cookie** (или заголовок `Authorization: Bearer <JWT>` — в зависимости от настройки приложения). Токен выдаётся при входе.
- **Эндпоинты предметов преподавателя** (`GET /api/subjects/teacher/my`, `GET /api/subjects/teacher/my/{curriculumSubjectId}`): доступны только **аутентифицированному пользователю с профилем преподавателя**. По JWT извлекается `userId`, по нему находится `teacherId`. Если профиль преподавателя отсутствует, возвращается `404` с кодом `SUBJECT_TEACHER_PROFILE_NOT_FOUND`.
- **Эндпоинты семестров** (чтение): доступны любому аутентифицированному пользователю. Создание/изменение/удаление семестров и учебных лет — только роли **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки **ISO-8601**: дата-время `"2025-02-05T12:00:00"`, дата `"2025-02-05"`.
- **Числа** — JSON number; для кредитов и весов может использоваться число с дробной частью.
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `UNAUTHORIZED`, `NOT_FOUND`, `FORBIDDEN`, `SUBJECT_TEACHER_PROFILE_NOT_FOUND`) |
| `message` | string | Человекочитаемое сообщение |
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

Пример (404, нет профиля преподавателя):

```json
{
  "code": "SUBJECT_TEACHER_PROFILE_NOT_FOUND",
  "message": "Преподавательский профиль не найден",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

---

## 2. Workflow: список семестров → список предметов → детали предмета

Рекомендуемый порядок запросов на фронтенде:

1. **Получить список семестров для выбора**  
   - Вариант A: `GET /api/academic/years` → затем `GET /api/academic/years/{academicYearId}/semesters` — по учебному году получить все семестры.  
   - Вариант B: `GET /api/academic/semesters/current` — получить текущий семестр и использовать его данные (в т.ч. `number`) для подстановки в фильтр списка предметов при необходимости.

2. **Получить список предметов преподавателя**  
   - `GET /api/subjects/teacher/my?semesterNo={semesterNo}`  
   - Параметр `semesterNo` опционален. Если передан — возвращаются только предметы учебного плана с этим номером семестра (1, 2, 3, …). Если не передан — возвращаются все предметы, где преподаватель назначен (основной преподаватель офферинга или слот).  
   - В ответе каждый элемент содержит `curriculumSubjectId`, данные предмета (код, китайское/английское название, департамент), семестр, длительность, тип контроля, кредиты и **список групп** (`groups`), которым преподаватель ведёт этот предмет.

3. **Получить детали выбранного предмета**  
   - По `curriculumSubjectId` из списка: `GET /api/subjects/teacher/my/{curriculumSubjectId}`  
   - Возвращается полная информация: данные предмета, данные curriculum_subject по семестру, список контролов (assessments), список реализаций (offerings) по группам с материалами.

4. **Работа с материалами к предмету** (опционально):
   - **Загрузка нового материала:**
     - `POST /api/documents/upload` — загрузить файл, получить `storedFileId`.
     - `POST /api/offerings/{offeringId}/materials` — привязать файл к offering с названием и описанием.
   - **Скачивание материала:**
     - `GET /api/documents/stored/{id}/download` — потоковое скачивание через бэкенд.
     - Или `GET /api/documents/stored/{id}/download-url` — получить presigned URL для прямого скачивания из хранилища.
   - **Удаление материала:**
     - `DELETE /api/materials/{materialId}` — удалить материал (автор или админ).

**Важно:**  
- `semesterNo` в `GET /api/subjects/teacher/my` — это **номер семестра в учебном плане** (1, 2, 3, …), а не UUID семестра из модуля academic. Для отображения в UI можно использовать эндпоинты academic (название семестра, даты); для фильтрации списка предметов передаётся целое число 1..N.  
- Для деталей предмета в path передаётся именно **curriculumSubjectId** (из поля `curriculumSubjectId` элемента списка), а не `subjectId`.
- `offeringId` для привязки материала берётся из поля `offerings[].id` в ответе деталей предмета.
- Файлы проходят проверки безопасности: размер (макс. 50 МБ по умолчанию), тип файла, magic bytes, антивирус.
- Доступ к файлам: владелец файла (`uploadedBy`) или пользователи с ролями **ADMIN**, **MODERATOR**, **SUPER_ADMIN**.

---

## 3. Семестры (Academic)

Эндпоинты для получения учебных лет и семестров. Нужны фронтенду, чтобы показать выбор семестра и при необходимости передать номер семестра в запрос списка предметов.

### 3.1 Модель данных семестра (SemesterDto)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор семестра |
| `academicYearId` | UUID | Id учебного года |
| `number` | integer | Номер семестра в учебном году (1 — осенний, 2 — весенний и т.д.) |
| `name` | string \| null | Название (например, «Осенний семестр») |
| `startDate` | string | Дата начала (ISO-8601, дата) |
| `endDate` | string | Дата окончания (ISO-8601, дата) |
| `examStartDate` | string \| null | Начало экзаменационного периода |
| `examEndDate` | string \| null | Конец экзаменационного периода |
| `weekCount` | integer \| null | Количество недель в семестре |
| `isCurrent` | boolean | Признак текущего семестра |
| `createdAt` | string | Дата и время создания записи (ISO-8601) |

### 3.2 Модель данных учебного года (AcademicYearDto)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор учебного года |
| `name` | string | Название (например, «2024/2025») |
| `startDate` | string | Дата начала (ISO-8601) |
| `endDate` | string | Дата окончания (ISO-8601) |
| `isCurrent` | boolean | Текущий ли учебный год |
| `createdAt` | string | Дата и время создания (ISO-8601) |

### 3.3 GET /api/academic/years — все учебные годы

**Назначение:** получить список учебных лет для выбора года и далее — списка семестров.

**Метод и путь:** `GET /api/academic/years`

**Права доступа:** аутентифицированный пользователь.

**Параметры:** нет.

**Успешный ответ:** `200 OK` — массив объектов **AcademicYearDto**.

**Ошибки:** при отсутствии или невалидном JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

---

### 3.4 GET /api/academic/years/{academicYearId}/semesters — семестры по учебному году

**Назначение:** получить все семестры выбранного учебного года (для выпадающего списка «Семестр» и отображения названий/дат).

**Метод и путь:** `GET /api/academic/years/{academicYearId}/semesters`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `academicYearId` | UUID | Id учебного года |

**Успешный ответ:** `200 OK` — массив объектов **SemesterDto**. Порядок по номеру семестра.

**Ошибки:** при несуществующем `academicYearId` может вернуться пустой массив или ошибка от зависимостей; при отсутствии JWT — `401` (код `UNAUTHORIZED`).

---

### 3.5 GET /api/academic/semesters/current — текущий семестр

**Назначение:** получить текущий семестр (например, для подстановки в UI по умолчанию).

**Метод и путь:** `GET /api/academic/semesters/current`

**Права доступа:** аутентифицированный пользователь.

**Параметры:** нет.

**Успешный ответ:** `200 OK` — объект **SemesterDto**.

**Ошибки:** если текущий семестр не задан — `404 Not Found` (тело может быть пустым или ErrorResponse с кодом `NOT_FOUND`). При отсутствии JWT — `401` (код `UNAUTHORIZED`).

---

### 3.6 GET /api/academic/semesters/{id} — семестр по ID

**Назначение:** получить один семестр по идентификатору (например, после сохранения выбора в URL или кэше).

**Метод и путь:** `GET /api/academic/semesters/{id}`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Идентификатор семестра |

**Успешный ответ:** `200 OK` — объект **SemesterDto**.

**Ошибки:** при отсутствии семестра с указанным `id` — `404 Not Found` (код `NOT_FOUND` в теле при использовании единого формата ошибок). При отсутствии JWT — `401` (код `UNAUTHORIZED`).

---

## 4. Предметы преподавателя (Teacher Subjects)

Эндпоинты возвращают данные только по тем предметам, где **текущий пользователь** назначен преподавателем (основной преподаватель в offering или преподаватель в слоте).

### 4.1 Модель данных: элемент списка (TeacherSubjectListItemDto)

Используется в ответе `GET /api/subjects/teacher/my`.

| Поле | Тип | Описание |
|------|-----|----------|
| `curriculumSubjectId` | UUID | Id предмета в учебном плане (curriculum_subject). Используется для запроса деталей: `GET /api/subjects/teacher/my/{curriculumSubjectId}` |
| `subjectId` | UUID | Id предмета в каталоге (subject) |
| `subjectCode` | string | Код предмета |
| `subjectChineseName` | string \| null | Название на китайском |
| `subjectEnglishName` | string \| null | Название на английском |
| `subjectDescription` | string \| null | Описание предмета |
| `departmentId` | UUID \| null | Id департамента (кафедры) |
| `departmentName` | string \| null | Название департамента |
| `semesterNo` | integer | Номер семестра в учебном плане (1, 2, 3, …) |
| `courseYear` | integer \| null | Курс (год обучения) |
| `durationWeeks` | integer | Длительность в неделях |
| `assessmentTypeId` | UUID | Id типа контроля (экзамен, зачёт и т.д.) |
| `assessmentTypeName` | string \| null | Название типа контроля |
| `credits` | number \| null | Кредиты |
| `groups` | array | Массив объектов **GroupInfoDto** — группы, которым преподаватель ведёт этот предмет |

**GroupInfoDto (элемент массива `groups`):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id группы |
| `code` | string \| null | Код группы |
| `name` | string \| null | Название группы |

### 4.2 GET /api/subjects/teacher/my — список моих предметов

**Назначение:** получить список предметов текущего преподавателя в укороченном виде для списка/карточек. Можно отфильтровать по номеру семестра учебного плана (`semesterNo`).

**Метод и путь:** `GET /api/subjects/teacher/my`

**Права доступа:** аутентифицированный пользователь с **профилем преподавателя**. Иначе — `404 SUBJECT_TEACHER_PROFILE_NOT_FOUND`.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `semesterNo` | integer | нет | Номер семестра в учебном плане (1, 2, 3, …). Если передан — возвращаются только предметы с этим `semesterNo`. Если не передан — все предметы преподавателя. |

**Успешный ответ:** `200 OK` — массив объектов **TeacherSubjectListItemDto**. Порядок: по номеру семестра, затем по коду предмета. Если у преподавателя нет назначений — пустой массив `[]`.

**Логика:**  
- По JWT определяется `userId`, по нему — `teacherId`.  
- Собираются все offerings, где преподаватель назначен (основной преподаватель офферинга или слот).  
- По каждому уникальному `curriculumSubjectId` формируется один элемент списка; в него подставляются данные предмета (subject), учебного плана (curriculum_subject), типа контроля и список групп из этих offerings.  
- Если передан `semesterNo`, в список попадают только те curriculum_subject, у которых `semesterNo` совпадает.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалидный |
| 404 | SUBJECT_TEACHER_PROFILE_NOT_FOUND | У пользователя нет профиля преподавателя |

---

### 4.3 Модель данных: детали предмета (TeacherSubjectDetailDto)

Используется в ответе `GET /api/subjects/teacher/my/{curriculumSubjectId}`.

| Поле | Тип | Описание |
|------|-----|----------|
| `subject` | object | **SubjectInfoDto** — данные предмета из каталога (общие для всех реализаций) |
| `curriculumSubject` | object | **CurriculumSubjectInfoDto** — предмет в учебном плане (семестр, часы, тип контроля) |
| `assessments` | array | Массив **CurriculumSubjectAssessmentInfoDto** — элементы контроля (экзамен, зачёт по неделям и т.д.) |
| `offerings` | array | Массив **GroupSubjectOfferingInfoDto** — реализации по группам (группа, преподаватель, аудитория, материалы) |

**SubjectInfoDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id предмета |
| `code` | string | Код предмета |
| `chineseName` | string \| null | Название на китайском |
| `englishName` | string \| null | Название на английском |
| `description` | string \| null | Описание |
| `departmentId` | UUID \| null | Id департамента |
| `departmentName` | string \| null | Название департамента |
| `createdAt` | string | Дата создания (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

**CurriculumSubjectInfoDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id curriculum_subject |
| `curriculumId` | UUID | Id учебного плана |
| `subjectId` | UUID | Id предмета в каталоге |
| `semesterNo` | integer | Номер семестра в учебном плане |
| `courseYear` | integer \| null | Курс |
| `durationWeeks` | integer | Длительность в неделях |
| `hoursTotal` | integer \| null | Всего часов |
| `hoursLecture` | integer \| null | Лекции |
| `hoursPractice` | integer \| null | Практика |
| `hoursLab` | integer \| null | Лабораторные |
| `hoursSeminar` | integer \| null | Семинары |
| `hoursSelfStudy` | integer \| null | Самостоятельная работа |
| `hoursConsultation` | integer \| null | Консультации |
| `hoursCourseWork` | integer \| null | Курсовая работа |
| `assessmentTypeId` | UUID | Id типа контроля |
| `assessmentTypeName` | string \| null | Название типа контроля |
| `credits` | number \| null | Кредиты |
| `createdAt` | string | ISO-8601 |
| `updatedAt` | string | ISO-8601 |

**CurriculumSubjectAssessmentInfoDto (элемент `assessments`):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id записи контроля |
| `assessmentTypeId` | UUID | Id типа контроля |
| `assessmentTypeName` | string \| null | Название типа |
| `weekNumber` | integer \| null | Номер недели проведения (null — конец семестра) |
| `isFinal` | boolean | Итоговый ли контроль |
| `weight` | number \| null | Вес в итоговой оценке (0..1) |
| `notes` | string \| null | Примечания |
| `createdAt` | string | ISO-8601 |

**GroupSubjectOfferingInfoDto (элемент `offerings`):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id offering (реализация предмета для группы) |
| `groupId` | UUID | Id группы |
| `groupCode` | string \| null | Код группы |
| `groupName` | string \| null | Название группы |
| `teacherId` | UUID \| null | Id основного преподавателя |
| `roomId` | UUID \| null | Id аудитории по умолчанию |
| `roomName` | string \| null | Название аудитории (например, «Корпус A 101») |
| `format` | string \| null | Формат: offline, online, mixed |
| `notes` | string \| null | Примечания |
| `createdAt` | string | ISO-8601 |
| `updatedAt` | string | ISO-8601 |
| `materials` | array | Массив **CourseMaterialInfoDto** — материалы к этому offering |

**CourseMaterialInfoDto (элемент `materials`):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id материала |
| `title` | string | Название материала |
| `description` | string \| null | Описание |
| `authorId` | UUID | Id пользователя-автора |
| `authorName` | string \| null | Отображаемое имя автора |
| `uploadedAt` | string | Дата загрузки (ISO-8601) |
| `file` | object | **StoredFileInfoDto** — метаданные файла |

**StoredFileInfoDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла в хранилище |
| `originalName` | string | Имя файла |
| `contentType` | string | MIME-тип |
| `size` | integer | Размер в байтах |
| `uploadedAt` | string | ISO-8601 |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 4.4 GET /api/subjects/teacher/my/{curriculumSubjectId} — детали предмета

**Назначение:** получить полную информацию по одному предмету учебного плана: данные предмета, данные curriculum_subject по семестру, список контролов (assessments) и все реализации (offerings) по группам с материалами. Доступ только если текущий преподаватель назначен на этот curriculum_subject (хотя бы в одном offering).

**Метод и путь:** `GET /api/subjects/teacher/my/{curriculumSubjectId}`

**Права доступа:** аутентифицированный пользователь с профилем преподавателя, причём этот преподаватель должен быть назначен на данный предмет (в любом из групп).

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `curriculumSubjectId` | UUID | Id предмета в учебном плане (берётся из поля `curriculumSubjectId` элемента списка `GET /api/subjects/teacher/my`) |

**Успешный ответ:** `200 OK` — объект **TeacherSubjectDetailDto** (поля `subject`, `curriculumSubject`, `assessments`, `offerings`).

**Логика:**  
- Проверяется существование curriculum_subject.  
- Проверяется, что текущий преподаватель имеет хотя бы один offering по этому curriculum_subject.  
- Возвращаются: данные subject (каталог), curriculum_subject (семестр, часы, тип контроля), все assessments по этому curriculum_subject, все offerings преподавателя по этому curriculum_subject с подгруженными материалами (доступ к материалам — по правилам доступа к offering).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT или токен невалидный |
| 404 | SUBJECT_TEACHER_PROFILE_NOT_FOUND | У пользователя нет профиля преподавателя |
| 404 | SUBJECT_CURRICULUM_SUBJECT_NOT_FOUND | Нет curriculum_subject с таким id |
| 403 | SUBJECT_ACCESS_DENIED | Curriculum subject найден, но текущий преподаватель не назначен ни на один offering по этому предмету |

---

## 5. Работа с файлами и материалами к предмету

Материалы к предмету (course materials) — это файлы, привязанные к конкретной реализации предмета для группы (offering). Каждый преподаватель может иметь свои материалы для одного и того же предмета в разных группах.

**Workflow загрузки материала:**

1. **Загрузить файл** → `POST /api/documents/upload` — получить `storedFileId`.
2. **Привязать файл к offering** → `POST /api/offerings/{offeringId}/materials` — создать материал с названием и описанием.

**Workflow скачивания:**

- **Потоковое скачивание через бэкенд**: `GET /api/documents/stored/{id}/download` — сервер возвращает файл как поток.
- **Прямое скачивание через presigned URL**: `GET /api/documents/stored/{id}/download-url` — получить временную ссылку для прямого скачивания из хранилища (S3/MinIO).

**Важно:**  
- `offeringId` для привязки материала берётся из поля `offerings[].id` в ответе `GET /api/subjects/teacher/my/{curriculumSubjectId}`.
- Доступ к файлам: владелец файла (`uploadedBy`) или пользователи с ролями **ADMIN**, **MODERATOR**, **SUPER_ADMIN**.
- Удаление материала: автор материала или администратор/модератор.

### 5.1 Модель данных: материал (CourseMaterialDto)

Используется в ответах эндпоинтов материалов.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id материала |
| `offeringId` | UUID | Id offering (реализация предмета для группы) |
| `title` | string | Название материала (до 500 символов) |
| `description` | string \| null | Описание (до 2000 символов) |
| `authorId` | UUID | Id пользователя-автора |
| `uploadedAt` | string | Дата и время загрузки (ISO-8601) |
| `file` | object | **StoredFileDto** — метаданные файла |

**StoredFileDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id файла в хранилище (используется для скачивания) |
| `size` | integer | Размер в байтах |
| `contentType` | string | MIME-тип (например, `application/pdf`, `image/png`) |
| `originalName` | string | Исходное имя файла |
| `uploadedAt` | string | Дата загрузки (ISO-8601) |
| `uploadedBy` | UUID | Id пользователя, загрузившего файл |

### 5.2 POST /api/documents/upload — загрузка файла

**Назначение:** загрузить файл в хранилище и получить метаданные (`storedFileId`). Файл проходит проверки безопасности (размер, тип, антивирус). После успешной загрузки файл нужно привязать к offering через `POST /api/offerings/{offeringId}/materials`.

**Метод и путь:** `POST /api/documents/upload`

**Content-Type:** `multipart/form-data`

**Права доступа:** аутентифицированный пользователь.

**Параметры формы:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `file` | file (binary) | да | Файл для загрузки |

**Ограничения:**
- Максимальный размер файла: по умолчанию 50 МБ (настраивается через `app.document.max-file-size-bytes`).
- Разрешённые типы файлов: определяются политикой `AllowedFileTypesPolicy` (обычно документы: PDF, DOCX, PPTX, изображения: PNG, JPG, и т.д.).
- Файл проверяется на вирусы (если настроен антивирус).
- Проверка magic bytes: содержимое файла должно соответствовать заявленному MIME-типу.

**Успешный ответ:** `201 Created` — объект **StoredFileDto**.

**Логика:**  
- Файл сохраняется во временный файл, проверяется безопасность (размер, тип, magic bytes, антивирус).
- Файл загружается в хранилище (MinIO/S3).
- Создаётся запись в БД с метаданными.
- При ошибке сохранения в БД файл удаляется из хранилища (атомарность).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Пустой файл (`file` отсутствует или размер 0) |
| 400 | UPLOAD_EMPTY_FILE | Файл пустой (размер <= 0) |
| 400 | UPLOAD_FILE_TOO_LARGE | Размер файла превышает максимум (обычно 50 МБ) |
| 400 | UPLOAD_FORBIDDEN_FILE_TYPE | Тип файла не разрешён (MIME или расширение) |
| 400 | UPLOAD_SUSPICIOUS_FILENAME | Подозрительное имя файла (path traversal, двойное расширение и т.д.) |
| 400 | UPLOAD_EXTENSION_MISMATCH | Расширение не соответствует MIME-типу |
| 400 | UPLOAD_CONTENT_TYPE_MISMATCH | Magic bytes не соответствуют заявленному MIME-типу |
| 400 | UPLOAD_MALWARE_DETECTED | Антивирус обнаружил вредоносное ПО |
| 401 | UNAUTHORIZED | Нет JWT или токен невалидный |
| 503 | UPLOAD_AV_UNAVAILABLE | Антивирус недоступен (fail-closed политика) |
| 500 | UPLOAD_FAILED | Ошибка загрузки в хранилище |
| 500 | SAVE_FAILED | Ошибка сохранения метаданных в БД |

---

### 5.3 POST /api/offerings/{offeringId}/materials — добавить материал к предмету

**Назначение:** привязать уже загруженный файл (по `storedFileId`) к offering как материал курса. Создаётся запись материала с названием и описанием.

**Метод и путь:** `POST /api/offerings/{offeringId}/materials`

**Content-Type:** `application/json`

**Права доступа:** роль **TEACHER** или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `offeringId` | UUID | Id offering (берётся из `offerings[].id` в деталях предмета) |

**Тело запроса (AddCourseMaterialRequest):**

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `storedFileId` | UUID | да | Id файла из `POST /api/documents/upload` |
| `title` | string | да | Название материала (до 500 символов) |
| `description` | string | нет | Описание (до 2000 символов) |

**Успешный ответ:** `201 Created` — объект **CourseMaterialDto**.

**Логика:**  
- Проверяется существование offering и stored file.
- Проверяется, что файл ещё не привязан к этому offering (уникальность `offeringId` + `storedFileId`).
- Создаётся запись материала с текущим пользователем как автором.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Невалидные данные (пустой `title`, превышение длины) |
| 400 | VALIDATION_FAILED | Ошибка валидации (в `details` — поля и сообщения) |
| 401 | UNAUTHORIZED | Нет JWT или токен невалидный |
| 403 | COURSE_MATERIAL_CREATE_PERMISSION_DENIED | Пользователь не TEACHER и не ADMIN/MODERATOR |
| 404 | COURSE_MATERIAL_OFFERING_NOT_FOUND | Offering не найден |
| 404 | STORED_FILE_NOT_FOUND | Файл с таким `storedFileId` не найден |
| 409 | COURSE_MATERIAL_ALREADY_EXISTS | Файл уже привязан к этому offering |
| 422 | COURSE_MATERIAL_SAVE_FAILED | Ошибка сохранения (например, constraint violation) |

---

### 5.4 GET /api/offerings/{offeringId}/materials — список материалов по offering

**Назначение:** получить все материалы для конкретного offering (реализации предмета для группы). Используется для отображения списка материалов на странице деталей предмета.

**Метод и путь:** `GET /api/offerings/{offeringId}/materials`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `offeringId` | UUID | Id offering |

**Успешный ответ:** `200 OK` — массив объектов **CourseMaterialDto**. Порядок: по `uploadedAt` (от новых к старым).

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | COURSE_MATERIAL_OFFERING_NOT_FOUND | Offering не найден |

**Примечание:** Этот эндпоинт дублирует данные из `GET /api/subjects/teacher/my/{curriculumSubjectId}` (поле `offerings[].materials`). Используйте его, если нужно обновить список материалов без повторного запроса деталей предмета.

---

### 5.5 GET /api/materials/{materialId} — получить материал по ID

**Назначение:** получить один материал по его идентификатору (например, для страницы просмотра материала или перед удалением).

**Метод и путь:** `GET /api/materials/{materialId}`

**Права доступа:** аутентифицированный пользователь.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `materialId` | UUID | Id материала |

**Успешный ответ:** `200 OK` — объект **CourseMaterialDto**.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 404 | COURSE_MATERIAL_NOT_FOUND | Материал не найден |

---

### 5.6 GET /api/documents/stored/{id}/download — скачать файл (поток)

**Назначение:** скачать файл через бэкенд (сервер возвращает файл как поток). Используется для прямого скачивания в браузере или мобильном приложении.

**Метод и путь:** `GET /api/documents/stored/{id}/download`

**Права доступа:** владелец файла (`uploadedBy` == текущий пользователь) или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id файла (`file.id` из материала) |

**Успешный ответ:** `200 OK` — бинарный поток файла.

**Заголовки ответа:**
- `Content-Type`: MIME-тип файла (из метаданных)
- `Content-Disposition`: `attachment; filename*=UTF-8''<encoded_filename>` — имя файла для сохранения

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | ACCESS_DENIED | Нет прав доступа (не владелец и не админ) |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 404 | FILE_NOT_IN_STORAGE | Метаданные есть, но файл отсутствует в хранилище |

---

### 5.7 GET /api/documents/stored/{id}/download-url — получить presigned URL для скачивания

**Назначение:** получить временную ссылку (presigned URL) для прямого скачивания файла из хранилища (S3/MinIO), минуя бэкенд. Ссылка действительна ограниченное время.

**Метод и путь:** `GET /api/documents/stored/{id}/download-url`

**Права доступа:** владелец файла или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id файла |

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `expires` | integer | нет | Время жизни ссылки в секундах. По умолчанию 3600 (1 час). Максимум зависит от настроек хранилища. |

**Успешный ответ:** `200 OK` — объект с полем `url`:

```json
{
  "url": "https://storage.example.com/bucket/file.pdf?X-Amz-Algorithm=..."
}
```

**Ошибки:** те же, что для `GET /api/documents/stored/{id}/download`.

**Использование:**  
- Получить URL через этот эндпоинт.
- Открыть URL в браузере или использовать для прямого скачивания (например, через `<a href>` с `download` атрибутом или `window.open()`).

---

### 5.8 GET /api/documents/stored/{id}/preview — получить presigned URL для preview

**Назначение:** получить временную ссылку для просмотра файла (preview) в браузере. Аналогично `download-url`, но может использоваться для встраивания в `<iframe>` или `<img>` (для изображений).

**Метод и путь:** `GET /api/documents/stored/{id}/preview`

**Права доступа:** владелец файла или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры:** те же, что для `GET /api/documents/stored/{id}/download-url`.

**Успешный ответ:** `200 OK` — объект с полем `url` (presigned URL).

**Ошибки:** те же, что для `GET /api/documents/stored/{id}/download`.

---

### 5.9 DELETE /api/materials/{materialId} — удалить материал

**Назначение:** удалить материал (запись материала). Если файл не используется другими материалами, он также удаляется из хранилища.

**Метод и путь:** `DELETE /api/materials/{materialId}`

**Права доступа:** автор материала (`authorId` == текущий пользователь) или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `materialId` | UUID | Id материала |

**Успешный ответ:** `204 No Content` (тело отсутствует).

**Логика:**  
- Удаляется запись материала.
- Если файл (`storedFileId`) не используется другими материалами, он удаляется из хранилища и БД.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | COURSE_MATERIAL_DELETE_PERMISSION_DENIED | Нет прав (не автор и не админ) |
| 404 | COURSE_MATERIAL_NOT_FOUND | Материал не найден |

---

### 5.10 DELETE /api/documents/stored/{id} — удалить файл

**Назначение:** удалить файл из хранилища и метаданные из БД. Можно удалить только если файл не используется материалами (или другими сущностями, ссылающимися на stored file).

**Метод и путь:** `DELETE /api/documents/stored/{id}`

**Права доступа:** владелец файла или **ADMIN/MODERATOR/SUPER_ADMIN**.

**Параметры пути:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| `id` | UUID | Id файла |

**Успешный ответ:** `204 No Content`.

**Ошибки:**

| HTTP | code | Когда возникает |
|------|------|------------------|
| 401 | UNAUTHORIZED | Нет JWT |
| 403 | ACCESS_DENIED | Нет прав доступа |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 409 | FILE_IN_USE | Файл используется (привязан к материалу или другой сущности) |

**Примечание:** Обычно файлы удаляются автоматически при удалении материала (`DELETE /api/materials/{materialId}`), если файл не используется. Прямое удаление файла используется редко (например, при отмене загрузки до привязки к материалу).

---

## 6. Сводная таблица ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Невалидные параметры (если применимо) |
| 400 | UPLOAD_EMPTY_FILE | Пустой файл при загрузке |
| 400 | UPLOAD_FILE_TOO_LARGE | Размер файла превышает максимум |
| 400 | UPLOAD_FORBIDDEN_FILE_TYPE | Тип файла не разрешён |
| 400 | UPLOAD_SUSPICIOUS_FILENAME | Подозрительное имя файла |
| 400 | UPLOAD_EXTENSION_MISMATCH | Расширение не соответствует MIME |
| 400 | UPLOAD_CONTENT_TYPE_MISMATCH | Magic bytes не соответствуют MIME |
| 400 | UPLOAD_MALWARE_DETECTED | Антивирус обнаружил вредоносное ПО |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Недостаточно прав (общая ошибка доступа) |
| 403 | SUBJECT_ACCESS_DENIED | Преподаватель не назначен на данный предмет |
| 403 | COURSE_MATERIAL_CREATE_PERMISSION_DENIED | Нет прав на создание материала (не TEACHER/ADMIN) |
| 403 | COURSE_MATERIAL_DELETE_PERMISSION_DENIED | Нет прав на удаление материала (не автор/админ) |
| 403 | ACCESS_DENIED | Нет прав доступа к файлу |
| 404 | NOT_FOUND | Ресурс не найден (семестр, учебный год и т.д.) |
| 404 | SUBJECT_TEACHER_PROFILE_NOT_FOUND | У пользователя нет профиля преподавателя |
| 404 | SUBJECT_CURRICULUM_SUBJECT_NOT_FOUND | Curriculum subject с указанным id не найден |
| 404 | STORED_FILE_NOT_FOUND | Файл не найден |
| 404 | FILE_NOT_IN_STORAGE | Файл отсутствует в хранилище |
| 404 | COURSE_MATERIAL_NOT_FOUND | Материал не найден |
| 404 | COURSE_MATERIAL_OFFERING_NOT_FOUND | Offering не найден |
| 409 | COURSE_MATERIAL_ALREADY_EXISTS | Файл уже привязан к этому offering |
| 409 | FILE_IN_USE | Файл используется и не может быть удалён |
| 413 | PAYLOAD_TOO_LARGE | Размер файла превышает максимум (альтернативный код для больших файлов) |
| 422 | COURSE_MATERIAL_SAVE_FAILED | Ошибка сохранения материала |
| 500 | UPLOAD_FAILED | Ошибка загрузки в хранилище |
| 500 | SAVE_FAILED | Ошибка сохранения метаданных |
| 503 | UPLOAD_AV_UNAVAILABLE | Антивирус недоступен |
| 503 | STORAGE_UNAVAILABLE | Хранилище временно недоступно |

---

## 7. Примеры запросов и ответов

### 7.1 Получение семестров учебного года

**Запрос:**  
`GET /api/academic/years`  
`GET /api/academic/years/550e8400-e29b-41d4-a716-446655440000/semesters`

**Ответ (фрагмент списка семестров):** `200 OK`

```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "academicYearId": "550e8400-e29b-41d4-a716-446655440000",
    "number": 1,
    "name": "Осенний семестр",
    "startDate": "2025-09-01",
    "endDate": "2026-01-15",
    "examStartDate": "2026-01-10",
    "examEndDate": "2026-01-25",
    "weekCount": 18,
    "isCurrent": true,
    "createdAt": "2025-02-01T09:00:00"
  }
]
```

### 7.2 Список предметов преподавателя без фильтра

**Запрос:**  
`GET /api/subjects/teacher/my`

**Ответ:** `200 OK`

```json
[
  {
    "curriculumSubjectId": "770e8400-e29b-41d4-a716-446655440002",
    "subjectId": "880e8400-e29b-41d4-a716-446655440003",
    "subjectCode": "MATH-101",
    "subjectChineseName": "高等数学",
    "subjectEnglishName": "Higher Mathematics",
    "subjectDescription": null,
    "departmentId": "990e8400-e29b-41d4-a716-446655440004",
    "departmentName": "Кафедра математики",
    "semesterNo": 1,
    "courseYear": 1,
    "durationWeeks": 16,
    "assessmentTypeId": "aa0e8400-e29b-41d4-a716-446655440005",
    "assessmentTypeName": "Exam",
    "credits": 4.0,
    "groups": [
      { "id": "bb0e8400-e29b-41d4-a716-446655440006", "code": "CS-2024-1", "name": "Группа 1" },
      { "id": "cc0e8400-e29b-41d4-a716-446655440007", "code": "CS-2024-2", "name": "Группа 2" }
    ]
  }
]
```

### 7.3 Список предметов с фильтром по семестру

**Запрос:**  
`GET /api/subjects/teacher/my?semesterNo=1`

**Ответ:** `200 OK` — массив **TeacherSubjectListItemDto**, только с `semesterNo === 1`.

### 7.4 Детали предмета

**Запрос:**  
`GET /api/subjects/teacher/my/770e8400-e29b-41d4-a716-446655440002`

**Ответ (сокращённо):** `200 OK`

```json
{
  "subject": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "code": "MATH-101",
    "chineseName": "高等数学",
    "englishName": "Higher Mathematics",
    "description": null,
    "departmentId": "990e8400-e29b-41d4-a716-446655440004",
    "departmentName": "Кафедра математики",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "curriculumSubject": {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "curriculumId": "dd0e8400-e29b-41d4-a716-446655440008",
    "subjectId": "880e8400-e29b-41d4-a716-446655440003",
    "semesterNo": 1,
    "courseYear": 1,
    "durationWeeks": 16,
    "hoursTotal": 64,
    "hoursLecture": 32,
    "hoursPractice": 32,
    "hoursLab": null,
    "hoursSeminar": null,
    "hoursSelfStudy": null,
    "hoursConsultation": null,
    "hoursCourseWork": null,
    "assessmentTypeId": "aa0e8400-e29b-41d4-a716-446655440005",
    "assessmentTypeName": "Exam",
    "credits": 4.0,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "assessments": [
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440009",
      "assessmentTypeId": "aa0e8400-e29b-41d4-a716-446655440005",
      "assessmentTypeName": "Exam",
      "weekNumber": null,
      "isFinal": true,
      "weight": 1.0,
      "notes": null,
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "offerings": [
    {
      "id": "ff0e8400-e29b-41d4-a716-446655440010",
      "groupId": "bb0e8400-e29b-41d4-a716-446655440006",
      "groupCode": "CS-2024-1",
      "groupName": "Группа 1",
      "teacherId": "100e8400-e29b-41d4-a716-446655440011",
      "roomId": "110e8400-e29b-41d4-a716-446655440012",
      "roomName": "Корпус A 101",
      "format": "offline",
      "notes": null,
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-01T00:00:00",
      "materials": [
        {
          "id": "120e8400-e29b-41d4-a716-446655440013",
          "title": "Лекция 1",
          "description": null,
          "authorId": "130e8400-e29b-41d4-a716-446655440014",
          "authorName": "Иван Иванов",
          "uploadedAt": "2025-02-01T10:00:00",
          "file": {
            "id": "140e8400-e29b-41d4-a716-446655440015",
            "originalName": "lecture1.pdf",
            "contentType": "application/pdf",
            "size": 102400,
            "uploadedAt": "2025-02-01T10:00:00",
            "uploadedBy": "130e8400-e29b-41d4-a716-446655440014"
          }
        }
      ]
    }
  ]
}
```

### 7.5 Загрузка файла и создание материала

**Шаг 1: Загрузка файла**

**Запрос:**  
`POST /api/documents/upload`  
`Content-Type: multipart/form-data`

**Тело (form-data):**
```
file: [бинарные данные файла, например lecture1.pdf]
```

**Ответ:** `201 Created`

```json
{
  "id": "140e8400-e29b-41d4-a716-446655440015",
  "size": 102400,
  "contentType": "application/pdf",
  "originalName": "lecture1.pdf",
  "uploadedAt": "2025-02-18T10:00:00",
  "uploadedBy": "130e8400-e29b-41d4-a716-446655440014"
}
```

**Шаг 2: Привязка файла к offering**

**Запрос:**  
`POST /api/offerings/ff0e8400-e29b-41d4-a716-446655440010/materials`  
`Content-Type: application/json`

**Тело:**
```json
{
  "storedFileId": "140e8400-e29b-41d4-a716-446655440015",
  "title": "Лекция 1: Введение",
  "description": "Первая лекция курса"
}
```

**Ответ:** `201 Created`

```json
{
  "id": "120e8400-e29b-41d4-a716-446655440013",
  "offeringId": "ff0e8400-e29b-41d4-a716-446655440010",
  "title": "Лекция 1: Введение",
  "description": "Первая лекция курса",
  "authorId": "130e8400-e29b-41d4-a716-446655440014",
  "uploadedAt": "2025-02-18T10:05:00",
  "file": {
    "id": "140e8400-e29b-41d4-a716-446655440015",
    "size": 102400,
    "contentType": "application/pdf",
    "originalName": "lecture1.pdf",
    "uploadedAt": "2025-02-18T10:00:00",
    "uploadedBy": "130e8400-e29b-41d4-a716-446655440014"
  }
}
```

### 7.6 Скачивание файла через presigned URL

**Запрос:**  
`GET /api/documents/stored/140e8400-e29b-41d4-a716-446655440015/download-url?expires=3600`

**Ответ:** `200 OK`

```json
{
  "url": "https://storage.example.com/bucket/files/140e8400-e29b-41d4-a716-446655440015.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=..."
}
```

**Использование на фронтенде:**  
```javascript
// Получить URL
const response = await fetch('/api/documents/stored/{id}/download-url');
const { url } = await response.json();

// Открыть в новой вкладке для скачивания
window.open(url, '_blank');

// Или создать ссылку для скачивания
const link = document.createElement('a');
link.href = url;
link.download = 'lecture1.pdf';
link.click();
```

### 7.7 Ошибка: нет профиля преподавателя

**Запрос:**  
`GET /api/subjects/teacher/my` (пользователь без роли преподавателя / без профиля)

**Ответ:** `404 Not Found`

```json
{
  "code": "SUBJECT_TEACHER_PROFILE_NOT_FOUND",
  "message": "Преподавательский профиль не найден",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.8 Ошибка: нет доступа к предмету

**Запрос:**  
`GET /api/subjects/teacher/my/770e8400-e29b-41d4-a716-446655440002` (преподаватель не назначен на этот curriculum subject)

**Ответ:** `403 Forbidden`

```json
{
  "code": "SUBJECT_ACCESS_DENIED",
  "message": "Нет доступа к этому предмету",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.9 Ошибка загрузки: файл слишком большой

**Запрос:**  
`POST /api/documents/upload` (файл > 50 МБ)

**Ответ:** `413 Payload Too Large` или `400 Bad Request`

```json
{
  "code": "UPLOAD_FILE_TOO_LARGE",
  "message": "File size exceeds maximum allowed size of 50 MB",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.10 Ошибка загрузки: запрещённый тип файла

**Запрос:**  
`POST /api/documents/upload` (например, `.exe` файл)

**Ответ:** `400 Bad Request`

```json
{
  "code": "UPLOAD_FORBIDDEN_FILE_TYPE",
  "message": "File type not allowed: application/x-msdownload",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.11 Ошибка загрузки: обнаружен вирус

**Запрос:**  
`POST /api/documents/upload` (файл с вредоносным содержимым)

**Ответ:** `400 Bad Request`

```json
{
  "code": "UPLOAD_MALWARE_DETECTED",
  "message": "File rejected",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.12 Ошибка: материал уже существует

**Запрос:**  
`POST /api/offerings/ff0e8400-e29b-41d4-a716-446655440010/materials` (тот же файл уже привязан к этому offering)

**Ответ:** `409 Conflict`

```json
{
  "code": "COURSE_MATERIAL_ALREADY_EXISTS",
  "message": "Course material with this file already exists for offering: ff0e8400-e29b-41d4-a716-446655440010",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```

### 7.13 Ошибка: нет прав на удаление материала

**Запрос:**  
`DELETE /api/materials/120e8400-e29b-41d4-a716-446655440013` (пользователь не автор и не админ)

**Ответ:** `403 Forbidden`

```json
{
  "code": "COURSE_MATERIAL_DELETE_PERMISSION_DENIED",
  "message": "You don't have permission to delete this course material",
  "timestamp": "2025-02-18T10:00:00.000Z",
  "details": null
}
```
