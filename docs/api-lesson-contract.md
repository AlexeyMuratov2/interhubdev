# Контракт API: уроки расписания

Документ-контракт для фронтенда по эндпоинтам модуля **Schedule**, связанным с сущностью **урока** (lesson). Описаны все эндпоинты для получения, создания, обновления и удаления уроков, структуры запросов и ответов, а также возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/schedule` | Расписание: здания, комнаты, таймслоты, **уроки** |

Хост и порт задаются окружением развёртывания.

### 1.2 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- Эндпоинты, которые **изменяют данные** (POST, PUT, DELETE), доступны только пользователям с одной из ролей: **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.
- Эндпоинты только для чтения (GET) доступны **любой аутентифицированной роли** (JWT обязателен).
- У каждого эндпоинта в описании указано поле **Роли:** — кто может вызывать ручку. При отсутствии прав возвращается `403 Forbidden` (код `FORBIDDEN`), при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID.
- **Даты и время** — строки **ISO-8601**: дата-время `"2025-02-05T12:00:00"`, дата `"2025-02-05"`, время `"09:00:00"` (формат `HH:mm:ss` для времени в ответах).
- **Числа** — JSON number; для целых (день недели, количество недель) — integer.
- **null** — явное отсутствие значения.

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки (например, `BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`, `SCHEDULE_LESSON_NOT_FOUND`, `SCHEDULE_OFFERING_NOT_FOUND`) |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для валидации — объект «имя поля → сообщение» |

Пример ответа с ошибкой (404):

```json
{
  "code": "SCHEDULE_LESSON_NOT_FOUND",
  "message": "Lesson not found: 550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных

### 2.1 LessonDto

Основная модель урока. Урок владеет датой и временем (startTime, endTime). timeslotId опционален (подсказка UI при создании из таймслота). offeringSlotId ссылается на слот офферинга, из которого был сгенерирован урок (для типа занятия и преподавателя на UI).

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор урока |
| `offeringId` | UUID | Id офферинга (группа + предмет учебного плана) |
| `offeringSlotId` | UUID \| null | Id слота офферинга, из которого был сгенерирован урок |
| `date` | string | Дата урока (ISO-8601, формат `yyyy-MM-dd`) |
| `startTime` | string | Время начала (формат `HH:mm:ss`) |
| `endTime` | string | Время окончания (формат `HH:mm:ss`) |
| `timeslotId` | UUID \| null | Id таймслота-шаблона (опционально, подсказка UI) |
| `roomId` | UUID \| null | Id комнаты |
| `topic` | string \| null | Тема урока |
| `status` | string \| null | Статус: `PLANNED`, `CANCELLED`, `DONE` (по умолчанию `PLANNED`) |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время обновления (ISO-8601) |

### 2.2 LessonForScheduleDto

Урок с полным контекстом для отображения в расписании: сам урок, данные офферинга, слота, преподавателей, комнаты, основного преподавателя, названия предмета и группы. Используется в эндпоинтах получения уроков по дате/неделе для UI расписания.

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object (**LessonDto**) | Данные урока |
| `offering` | object (**OfferingSummaryDto**) | Краткие данные офферинга (id, groupId, curriculumSubjectId, teacherId) |
| `slot` | object (**SlotSummaryDto**) \| null | Краткие данные слота (id, dayOfWeek, startTime, endTime, lessonType, roomId, teacherId) |
| `teachers` | array of **TeacherRoleDto** | Список преподавателей с ролями (teacherId, role: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` или `null` для основного) |
| `room` | object (**RoomSummaryDto**) \| null | Краткие данные комнаты (id, number, buildingName) |
| `mainTeacher` | object (**TeacherSummaryDto**) \| null | Основной преподаватель (id, displayName) |
| `subjectName` | string \| null | Название предмета |
| `group` | object (**GroupSummaryDto**) \| null | Краткие данные группы (id, code, name) |

### 2.3 Вспомогательные DTO

**OfferingSummaryDto:**
- `id` (UUID)
- `groupId` (UUID)
- `curriculumSubjectId` (UUID)
- `teacherId` (UUID \| null)

**SlotSummaryDto:**
- `id` (UUID)
- `offeringId` (UUID)
- `dayOfWeek` (integer, 1–7, понедельник=1)
- `startTime` (string, `HH:mm:ss`)
- `endTime` (string, `HH:mm:ss`)
- `timeslotId` (UUID \| null)
- `lessonType` (string: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR`)
- `roomId` (UUID \| null)
- `teacherId` (UUID \| null)
- `createdAt` (string, ISO-8601)

**TeacherRoleDto:**
- `teacherId` (UUID)
- `role` (string \| null: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` или `null`)

**RoomSummaryDto:**
- `id` (UUID)
- `number` (string)
- `buildingName` (string)

**TeacherSummaryDto:**
- `id` (UUID)
- `displayName` (string)

**GroupSummaryDto:**
- `id` (UUID)
- `code` (string)
- `name` (string)

---

## 3. Получение уроков

### 3.1 GET /api/schedule/lessons

**Назначение:** получить все уроки на указанную дату с полным контекстом для отображения в расписании (офферинг, слот, преподаватели, комната, группа). Удобно для экрана «Расписание на день».

**Метод и путь:** `GET /api/schedule/lessons`

**Роли:** любая аутентифицированная роль (JWT обязателен).

**Query-параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `date` | string | обязательно | Дата в формате ISO-8601 (`yyyy-MM-dd`, например `2025-02-07`) |

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto**. Упорядочены по времени начала (`startTime`).

**Ошибки:** `400 Bad Request` — не указан параметр `date` или неверный формат даты (код `BAD_REQUEST`).

---

### 3.2 GET /api/schedule/lessons/week

**Назначение:** получить все уроки на неделю (ISO-неделя: понедельник–воскресенье), содержащую указанную дату, с полным контекстом для отображения в расписании. Удобно для экрана «Расписание на неделю».

**Метод и путь:** `GET /api/schedule/lessons/week`

**Роли:** любая аутентифицированная роль.

**Query-параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `date` | string | обязательно | Любая дата в неделе (ISO-8601, `yyyy-MM-dd`). По ней вычисляются границы недели (понедельник–воскресенье). |

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto**. Упорядочены по дате, затем по времени начала. Загружаются батчами, без N+1.

**Ошибки:** `400 Bad Request` — не указан параметр `date` или неверный формат даты.

---

### 3.3 GET /api/schedule/lessons/week/group/{groupId}

**Назначение:** получить уроки на неделю (ISO-неделя: понедельник–воскресенье) для указанной группы. Возвращаются только уроки по офферингам этой группы. Удобно для экрана «Расписание группы на неделю».

**Метод и путь:** `GET /api/schedule/lessons/week/group/{groupId}`

**Роли:** любая аутентифицированная роль.

**Параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `groupId` | UUID | да (path) | Id группы |
| `date` | string | да (query) | Любая дата в неделе (ISO-8601, `yyyy-MM-dd`) |

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto**. Упорядочены по дате, затем по времени начала. Если у группы нет офферингов или нет уроков в этой неделе, возвращается пустой массив. Загружаются батчами, без N+1.

**Ошибки:**  
- `400 Bad Request` — не указан параметр `date` или неверный формат даты.  
- `404 Not Found` — группа не найдена (код `SCHEDULE_GROUP_NOT_FOUND`).

---

### 3.4 GET /api/schedule/lessons/week/teacher

**Назначение:** получить уроки на неделю (ISO-неделя: понедельник–воскресенье) для текущего аутентифицированного преподавателя. Возвращаются уроки по всем офферингам, где преподаватель назначен (основной преподаватель офферинга или преподаватель слота). Удобно для экрана «Моё расписание на неделю».

**Метод и путь:** `GET /api/schedule/lessons/week/teacher`

**Роли:** любая аутентифицированная роль (JWT обязателен). Пользователь должен иметь профиль преподавателя.

**Query-параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `date` | string | обязательно | Любая дата в неделе (ISO-8601, `yyyy-MM-dd`) |

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto**. Упорядочены по дате, затем по времени начала. Если у преподавателя нет офферингов или нет уроков в этой неделе, возвращается пустой массив. Загружаются батчами, без N+1.

**Ошибки:**  
- `400 Bad Request` — не указан параметр `date` или неверный формат даты.  
- `401 Unauthorized` — не передан или невалидный JWT (код `UNAUTHORIZED`).  
- `403 Forbidden` — у пользователя нет профиля преподавателя (код `SCHEDULE_TEACHER_PROFILE_NOT_FOUND`).

---

### 3.5 GET /api/schedule/lessons/offering/{offeringId}

**Назначение:** получить все уроки по указанному офферингу. Удобно для отображения всех уроков по предмету группы или для проверки сгенерированных уроков после генерации.

**Метод и путь:** `GET /api/schedule/lessons/offering/{offeringId}`

**Роли:** любая аутентифицированная роль.

**Параметр пути:** `offeringId` — UUID офферинга.

**Успешный ответ:** `200 OK` — массив объектов **LessonDto**. Упорядочены по дате, затем по времени начала.

**Ошибки:** при несуществующем `offeringId` возвращается пустой массив (уроки не найдены).

---

### 3.6 GET /api/schedule/lessons/group/{groupId}

**Назначение:** получить уроки на указанную дату для указанной группы с полным контекстом для отображения в расписании. Возвращаются уроки по всем офферингам группы на эту дату.

**Метод и путь:** `GET /api/schedule/lessons/group/{groupId}`

**Роли:** любая аутентифицированная роль.

**Параметры:**

| Параметр | Тип | Обязательность | Описание |
|----------|-----|----------------|----------|
| `groupId` | UUID | да (path) | Id группы |
| `date` | string | да (query) | Дата (ISO-8601, `yyyy-MM-dd`) |

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto**. Упорядочены по времени начала. Если у группы нет офферингов или нет уроков на эту дату, возвращается пустой массив. Загружаются батчами, без N+1.

**Ошибки:** `400 Bad Request` — не указан параметр `date` или неверный формат даты.

---

### 3.7 GET /api/schedule/lessons/{id}

**Назначение:** получить урок по идентификатору. Удобно для формы редактирования урока или просмотра деталей.

**Метод и путь:** `GET /api/schedule/lessons/{id}`

**Роли:** любая аутентифицированная роль.

**Параметр пути:** `id` — UUID урока.

**Успешный ответ:** `200 OK` — объект **LessonDto**.

**Ошибки:**  
- `404 Not Found` — урок не найден (код `SCHEDULE_LESSON_NOT_FOUND`, сообщение вида `"Lesson not found: <id>"`).

---

## 4. Создание урока

### 4.1 POST /api/schedule/lessons

**Назначение:** создать один урок вручную. Урок владеет временем (startTime, endTime). timeslotId опционален (подсказка UI при создании из таймслота). Обычно уроки создаются автоматически через генерацию по офферингам (см. модуль Offerings), но можно создать урок вручную для разовых занятий или корректировок.

**Метод и путь:** `POST /api/schedule/lessons`

**Роли:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `offeringId` | UUID | обязательно | Id офферинга (группа + предмет учебного плана) |
| `date` | string | обязательно | Дата урока (ISO-8601, формат `yyyy-MM-dd`, например `"2025-02-07"`) |
| `startTime` | string | обязательно | Время начала (формат `"HH:mm"` или `"HH:mm:ss"`, например `"09:00"` или `"09:00:00"`) |
| `endTime` | string | обязательно | Время окончания (формат `"HH:mm"` или `"HH:mm:ss"`). Должно быть после startTime. |
| `timeslotId` | UUID | нет | Id таймслота-шаблона (опционально, подсказка UI) |
| `roomId` | UUID | нет | Id комнаты |
| `topic` | string | нет | Тема урока |
| `status` | string | нет | Статус: `PLANNED` (по умолчанию), `CANCELLED`, `DONE` |

**Успешный ответ:** `201 Created` — объект **LessonDto**.

**Ошибки:**  
- `400 Bad Request` — не указаны обязательные поля (offeringId, date, startTime, endTime); неверный формат даты или времени; endTime не после startTime (код `BAD_REQUEST`). При валидации через `@Valid` в `details` приходит объект «поле → сообщение» (код `VALIDATION_FAILED`).  
- `404 Not Found` — офферинг, таймслот или комната не найдены (коды `SCHEDULE_OFFERING_NOT_FOUND`, `SCHEDULE_TIMESLOT_NOT_FOUND`, `SCHEDULE_ROOM_NOT_FOUND`).  
- `409 Conflict` — урок с таким офферингом, датой и временем уже существует (код `SCHEDULE_LESSON_ALREADY_EXISTS`, сообщение `"Lesson already exists for this offering, date and time"`).

---

## 5. Обновление урока

### 5.1 PUT /api/schedule/lessons/{id}

**Назначение:** обновить урок: изменить время (startTime, endTime), комнату, тему или статус. Можно обновить только часть полей (остальные остаются без изменений).

**Метод и путь:** `PUT /api/schedule/lessons/{id}`

**Роли:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID урока.

**Тело запроса:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `startTime` | string | нет | Время начала (формат `"HH:mm"` или `"HH:mm:ss"`). Если указано вместе с endTime, оба должны быть валидными и endTime после startTime. |
| `endTime` | string | нет | Время окончания (формат `"HH:mm"` или `"HH:mm:ss"`). Если указано вместе с startTime, должно быть после startTime. |
| `roomId` | UUID \| null | нет | Id комнаты или `null` (чтобы убрать комнату) |
| `topic` | string \| null | Тема урока или `null` (чтобы очистить) |
| `status` | string | нет | Статус: `PLANNED`, `CANCELLED`, `DONE` |

**Успешный ответ:** `200 OK` — объект **LessonDto** (обновлённый урок).

**Ошибки:**  
- `400 Bad Request` — неверный формат времени; если указаны и startTime, и endTime, но endTime не после startTime (код `BAD_REQUEST`).  
- `404 Not Found` — урок или комната (если указана) не найдены (коды `SCHEDULE_LESSON_NOT_FOUND`, `SCHEDULE_ROOM_NOT_FOUND`).

---

## 6. Удаление урока

### 6.1 DELETE /api/schedule/lessons/{id}

**Назначение:** удалить урок. Удаляется только эта запись урока; офферинг и слоты офферинга не изменяются.

**Метод и путь:** `DELETE /api/schedule/lessons/{id}`

**Роли:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID урока.

**Успешный ответ:** `204 No Content` (тела ответа нет).

**Ошибки:**  
- `404 Not Found` — урок не найден (код `SCHEDULE_LESSON_NOT_FOUND`).

---

## 7. Сводная таблица ошибок

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Не указаны обязательные поля; неверный формат даты/времени; endTime не после startTime |
| 400 | VALIDATION_FAILED | Ошибка валидации запроса через Bean Validation (в `details` — объект «поле → сообщение») |
| 401 | UNAUTHORIZED | Не передан или невалидный JWT |
| 403 | FORBIDDEN | Нет роли MODERATOR, ADMIN или SUPER_ADMIN для операции изменения |
| 403 | SCHEDULE_TEACHER_PROFILE_NOT_FOUND | У пользователя нет профиля преподавателя (GET /lessons/week/teacher) |
| 404 | NOT_FOUND | Общий «не найдено» |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Урок не найден |
| 404 | SCHEDULE_OFFERING_NOT_FOUND | Офферинг не найден (при создании урока) |
| 404 | SCHEDULE_ROOM_NOT_FOUND | Комната не найдена (при создании/обновлении урока) |
| 404 | SCHEDULE_TIMESLOT_NOT_FOUND | Таймслот не найден (при создании урока) |
| 404 | SCHEDULE_GROUP_NOT_FOUND | Группа не найдена (GET /lessons/week/group/{groupId}) |
| 409 | CONFLICT | Общий конфликт |
| 409 | SCHEDULE_LESSON_ALREADY_EXISTS | Урок с таким офферингом, датой и временем уже существует |

---

## 8. Примеры запросов и ответов

### Получение уроков на дату

**Запрос:**  
`GET /api/schedule/lessons?date=2025-02-07`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK`

```json
[
  {
    "lesson": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "offeringId": "660e8400-e29b-41d4-a716-446655440010",
      "offeringSlotId": "770e8400-e29b-41d4-a716-446655440020",
      "date": "2025-02-07",
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "timeslotId": "880e8400-e29b-41d4-a716-446655440030",
      "roomId": "990e8400-e29b-41d4-a716-446655440040",
      "topic": "Введение в программирование",
      "status": "PLANNED",
      "createdAt": "2025-02-01T10:00:00",
      "updatedAt": "2025-02-01T10:00:00"
    },
    "offering": {
      "id": "660e8400-e29b-41d4-a716-446655440010",
      "groupId": "aa0e8400-e29b-41d4-a716-446655440050",
      "curriculumSubjectId": "bb0e8400-e29b-41d4-a716-446655440060",
      "teacherId": "cc0e8400-e29b-41d4-a716-446655440070"
    },
    "slot": {
      "id": "770e8400-e29b-41d4-a716-446655440020",
      "offeringId": "660e8400-e29b-41d4-a716-446655440010",
      "dayOfWeek": 5,
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "timeslotId": "880e8400-e29b-41d4-a716-446655440030",
      "lessonType": "LECTURE",
      "roomId": "990e8400-e29b-41d4-a716-446655440040",
      "teacherId": "cc0e8400-e29b-41d4-a716-446655440070",
      "createdAt": "2025-01-15T10:00:00"
    },
    "teachers": [
      {
        "teacherId": "cc0e8400-e29b-41d4-a716-446655440070",
        "role": null
      }
    ],
    "room": {
      "id": "990e8400-e29b-41d4-a716-446655440040",
      "number": "101",
      "buildingName": "Главный корпус"
    },
    "mainTeacher": {
      "id": "cc0e8400-e29b-41d4-a716-446655440070",
      "displayName": "Иванов Иван Иванович"
    },
    "subjectName": "Программирование",
    "group": {
      "id": "aa0e8400-e29b-41d4-a716-446655440050",
      "code": "CS-2024-1",
      "name": "Группа программистов 2024"
    }
  }
]
```

---

### Получение уроков на неделю для группы

**Запрос:**  
`GET /api/schedule/lessons/week/group/aa0e8400-e29b-41d4-a716-446655440050?date=2025-02-07`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK` — массив **LessonForScheduleDto** (структура та же, что выше).

---

### Получение урока по ID

**Запрос:**  
`GET /api/schedule/lessons/550e8400-e29b-41d4-a716-446655440001`  
`Authorization: Bearer <JWT>`

**Ответ:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "offeringId": "660e8400-e29b-41d4-a716-446655440010",
  "offeringSlotId": "770e8400-e29b-41d4-a716-446655440020",
  "date": "2025-02-07",
  "startTime": "09:00:00",
  "endTime": "10:30:00",
  "timeslotId": "880e8400-e29b-41d4-a716-446655440030",
  "roomId": "990e8400-e29b-41d4-a716-446655440040",
  "topic": "Введение в программирование",
  "status": "PLANNED",
  "createdAt": "2025-02-01T10:00:00",
  "updatedAt": "2025-02-01T10:00:00"
}
```

---

### Создание урока

**Запрос:**  
`POST /api/schedule/lessons`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "offeringId": "660e8400-e29b-41d4-a716-446655440010",
  "date": "2025-02-07",
  "startTime": "09:00",
  "endTime": "10:30",
  "timeslotId": "880e8400-e29b-41d4-a716-446655440030",
  "roomId": "990e8400-e29b-41d4-a716-446655440040",
  "topic": "Введение в программирование",
  "status": "PLANNED"
}
```

**Ответ:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "offeringId": "660e8400-e29b-41d4-a716-446655440010",
  "offeringSlotId": null,
  "date": "2025-02-07",
  "startTime": "09:00:00",
  "endTime": "10:30:00",
  "timeslotId": "880e8400-e29b-41d4-a716-446655440030",
  "roomId": "990e8400-e29b-41d4-a716-446655440040",
  "topic": "Введение в программирование",
  "status": "PLANNED",
  "createdAt": "2025-02-07T08:00:00",
  "updatedAt": "2025-02-07T08:00:00"
}
```

---

### Обновление урока

**Запрос:**  
`PUT /api/schedule/lessons/550e8400-e29b-41d4-a716-446655440001`  
`Authorization: Bearer <JWT>`  
`Content-Type: application/json`

```json
{
  "startTime": "09:30",
  "endTime": "11:00",
  "roomId": "aa0e8400-e29b-41d4-a716-446655440080",
  "topic": "Продолжение: массивы и циклы",
  "status": "PLANNED"
}
```

**Ответ:** `200 OK` — объект **LessonDto** с обновлёнными полями.

---

### Удаление урока

**Запрос:**  
`DELETE /api/schedule/lessons/550e8400-e29b-41d4-a716-446655440001`  
`Authorization: Bearer <JWT>`

**Ответ:** `204 No Content` (тела ответа нет).

---

### Ошибка: урок не найден

**Запрос:** `GET /api/schedule/lessons/00000000-0000-0000-0000-000000000000`

**Ответ:** `404 Not Found`

```json
{
  "code": "SCHEDULE_LESSON_NOT_FOUND",
  "message": "Lesson not found: 00000000-0000-0000-0000-000000000000",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка: урок уже существует

**Запрос:** `POST /api/schedule/lessons` с телом, где offeringId, date, startTime, endTime совпадают с существующим уроком.

**Ответ:** `409 Conflict`

```json
{
  "code": "SCHEDULE_LESSON_ALREADY_EXISTS",
  "message": "Lesson already exists for this offering, date and time",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка: нет профиля преподавателя

**Запрос:** `GET /api/schedule/lessons/week/teacher?date=2025-02-07` от пользователя без профиля преподавателя.

**Ответ:** `403 Forbidden`

```json
{
  "code": "SCHEDULE_TEACHER_PROFILE_NOT_FOUND",
  "message": "User does not have a teacher profile",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### Ошибка валидации

**Запрос:** `POST /api/schedule/lessons` с телом без обязательных полей.

**Ответ:** `400 Bad Request`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": {
    "offeringId": "Offering id is required",
    "date": "Date is required",
    "startTime": "Start time is required",
    "endTime": "End time is required"
  }
}
```
