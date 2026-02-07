# Контракт API: расписание (здания, аудитории, слоты, занятия)

Документ-контракт для фронтенда. Описаны эндпоинты модуля Schedule: здания (buildings), аудитории (rooms), шаблоны времени (timeslots) и занятия (lessons). Занятие привязано к предложению (offering), хранит дату и время; слот (timeslot) — шаблон «день недели + интервал» для UI. Для контекста расписания описан также эндпоинт модуля Academic: получение семестра по дате. Структуры запросов и ответов, валидация и возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/schedule` | Здания, аудитории, слоты времени, занятия |
| `/api/academic` | Учебные годы и семестры (для контекста расписания — семестр по дате) |

Хост и порт задаются окружением. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы выполняются с заголовком **`Authorization: Bearer <JWT>`**.
- Эндпоинты, которые **изменяют данные** (POST, PUT, DELETE), доступны только пользователям с одной из ролей: **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.
- Эндпоинты только для чтения (GET) в этом документе не помечены отдельной ролью; при необходимости уточните политику доступа в проекте.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки в формате **ISO-8601**: дата-время вида `"2025-02-05T12:00:00"`, дата вида `"2025-02-05"`, время вида `"09:00"` или `"09:00:00"`.
- **Числа** — JSON number; для целых полей (день недели, вместимость) — целое число.
- **Время в ответах** — всегда строка в формате **HH:mm:ss** (канон для полей startTime, endTime). В запросах допускается HH:mm или HH:mm:ss.
- **null** — явное отсутствие значения; поля могут не присутствовать в JSON или быть `null` в зависимости от контракта.

### 1.4 Формат ошибок

При любой ошибке (4xx, 5xx) сервер возвращает JSON-объект **ErrorResponse**:

| Поле | Тип | Описание |
|------|-----|----------|
| `code` | string | Код ошибки: общий (например `BAD_REQUEST`, `NOT_FOUND`, `VALIDATION_FAILED`, `UNAUTHORIZED`, `FORBIDDEN`) или доменный модуля Schedule (`SCHEDULE_BUILDING_NOT_FOUND`, `SCHEDULE_ROOM_NOT_FOUND`, `SCHEDULE_TIMESLOT_NOT_FOUND`, `SCHEDULE_LESSON_NOT_FOUND`, `SCHEDULE_OFFERING_NOT_FOUND`, `SCHEDULE_GROUP_NOT_FOUND`, `SCHEDULE_BUILDING_HAS_ROOMS`, `SCHEDULE_LESSON_ALREADY_EXISTS`). Фронту достаточно различать общий и доменный код по префиксу. |
| `message` | string | Человекочитаемое сообщение |
| `timestamp` | string | Момент возникновения ошибки (ISO-8601, UTC) |
| `details` | object \| null | Опционально. Для ошибок валидации — объект «имя поля → сообщение»; иначе может отсутствовать |

**Канон для 400:** при **аннотационной валидации** (пустые/неверные поля по аннотациям) — всегда код `VALIDATION_FAILED` и объект в `details`. При **нарушении формата или логики** (формат даты/времени, endTime ≤ startTime, dayOfWeek и т.п.) — код `BAD_REQUEST`. **Несуществующие ссылки** (offering, room, timeslot, group, building, lesson) — всегда **404** с соответствующим кодом (SCHEDULE_*_NOT_FOUND), не 400.

Пример ответа с ошибкой валидации (400):

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Ошибка проверки данных. Проверьте заполненные поля.",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": {
    "name": "Name is required",
    "capacity": "Capacity must be >= 0"
  }
}
```

Пример ответа с ошибкой «не найдено» (404):

```json
{
  "code": "SCHEDULE_BUILDING_NOT_FOUND",
  "message": "Building not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

## 2. Модели данных

### 2.1 BuildingDto (здание)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор здания |
| `name` | string | Название здания |
| `address` | string \| null | Адрес |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

### 2.2 RoomDto (аудитория)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор аудитории |
| `buildingId` | UUID | Id здания |
| `buildingName` | string | Название здания (для отображения) |
| `number` | string | Номер аудитории |
| `capacity` | integer \| null | Вместимость (≥ 0) |
| `type` | string \| null | Тип аудитории |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

### 2.3 TimeslotDto (шаблон времени)

Шаблон «день недели + интервал» для выбора времени занятия в UI (например, «Пн 09:00–10:30»).

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор слота |
| `dayOfWeek` | integer | День недели: 1 = понедельник, 7 = воскресенье |
| `startTime` | string | Время начала (в ответах всегда **HH:mm:ss**) |
| `endTime` | string | Время окончания (в ответах всегда **HH:mm:ss**) |

### 2.4 LessonDto (занятие)

Занятие хранит дату и время (startTime, endTime).

**Правило про timeslotId:** `timeslotId` — справочный атрибут для UI; может не совпадать с startTime/endTime; **сервер не валидирует соответствие** (не доверяйте слоту для отображения времени — используйте startTime/endTime).

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор занятия |
| `offeringId` | UUID | Id предложения (offering) |
| `offeringSlotId` | UUID \| null | Id слота предложения, из которого сгенерировано занятие (для типа и преподавателя в UI) |
| `date` | string | Дата занятия (yyyy-MM-dd) |
| `startTime` | string | Время начала (в ответах всегда **HH:mm:ss**) |
| `endTime` | string | Время окончания (в ответах всегда **HH:mm:ss**) |
| `timeslotId` | UUID \| null | Id шаблона времени; справочный, может не совпадать с startTime/endTime; сервер не валидирует соответствие |
| `roomId` | UUID \| null | Id аудитории |
| `topic` | string \| null | Тема занятия |
| `status` | string | Статус: `PLANNED`, `CANCELLED`, `DONE` (по умолчанию при создании — `PLANNED`; консистентно с lessonType в UPPER_CASE) |
| `createdAt` | string | Дата и время создания (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

### 2.5 LessonForScheduleDto (занятие с контекстом для расписания)

Используется в ответах **GET /api/schedule/lessons?date=**, **GET /api/schedule/lessons/week?date=**, **GET /api/schedule/lessons/week/group/{groupId}?date=** и **GET /api/schedule/lessons/group/{groupId}?date=**. Содержит занятие, офферинг, слот, учителей, комнату, основного преподавателя и название предмета — достаточно для построения расписания без дополнительных запросов.

| Поле | Тип | Описание |
|------|-----|----------|
| `lesson` | object | Занятие — объект **LessonDto** (см. 2.4) |
| `offering` | object \| null | Краткие данные офферинга — **OfferingSummaryDto**; может быть null |
| `slot` | object \| null | Слот офферинга (день, время, тип занятия, комната, учитель слота) — **SlotSummaryDto**; null для занятий без offeringSlotId |
| `teachers` | array | Учителя, привязанные к офферингу с ролью (LECTURE, PRACTICE, LAB) — массив **TeacherRoleDto** |
| `room` | object \| null | Аудитория для отображения — **RoomSummaryDto**; приоритет: **room урока** (lesson.roomId) → **room слота** (slot.roomId) → null. При конфликте используется это правило для стабильного поведения. |
| `mainTeacher` | object \| null | Основной преподаватель (ведёт предмет/слот) — **TeacherSummaryDto**; приоритет: **учитель слота** (slot.teacherId) → **учитель офферинга** (offering.teacherId) → null. |
| `subjectName` | string \| null | Название предмета (для отображения); может быть null |

**Правила приоритета (при конфликте источников):**

- **room:** сначала аудитория, указанная у занятия (lesson.roomId); если у занятия нет roomId — аудитория слота (slot.roomId); иначе null.
- **mainTeacher:** сначала преподаватель, привязанный к слоту (slot.teacherId); если слота нет или у слота нет teacherId — преподаватель офферинга (offering.teacherId); иначе null.

**OfferingSummaryDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id офферинга |
| `groupId` | UUID | Id группы |
| `curriculumSubjectId` | UUID | Id предмета учебного плана |
| `teacherId` | UUID \| null | Id учителя офферинга (основной преподаватель по умолчанию) |

**SlotSummaryDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id слота офферинга |
| `offeringId` | UUID | Id офферинга |
| `dayOfWeek` | integer | День недели (1–7) |
| `startTime` | string | Время начала (в ответах всегда **HH:mm:ss**) |
| `endTime` | string | Время окончания |
| `timeslotId` | UUID \| null | Id шаблона времени |
| `lessonType` | string | Тип занятия: `LECTURE`, `PRACTICE`, `LAB`, `SEMINAR` |
| `roomId` | UUID \| null | Id аудитории слота |
| `teacherId` | UUID \| null | Id учителя, привязанного к слоту |
| `createdAt` | string | Дата и время создания слота (ISO-8601) |

**TeacherRoleDto:**

| Поле | Тип | Описание |
|------|-----|----------|
| `teacherId` | UUID | Id учителя (профиль) |
| `role` | string | Роль: `LECTURE`, `PRACTICE`, `LAB` |

**RoomSummaryDto** (аудитория для отображения в расписании):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id аудитории |
| `number` | string | Номер аудитории |
| `buildingName` | string \| null | Название здания |

**TeacherSummaryDto** (преподаватель для отображения):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id преподавателя (профиль) |
| `displayName` | string | Отображаемое имя (englishName или personnel number) |

### 2.6 Уроки без слота (ручные уроки)

Когда у занятия нет привязки к слоту офферинга (`offeringSlotId === null`), в ответе **slot** будет **null**. Такие уроки (ручные) отображаются так:

- **Время** — всегда есть в объекте **lesson**: `date`, `startTime`, `endTime`. UI использует их напрямую.
- **Тип занятия** — у ручного урока нет слота, поэтому тип (LECTURE, PRACTICE, LAB и т.д.) не задан. Рекомендуется показывать в UI «—» или «Custom».
- **room** и **mainTeacher** — вычисляются в любом случае: **room** по приоритету «room урока → room слота → null» (у ручного урока слот отсутствует, поэтому берётся только room урока, если задан). **mainTeacher** по приоритету «учитель слота → учитель офферинга» (у ручного урока — учитель офферинга, если есть). Таким образом, ручной урок можно полноценно показать в расписании с аудиторией и преподавателем.

---

## 3. Здания (Buildings)

### 3.1 GET /api/schedule/buildings

**Назначение:** получить список всех зданий (для выпадающих списков и карточек).

**Метод и путь:** `GET /api/schedule/buildings`

**Параметры:** нет.

**Успешный ответ:** `200 OK` — массив объектов **BuildingDto**. Порядок: по названию.

**Ошибки:** при невалидном JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

---

### 3.2 GET /api/schedule/buildings/{id}

**Назначение:** получить здание по id.

**Метод и путь:** `GET /api/schedule/buildings/{id}`

**Параметр пути:** `id` — UUID здания.

**Успешный ответ:** `200 OK` — один объект **BuildingDto**.

**Ошибки:** `404 Not Found` — здание не найдено. Код в теле: `SCHEDULE_BUILDING_NOT_FOUND`, сообщение вида `"Building not found: <id>"`.

---

### 3.3 POST /api/schedule/buildings

**Назначение:** создать здание.

**Метод и путь:** `POST /api/schedule/buildings`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:**

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `name` | string | да | Название здания (не пустое) |
| `address` | string | нет | Адрес |

**Успешный ответ:** `201 Created` — созданный объект **BuildingDto**.

**Ошибки:**

- `400 Bad Request` — пустое имя; при аннотационной валидации код `VALIDATION_FAILED`, в `details` — объект «поле → сообщение».
- `401 Unauthorized` — нет или невалидный JWT.
- `403 Forbidden` — недостаточно прав (код `FORBIDDEN`).

---

### 3.4 PUT /api/schedule/buildings/{id}

**Назначение:** обновить здание.

**Метод и путь:** `PUT /api/schedule/buildings/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID здания.

**Тело запроса:**

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `name` | string | нет | Новое название |
| `address` | string | нет | Новый адрес |

**Успешный ответ:** `200 OK` — обновлённый объект **BuildingDto**.

**Ошибки:**

- `404 Not Found` — здание не найдено (код `SCHEDULE_BUILDING_NOT_FOUND`).
- `401` / `403` — как в 3.3.

---

### 3.5 DELETE /api/schedule/buildings/{id}

**Назначение:** удалить здание. Нельзя удалить здание, в котором есть аудитории.

**Метод и путь:** `DELETE /api/schedule/buildings/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID здания.

**Успешный ответ:** `204 No Content`.

**Ошибки:**

- `404 Not Found` — здание не найдено (`SCHEDULE_BUILDING_NOT_FOUND`).
- `409 Conflict` — в здании есть аудитории. Код `SCHEDULE_BUILDING_HAS_ROOMS`, сообщение: «Building has rooms; delete or reassign rooms first».
- `401` / `403` — как в 3.3.

---

## 4. Аудитории (Rooms)

### 4.1 GET /api/schedule/rooms

**Назначение:** получить список всех аудиторий (для выбора аудитории при создании/редактировании занятия).

**Метод и путь:** `GET /api/schedule/rooms`

**Параметры:** нет.

**Успешный ответ:** `200 OK` — массив объектов **RoomDto**. Порядок: по зданию, затем по номеру.

**Ошибки:** при невалидном JWT — `401 Unauthorized`.

---

### 4.2 GET /api/schedule/rooms/{id}

**Назначение:** получить аудиторию по id.

**Метод и путь:** `GET /api/schedule/rooms/{id}`

**Параметр пути:** `id` — UUID аудитории.

**Успешный ответ:** `200 OK` — один объект **RoomDto**.

**Ошибки:** `404 Not Found` — аудитория не найдена. Код `SCHEDULE_ROOM_NOT_FOUND`, сообщение вида `"Room not found: <id>"`.

---

### 4.3 POST /api/schedule/rooms

**Назначение:** создать аудиторию в здании.

**Метод и путь:** `POST /api/schedule/rooms`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:** объект **RoomCreateRequest**.

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `buildingId` | UUID | да | Id здания |
| `number` | string | да | Номер аудитории (не пустой) |
| `capacity` | integer | нет | Вместимость (≥ 0); при отсутствии может быть null |
| `type` | string | нет | Тип аудитории |

**Успешный ответ:** `201 Created` — созданный объект **RoomDto**.

**Ошибки:**

- `400 Bad Request` — не указан buildingId или number; capacity < 0. При аннотационной валидации код `VALIDATION_FAILED`, в `details` — объект «поле → сообщение».
- `404 Not Found` — здание не найдено. Код `SCHEDULE_BUILDING_NOT_FOUND`, сообщение вида `"Building not found: <id>"`.
- `401` / `403` — как в 3.3.

---

### 4.4 POST /api/schedule/rooms/bulk

**Назначение:** создать несколько аудиторий одной транзакцией (всё или ничего).

**Метод и путь:** `POST /api/schedule/rooms/bulk`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:** массив объектов **RoomCreateRequest** (структура как в 4.3).

**Успешный ответ:** `201 Created` — массив созданных объектов **RoomDto** в том же порядке.

**Ошибки:** при ошибке валидации или при отсутствии любого здания — откат транзакции, ответ с соответствующим кодом (400/404). `401` / `403` — как в 3.3.

---

### 4.5 PUT /api/schedule/rooms/{id}

**Назначение:** обновить аудиторию (здание, номер, вместимость, тип).

**Метод и путь:** `PUT /api/schedule/rooms/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID аудитории.

**Тело запроса:**

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `buildingId` | UUID | нет | Id здания (при переносе в другое здание) |
| `number` | string | нет | Новый номер |
| `capacity` | integer | нет | Вместимость (≥ 0) |
| `type` | string | нет | Тип |

**Успешный ответ:** `200 OK` — обновлённый объект **RoomDto**.

**Ошибки:**

- `400 Bad Request` — capacity < 0; при аннотационной валидации — `VALIDATION_FAILED` и `details`.
- `404 Not Found` — аудитория или (при смене здания) здание не найдены.
- `401` / `403` — как в 3.3.

---

### 4.6 DELETE /api/schedule/rooms/{id}

**Назначение:** удалить аудиторию.

**Метод и путь:** `DELETE /api/schedule/rooms/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID аудитории.

**Успешный ответ:** `204 No Content`.

**Ошибки:** `404 Not Found` — аудитория не найдена (`SCHEDULE_ROOM_NOT_FOUND`). `401` / `403` — как в 3.3.

---

## 5. Слоты времени (Timeslots)

Шаблоны времени для UI (например, «Пн 09:00–10:30», «Вт 14:00–15:30»). При удалении слота занятия не удаляются; у занятий поле `timeslotId` обнуляется.

### 5.1 GET /api/schedule/timeslots

**Назначение:** получить все шаблоны времени (для выбора при создании занятия или настройке расписания).

**Метод и путь:** `GET /api/schedule/timeslots`

**Параметры:** нет.

**Успешный ответ:** `200 OK` — массив объектов **TimeslotDto**. Порядок: по дню недели, затем по времени начала.

**Ошибки:** при невалидном JWT — `401 Unauthorized`.

---

### 5.2 GET /api/schedule/timeslots/{id}

**Назначение:** получить слот по id.

**Метод и путь:** `GET /api/schedule/timeslots/{id}`

**Параметр пути:** `id` — UUID слота.

**Успешный ответ:** `200 OK` — один объект **TimeslotDto**.

**Ошибки:** `404 Not Found` — слот не найден. Код `SCHEDULE_TIMESLOT_NOT_FOUND`, сообщение вида `"Timeslot not found: <id>"`.

---

### 5.3 POST /api/schedule/timeslots

**Назначение:** создать шаблон времени.

**Метод и путь:** `POST /api/schedule/timeslots`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:** объект **TimeslotCreateRequest**.

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `dayOfWeek` | integer | да | День недели: 1–7 (1 = понедельник, 7 = воскресенье) |
| `startTime` | string | да | Время начала (HH:mm или HH:mm:ss) |
| `endTime` | string | да | Время окончания (HH:mm или HH:mm:ss); должно быть позже startTime |

**Успешный ответ:** `201 Created` — созданный объект **TimeslotDto**.

**Ошибки:**

- `400 Bad Request` — dayOfWeek не из диапазона 1–7; неверный формат времени; endTime не позже startTime. При аннотационной валидации код `VALIDATION_FAILED`, в `details` — объект «поле → сообщение».
- `401` / `403` — как в 3.3.

---

### 5.4 POST /api/schedule/timeslots/bulk

**Назначение:** создать несколько слотов одной транзакцией (всё или ничего).

**Метод и путь:** `POST /api/schedule/timeslots/bulk`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:** массив объектов **TimeslotCreateRequest** (структура как в 5.3).

**Успешный ответ:** `201 Created` — массив созданных объектов **TimeslotDto** в том же порядке.

**Ошибки:** при ошибке — откат всей пачки, ответ `400`. **Канон:** при аннотационной валидации (пустые/неверные поля по аннотациям) — код `VALIDATION_FAILED` и `details`; при нарушении формата или логики (dayOfWeek, время, endTime ≤ startTime) — код `BAD_REQUEST`. `401` / `403` — как в 3.3.

---

### 5.5 DELETE /api/schedule/timeslots/{id}

**Назначение:** удалить слот. Занятия, ссылающиеся на этот слот, сохраняются; у них `timeslotId` устанавливается в null.

**Метод и путь:** `DELETE /api/schedule/timeslots/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID слота.

**Успешный ответ:** `204 No Content`.

**Ошибки:** `404 Not Found` — слот не найден (`SCHEDULE_TIMESLOT_NOT_FOUND`). `401` / `403` — как в 3.3.

---

### 5.6 DELETE /api/schedule/timeslots

**Назначение:** удалить все слоты. Занятия сохраняются; у занятий, ссылавшихся на слоты, `timeslotId` обнуляется.

**Метод и путь:** `DELETE /api/schedule/timeslots`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметры:** нет.

**Успешный ответ:** `204 No Content`.

**Ошибки:** `401` / `403` — как в 3.3.

---

## 6. Занятия (Lessons)

Занятие привязано к предложению (offering), хранит дату и время (startTime, endTime). Опционально можно указать timeslotId (подсказка для UI) и roomId.

**Общие правила валидации:**

- Сервер **не проверяет пересечения** по room / teacher / group (два занятия в одной аудитории в одно время, двойная нагрузка преподавателя и т.п. не запрещены).
- Сервер проверяет только **базовую валидацию**: формат даты/времени, endTime > startTime; при отсутствии ссылок (offering, room, timeslot при указании) возвращается **404**, не 400.

### 6.1 GET /api/schedule/lessons

**Назначение:** получить занятия на указанную дату с полным контекстом для построения расписания (офферинг, слот, учителя). Дополнительные запросы к офферингам/слотам/учителям не требуются.

**Метод и путь:** `GET /api/schedule/lessons`

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `date` | string | да | Дата в формате ISO-8601 (yyyy-MM-dd), например `2025-02-07` |

**Пример:** `GET /api/schedule/lessons?date=2025-02-07`

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto** (п. 2.5). В каждом элементе: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`. Порядок: по времени начала. Загрузка офферингов, слотов, учителей, комнат и названий предметов выполняется батчем (без N+1).

**Ошибки:** при неверном формате даты — `400 Bad Request`. При невалидном JWT — `401 Unauthorized`.

---

### 6.2 GET /api/schedule/lessons/group/{groupId}

**Назначение:** получить занятия на указанную дату **для конкретной группы** с полным контекстом (офферинг, слот, учителя). Удобно для экрана «Расписание группы на день» без фильтрации и без дополнительных запросов.

**Метод и путь:** `GET /api/schedule/lessons/group/{groupId}`

**Параметры пути:** `groupId` — UUID группы.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `date` | string | да | Дата в формате ISO-8601 (yyyy-MM-dd), например `2025-02-07` |

**Пример:** `GET /api/schedule/lessons/group/550e8400-e29b-41d4-a716-446655440000?date=2025-02-07`

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto** (п. 2.5). В выборку попадают только занятия офферингов данной группы на указанную дату; в каждом элементе: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`. Порядок: по времени начала. **Если группа существует, но у неё нет офферингов или занятий на эту дату — пустой массив `[]`.** Загрузка контекста — батчем (без N+1).

**Ошибки:**

- `404 Not Found` — **группа не существует.** Код в теле: `SCHEDULE_GROUP_NOT_FOUND`, сообщение вида `"Group not found: <groupId>"`. Так фронт явно отличает «неверный id» от «группа есть, занятий нет».
- `400 Bad Request` — неверный формат даты.
- `401 Unauthorized` — невалидный JWT.

---

### 6.3 GET /api/schedule/lessons/week

**Назначение:** получить занятия на **неделю**, в которую входит указанная дата (ISO-неделя: понедельник–воскресенье), с полным контекстом для построения расписания (офферинг, слот, учителя). Структура ответа совпадает с ручкой получения занятий на день (максимум информации — LessonForScheduleDto). Удобно для экрана «Расписание на неделю» без дополнительных запросов.

**Метод и путь:** `GET /api/schedule/lessons/week`

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `date` | string | да | Любая дата в нужной неделе в формате ISO-8601 (yyyy-MM-dd), например `2025-02-07`. Неделя определяется как понедельник–воскресенье (ISO). |

**Пример:** `GET /api/schedule/lessons/week?date=2025-02-07`

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto** (п. 2.5). В выборку попадают все занятия, дата которых попадает в интервал [понедельник этой недели, воскресенье этой недели]. В каждом элементе: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`. Порядок: по дате, затем по времени начала. Загрузка офферингов, слотов, учителей, комнат и названий предметов выполняется батчем (без N+1).

**Ошибки:** при неверном формате даты — `400 Bad Request`. При невалидном JWT — `401 Unauthorized`.

---

### 6.4 GET /api/schedule/lessons/week/group/{groupId}

**Назначение:** получить занятия на **неделю** (ISO: понедельник–воскресенье) **для указанной группы** с полным контекстом. То же, что GET /api/schedule/lessons/week, но с фильтром по группе — в выборку попадают только занятия офферингов этой группы. Удобно для экрана «Расписание группы на неделю» без маппинга на фронте.

**Метод и путь:** `GET /api/schedule/lessons/week/group/{groupId}`

**Параметры пути:** `groupId` — UUID группы.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `date` | string | да | Любая дата в нужной неделе в формате ISO-8601 (yyyy-MM-dd). Неделя определяется как понедельник–воскресенье (ISO). |

**Пример:** `GET /api/schedule/lessons/week/group/550e8400-e29b-41d4-a716-446655440000?date=2025-02-07`

**Успешный ответ:** `200 OK` — массив объектов **LessonForScheduleDto** (п. 2.5). В выборку попадают занятия, дата которых в указанной неделе и offering которых принадлежит группе. В каждом элементе: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`. Порядок: по дате, затем по времени начала. **Если группа существует, но у неё нет офферингов или занятий в эту неделю — пустой массив `[]`.** Загрузка контекста — батчем (без N+1).

**Ошибки:**

- `404 Not Found` — **группа не существует.** Код в теле: `SCHEDULE_GROUP_NOT_FOUND`, сообщение вида `"Group not found: <groupId>"`.
- `400 Bad Request` — неверный формат даты.
- `401 Unauthorized` — невалидный JWT.

---

### 6.5 GET /api/schedule/lessons/offering/{offeringId}

**Назначение:** получить все занятия по предложению (для экрана «Занятия курса»).

**Метод и путь:** `GET /api/schedule/lessons/offering/{offeringId}`

**Параметр пути:** `offeringId` — UUID предложения (offering).

**Успешный ответ:** `200 OK` — массив объектов **LessonDto** (возможно пустой). Порядок: по дате, затем по времени начала.

**Ошибки:** при невалидном JWT — `401 Unauthorized`.

---

### 6.6 GET /api/schedule/lessons/{id}

**Назначение:** получить занятие по id.

**Метод и путь:** `GET /api/schedule/lessons/{id}`

**Параметр пути:** `id` — UUID занятия.

**Успешный ответ:** `200 OK` — один объект **LessonDto**.

**Ошибки:** `404 Not Found` — занятие не найдено. Код `SCHEDULE_LESSON_NOT_FOUND`, сообщение вида `"Lesson not found: <id>"`.

---

### 6.7 POST /api/schedule/lessons

**Назначение:** создать занятие. Время занятия задаётся полями startTime и endTime (строки); timeslotId опционален (подсказка для UI).

**Метод и путь:** `POST /api/schedule/lessons`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Тело запроса:**

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `offeringId` | UUID | да | Id предложения |
| `date` | string | да | Дата (yyyy-MM-dd) |
| `startTime` | string | да | Время начала (HH:mm или HH:mm:ss) |
| `endTime` | string | да | Время окончания (HH:mm или HH:mm:ss); должно быть позже startTime |
| `timeslotId` | UUID | нет | Id шаблона времени (справочный для UI; сервер не проверяет соответствие startTime/endTime) |
| `roomId` | UUID | нет | Id аудитории |
| `topic` | string | нет | Тема |
| `status` | string | нет | Статус: `PLANNED`, `CANCELLED`, `DONE`; по умолчанию `PLANNED` |

**Успешный ответ:** `201 Created` — созданный объект **LessonDto**.

**Ошибки:**

- `400 Bad Request` — неверный формат даты/времени; endTime не позже startTime; status не из списка (PLANNED, CANCELLED, DONE). При аннотационной валидации код `VALIDATION_FAILED`, в `details` — объект «поле → сообщение».
- `404 Not Found` — предложение (offering), слот (timeslot) или аудитория (room) не найдены. Коды: `SCHEDULE_OFFERING_NOT_FOUND`, `SCHEDULE_TIMESLOT_NOT_FOUND`, `SCHEDULE_ROOM_NOT_FOUND`.
- `409 Conflict` — занятие с таким же offeringId, date, startTime, endTime уже существует. Код `SCHEDULE_LESSON_ALREADY_EXISTS`, сообщение: «Lesson already exists for this offering, date and time».
- `401` / `403` — как в 3.3.

---

### 6.8 PUT /api/schedule/lessons/{id}

**Назначение:** обновить занятие (время, аудитория, тема, статус).

**Метод и путь:** `PUT /api/schedule/lessons/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID занятия.

**Тело запроса:** все поля опциональны.

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `startTime` | string | нет | Время начала (в запросах допускается HH:mm или HH:mm:ss) |
| `endTime` | string | нет | Время окончания; если указаны оба времени — endTime должно быть позже startTime |
| `roomId` | UUID \| null | нет | Id аудитории; **разрешено передать `null`** — снять привязку к аудитории |
| `topic` | string | нет | Тема |
| `status` | string | нет | Статус: `PLANNED`, `CANCELLED`, `DONE` |

**Успешный ответ:** `200 OK` — обновлённый объект **LessonDto**.

**Ошибки:**

- `400 Bad Request` — неверный формат времени или endTime не позже startTime; неверный status.
- `404 Not Found` — занятие не найдено; или (только при указании непустого roomId) аудитория не найдена. При передаче `roomId: null` проверка аудитории не выполняется — снятие комнаты всегда разрешено.
- `401` / `403` — как в 3.3.

---

### 6.9 DELETE /api/schedule/lessons/{id}

**Назначение:** удалить занятие.

**Метод и путь:** `DELETE /api/schedule/lessons/{id}`

**Права доступа:** MODERATOR, ADMIN, SUPER_ADMIN.

**Параметр пути:** `id` — UUID занятия.

**Успешный ответ:** `204 No Content`.

**Ошибки:** `404 Not Found` — занятие не найдено (`SCHEDULE_LESSON_NOT_FOUND`). `401` / `403` — как в 3.3.

---

### 6.10 GET /api/academic/semesters/by-date — семестр по дате (модуль Academic)

**Назначение:** получить семестр, в который попадает указанная дата (для отображения контекста в экране расписания: название семестра, учебный год, границы семестра).

**Метод и путь:** `GET /api/academic/semesters/by-date`

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `date` | string | да | Дата в формате ISO-8601 (yyyy-MM-dd), например `2025-02-07` |

**Пример:** `GET /api/academic/semesters/by-date?date=2025-02-07`

**Успешный ответ:** `200 OK` — объект **SemesterDto** (модуль Academic):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Идентификатор семестра |
| `academicYearId` | UUID | Id учебного года |
| `number` | integer | Номер семестра (1, 2, …) |
| `name` | string \| null | Название семестра |
| `startDate` | string | Дата начала (yyyy-MM-dd) |
| `endDate` | string | Дата окончания (yyyy-MM-dd) |
| `examStartDate` | string \| null | Начало экзаменационной сессии |
| `examEndDate` | string \| null | Окончание экзаменационной сессии |
| `weekCount` | integer \| null | Количество учебных недель |
| `isCurrent` | boolean | Является ли семестр текущим |
| `createdAt` | string | Дата и время создания (ISO-8601) |

**Ошибки:**

- `400 Bad Request` — параметр `date` отсутствует или неверный формат (ожидается yyyy-MM-dd). Код `BAD_REQUEST` или `VALIDATION_FAILED` (в зависимости от реализации).
- `404 Not Found` — ни один семестр не содержит указанную дату (дата вне границ всех семестров). Код `NOT_FOUND`, сообщение вида «Semester not found for date: …» или общее.
- `401 Unauthorized` — невалидный JWT.

---

## 7. Сводная таблица кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Неверные данные: формат даты/времени, endTime ≤ startTime, dayOfWeek не 1–7, capacity < 0, status не PLANNED/CANCELLED/DONE, пустые обязательные поля и т.п. |
| 400 | VALIDATION_FAILED | Ошибка аннотационной валидации; в `details` — объект «поле → сообщение» |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Недостаточно прав (нет роли MODERATOR/ADMIN/SUPER_ADMIN) |
| 404 | SCHEDULE_BUILDING_NOT_FOUND | Здание не найдено |
| 404 | SCHEDULE_ROOM_NOT_FOUND | Аудитория не найдена |
| 404 | SCHEDULE_TIMESLOT_NOT_FOUND | Слот не найден |
| 404 | SCHEDULE_LESSON_NOT_FOUND | Занятие не найдено |
| 404 | SCHEDULE_OFFERING_NOT_FOUND | Предложение (offering) не найдено (при создании занятия) |
| 404 | SCHEDULE_GROUP_NOT_FOUND | Группа не найдена (GET /api/schedule/lessons/group/{groupId}) |
| 404 | NOT_FOUND | В т.ч. семестр не найден для указанной даты (GET /api/academic/semesters/by-date) |
| 409 | SCHEDULE_BUILDING_HAS_ROOMS | Нельзя удалить здание: в нём есть аудитории |
| 409 | SCHEDULE_LESSON_ALREADY_EXISTS | Занятие с таким же offering, датой и временем уже существует |

Точные формулировки сообщений приведены в разделах по эндпоинтам выше.

---

## 8. Примеры запросов и ответов

### 8.1 POST /api/schedule/buildings — создание здания

**Запрос:** `POST /api/schedule/buildings`  
**Тело:**

```json
{
  "name": "Корпус A",
  "address": "ул. Университетская, 1"
}
```

**Ответ 201 Created:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Корпус A",
  "address": "ул. Университетская, 1",
  "createdAt": "2025-02-07T10:00:00",
  "updatedAt": "2025-02-07T10:00:00"
}
```

---

### 8.2 POST /api/schedule/rooms — создание аудитории

**Запрос:** `POST /api/schedule/rooms`  
**Тело:**

```json
{
  "buildingId": "550e8400-e29b-41d4-a716-446655440001",
  "number": "101",
  "capacity": 30,
  "type": "lecture"
}
```

**Ответ 201 Created:**

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440002",
  "buildingId": "550e8400-e29b-41d4-a716-446655440001",
  "buildingName": "Корпус A",
  "number": "101",
  "capacity": 30,
  "type": "lecture",
  "createdAt": "2025-02-07T10:05:00",
  "updatedAt": "2025-02-07T10:05:00"
}
```

---

### 8.3 POST /api/schedule/timeslots — создание слота

**Запрос:** `POST /api/schedule/timeslots`  
**Тело:**

```json
{
  "dayOfWeek": 1,
  "startTime": "09:00",
  "endTime": "10:30"
}
```

**Ответ 201 Created:**

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "dayOfWeek": 1,
  "startTime": "09:00:00",
  "endTime": "10:30:00"
}
```

---

### 8.4 POST /api/schedule/lessons — создание занятия

**Запрос:** `POST /api/schedule/lessons`  
**Тело:**

```json
{
  "offeringId": "880e8400-e29b-41d4-a716-446655440010",
  "date": "2025-02-10",
  "startTime": "09:00",
  "endTime": "10:30",
  "timeslotId": "770e8400-e29b-41d4-a716-446655440003",
  "roomId": "660e8400-e29b-41d4-a716-446655440002",
  "topic": "Введение в курс",
  "status": "PLANNED"
}
```

**Ответ 201 Created:**

```json
{
  "id": "990e8400-e29b-41d4-a716-446655440020",
  "offeringId": "880e8400-e29b-41d4-a716-446655440010",
  "offeringSlotId": null,
  "date": "2025-02-10",
  "startTime": "09:00:00",
  "endTime": "10:30:00",
  "timeslotId": "770e8400-e29b-41d4-a716-446655440003",
  "roomId": "660e8400-e29b-41d4-a716-446655440002",
  "topic": "Введение в курс",
  "status": "PLANNED",
  "createdAt": "2025-02-07T11:00:00",
  "updatedAt": "2025-02-07T11:00:00"
}
```

---

### 8.5 GET /api/schedule/lessons?date=2025-02-07 — занятия на дату с контекстом

**Запрос:** `GET /api/schedule/lessons?date=2025-02-07`

**Ответ 200 OK (массив LessonForScheduleDto):**

```json
[
  {
    "lesson": {
      "id": "990e8400-e29b-41d4-a716-446655440020",
      "offeringId": "880e8400-e29b-41d4-a716-446655440010",
      "offeringSlotId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "date": "2025-02-07",
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "timeslotId": "770e8400-e29b-41d4-a716-446655440003",
      "roomId": "660e8400-e29b-41d4-a716-446655440002",
      "topic": "Лекция 1",
      "status": "PLANNED",
      "createdAt": "2025-02-07T11:00:00",
      "updatedAt": "2025-02-07T11:00:00"
    },
    "offering": {
      "id": "880e8400-e29b-41d4-a716-446655440010",
      "groupId": "550e8400-e29b-41d4-a716-446655440000",
      "curriculumSubjectId": "440e8400-e29b-41d4-a716-446655440099",
      "teacherId": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
    },
    "slot": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "offeringId": "880e8400-e29b-41d4-a716-446655440010",
      "dayOfWeek": 1,
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "timeslotId": "770e8400-e29b-41d4-a716-446655440003",
      "lessonType": "LECTURE",
      "roomId": "660e8400-e29b-41d4-a716-446655440002",
      "teacherId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "createdAt": "2025-02-07T10:00:00"
    },
    "teachers": [
      { "teacherId": "b2c3d4e5-f6a7-8901-bcde-f12345678901", "role": "LECTURE" }
    ],
    "room": {
      "id": "660e8400-e29b-41d4-a716-446655440002",
      "number": "101",
      "buildingName": "Корпус A"
    },
    "mainTeacher": {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "displayName": "John Smith"
    },
    "subjectName": "Mathematics"
  }
]
```

---

### 8.6 GET /api/schedule/lessons/group/{groupId} — занятия группы на дату

**Запрос:** `GET /api/schedule/lessons/group/550e8400-e29b-41d4-a716-446655440000?date=2025-02-07`

**Ответ 200 OK** — массив **LessonForScheduleDto** (структура как в 8.5: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`). Если группа существует, но у неё нет офферингов или занятий на эту дату — пустой массив `[]`.

**Ответ 404 Not Found** (группа не существует) — тело **ErrorResponse** с кодом `SCHEDULE_GROUP_NOT_FOUND`, сообщение вида `"Group not found: 550e8400-e29b-41d4-a716-446655440000"`.

---

### 8.6.1 GET /api/schedule/lessons/week — занятия на неделю

**Запрос:** `GET /api/schedule/lessons/week?date=2025-02-07`

**Ответ 200 OK** — массив **LessonForScheduleDto** (структура как в 8.5: `lesson`, `offering`, `slot`, `teachers`, `room`, `mainTeacher`, `subjectName`). В выборку входят все занятия, дата которых попадает в неделю понедельник–воскресенье, содержащую указанную дату. Порядок: по дате, затем по времени начала. Если в неделе нет занятий — пустой массив `[]`.

---

### 8.6.2 GET /api/schedule/lessons/week/group/{groupId} — занятия группы на неделю

**Запрос:** `GET /api/schedule/lessons/week/group/550e8400-e29b-41d4-a716-446655440000?date=2025-02-07`

**Ответ 200 OK** — массив **LessonForScheduleDto** (структура как в 8.5). В выборку входят только занятия офферингов указанной группы, дата которых попадает в неделю понедельник–воскресенье. Порядок: по дате, затем по времени начала. Если группа существует, но у неё нет офферингов или занятий в эту неделю — пустой массив `[]`.

**Ответ 404 Not Found** (группа не существует) — тело **ErrorResponse** с кодом `SCHEDULE_GROUP_NOT_FOUND`, сообщение вида `"Group not found: 550e8400-e29b-41d4-a716-446655440000"`.

---

### 8.7 Ошибка 409 — занятие уже существует

**Запрос:** повторный `POST /api/schedule/lessons` с тем же `offeringId`, `date`, `startTime`, `endTime`.

**Ответ 409 Conflict:**

```json
{
  "code": "SCHEDULE_LESSON_ALREADY_EXISTS",
  "message": "Lesson already exists for this offering, date and time",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### 8.8 Ошибка 409 — нельзя удалить здание с аудиториями

**Запрос:** `DELETE /api/schedule/buildings/550e8400-e29b-41d4-a716-446655440001` (в здании есть комнаты).

**Ответ 409 Conflict:**

```json
{
  "code": "SCHEDULE_BUILDING_HAS_ROOMS",
  "message": "Building has rooms; delete or reassign rooms first",
  "timestamp": "2025-02-07T12:00:00.123Z",
  "details": null
}
```

---

### 8.9 GET /api/academic/semesters/by-date — семестр по дате

**Запрос:** `GET /api/academic/semesters/by-date?date=2025-02-07`

**Ответ 200 OK:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "academicYearId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "number": 1,
  "name": "Осенний семестр 2024/25",
  "startDate": "2024-09-01",
  "endDate": "2024-12-31",
  "examStartDate": "2025-01-09",
  "examEndDate": "2025-01-25",
  "weekCount": 16,
  "isCurrent": true,
  "createdAt": "2024-08-15T10:00:00"
}
```

**Ответ 404 Not Found** (дата не попадает ни в один семестр): тело **ErrorResponse** с кодом `NOT_FOUND`, сообщение вида «Semester not found for date: 2025-08-15» или общее.
