# Контракт API: студенческие группы и кураторы

Документ-контракт для фронтенда. По нему можно реализовать создание и управление студенческими группами и назначение куратора группы (им может быть любой пользователь: администратор, сотрудник, учитель). Описаны структуры запросов и ответов всех эндпоинтов, правила валидации и возможные ошибки.

---

## 1. Общие сведения

### 1.1 Базовый URL и префиксы

| Префикс | Назначение |
|---------|-------------|
| `/api` | Общий префикс REST API |
| `/api/groups` | Студенческие группы, старосты группы, оверрайды учебного плана группы |
| `/api/account` | Учёт и профили; списки учителей и студентов (в т.ч. для выбора куратора) |

Хост и порт задаются окружением развёртывания. В описании ниже указаны только путь и query.

### 1.2 Авторизация

- Запросы к описанным эндпоинтам выполняются с заголовком: **`Authorization: Bearer <JWT>`** (токен выдаётся при входе).
- Эндпоинты, которые **изменяют данные** (POST, PUT, DELETE), доступны только пользователям с одной из ролей: **MODERATOR**, **ADMIN**, **SUPER_ADMIN**.
- Эндпоинты только для чтения (GET) в этом документе не помечены отдельной ролью; при необходимости уточните политику доступа в проекте.

### 1.3 Типы данных в ответах

- **UUID** — строка в формате UUID, например: `"550e8400-e29b-41d4-a716-446655440000"`.
- **Даты и время** — строки в формате **ISO-8601**: дата-время вида `"2025-02-05T12:00:00"`, дата вида `"2025-02-05"`.
- **Числа** — JSON number; для целых полей (год, количество недель) — целое число.
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
  "timestamp": "2025-02-05T12:00:00.123Z",
  "details": {
    "code": "Code is required",
    "startYear": "startYear must be at least 1900"
  }
}
```

Пример ответа с ошибкой «не найдено» (404):

```json
{
  "code": "NOT_FOUND",
  "message": "Group not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-02-05T12:00:00.123Z",
  "details": null
}
```

---

## 2. Выбор куратора группы

Куратором группы может быть **любой пользователь** (администратор, сотрудник, учитель). В запросах создания и обновления группы передаётся **id пользователя** (`curatorUserId`). Для списка кандидатов в кураторы можно использовать: **GET /api/account/users** (все пользователи) или **GET /api/account/teachers** (только учителя; в этом случае в `curatorUserId` подставляйте `profile.userId`).

### 2.1 GET /api/account/teachers

**Назначение:** постраничное получение списка учителей (подмножество пользователей; удобно для выпадающего списка «Ответственный за группу», если кураторами назначают только учителей).

**Метод и путь:** `GET /api/account/teachers`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `cursor` | UUID | нет | Курсор пагинации: значение `nextCursor` из предыдущего ответа. Для первой страницы параметр не передавать. |
| `limit`  | integer | нет | Максимальное количество элементов на странице. По умолчанию 30, верхняя граница 30 (значения больше 30 трактуются как 30). |

**Пример запроса первой страницы:**  
`GET /api/account/teachers?limit=30`

**Пример запроса следующей страницы:**  
`GET /api/account/teachers?cursor=<значение nextCursor из предыдущего ответа>&limit=30`

**Успешный ответ:** `200 OK`

Тело ответа — объект **TeacherListPage**:

| Поле | Тип | Описание |
|------|-----|----------|
| `items` | array | Массив объектов **TeacherProfileItem** |
| `nextCursor` | UUID \| null | Если не `null`, следующая страница запрашивается с `cursor=nextCursor`. Если `null`, это последняя страница. |

**Структура элемента массива items (TeacherProfileItem):**

| Поле | Тип | Описание |
|------|-----|----------|
| `profile` | object | Профиль учителя — объект **TeacherDto** |
| `displayName` | string | Отображаемое имя для UI: приоритетно `englishName` учителя, иначе ФИО пользователя |

**Структура объекта TeacherDto (profile):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля учителя |
| `userId` | UUID | **Id пользователя (аккаунта).** Это значение передаётся в поле `curatorUserId` при создании (POST /api/groups) и обновлении (PUT /api/groups/{id}) группы, если куратор выбран из списка учителей. |
| `teacherId` | string \| null | Внешний идентификатор учителя |
| `faculty` | string \| null | Факультет |
| `englishName` | string \| null | Имя на английском |
| `position` | string \| null | Должность |
| `createdAt` | string | Дата и время создания профиля (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

**Для куратора группы:** при выборе куратора из списка учителей подставляйте в `curatorUserId` значение **`profile.userId`**. Для выбора куратора из всех пользователей используйте **GET /api/account/users** (см. контракт учётного модуля); в `curatorUserId` передаётся `id` пользователя из ответа.

**Ошибки:** при отсутствии прав — `403 Forbidden` (код `FORBIDDEN`); при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

---

## 3. Студенческие группы (CRUD)

### 3.1 Модель данных группы (StudentGroupDto)

Во всех успешных ответах, где возвращается группа (или список групп), используется объект **StudentGroupDto**:

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Уникальный идентификатор группы |
| `programId` | UUID | Id образовательной программы |
| `curriculumId` | UUID | Id учебного плана (curriculum), привязанного к группе |
| `code` | string | Код группы (уникален в системе) |
| `name` | string \| null | Название группы |
| `description` | string \| null | Описание |
| `startYear` | integer | Год начала (диапазон 1900–2100) |
| `graduationYear` | integer \| null | Год выпуска |
| `curatorUserId` | UUID \| null | Id пользователя-куратора (любой пользователь: администратор, сотрудник, учитель); при отсутствии куратора — `null` |
| `createdAt` | string | Дата и время создания записи (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

---

### 3.2 GET /api/groups — получить все группы

**Метод и путь:** `GET /api/groups`

**Параметры:** нет.

**Успешный ответ:** `200 OK`

Тело — массив объектов **StudentGroupDto**. Порядок: по `programId`, затем по `code`.

---

### 3.3 GET /api/groups/{id} — получить группу по id

**Метод и путь:** `GET /api/groups/{id}`

**Параметр пути:** `id` — UUID группы.

**Успешный ответ:** `200 OK` — один объект **StudentGroupDto**.

**Ошибка:** `404 Not Found` — группа с указанным id не найдена. В теле ошибки код `NOT_FOUND`, сообщение вида `"Group not found: <id>"`.

---

### 3.4 GET /api/groups/code/{code} — получить группу по коду

**Метод и путь:** `GET /api/groups/code/{code}`

**Параметр пути:** `code` — строковый код группы (например, `CS-2024-1`).

**Успешный ответ:** `200 OK` — один объект **StudentGroupDto**.

**Ошибка:** `404 Not Found` — группа с таким кодом не найдена.

---

### 3.5 GET /api/groups/program/{programId} — получить группы по программе

**Метод и путь:** `GET /api/groups/program/{programId}`

**Параметр пути:** `programId` — UUID образовательной программы.

**Успешный ответ:** `200 OK` — массив объектов **StudentGroupDto** (только группы данной программы). Порядок по `code`.

---

### 3.6 POST /api/groups — создать группу

**Метод и путь:** `POST /api/groups`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Заголовки:** `Content-Type: application/json`

**Тело запроса:** объект **CreateGroupRequest** (JSON).

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `programId` | UUID | да | Id образовательной программы |
| `curriculumId` | UUID | да | Id учебного плана (curriculum) |
| `code` | string | да | Код группы; не пустая строка; после обрезки пробелов должен быть уникален в системе |
| `name` | string | нет | Название группы |
| `description` | string | нет | Описание |
| `startYear` | integer | да | Год начала обучения; допустимый диапазон 1900–2100 |
| `graduationYear` | integer | нет | Год выпуска |
| `curatorUserId` | UUID | нет | Id пользователя-куратора (из GET /api/account/users или `profile.userId` из GET /api/account/teachers); при отсутствии — не передавать или передать `null` |

**Успешный ответ:** `201 Created`

Тело — созданный объект **StudentGroupDto** (с заполненными `id`, `createdAt`, `updatedAt`).

**Возможные ошибки:**

- `400 Bad Request` — неверные или отсутствующие обязательные данные. Примеры сообщений: `"Program id is required"`, `"Curriculum id is required"`, `"Code is required"`, `"startYear must be at least 1900"`, `"startYear must be at most 2100"`. При аннотационной валидации полей код может быть `VALIDATION_FAILED`, в `details` — объект «поле → сообщение».
- `404 Not Found` — не найдена программа, учебный план или пользователь-куратор. Сообщения: `"Program not found: <programId>"`, `"Curriculum not found: <curriculumId>"`, `"User not found: <curatorUserId>"`.
- `409 Conflict` — группа с таким кодом уже существует. Сообщение: `"Group with code '<code>' already exists"` (в кавычках подставлен обрезанный код).

---

### 3.7 PUT /api/groups/{id} — обновить группу

**Метод и путь:** `PUT /api/groups/{id}`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `id` — UUID группы.

**Тело запроса:** объект **UpdateGroupRequest** (JSON). Все поля опциональны; изменяются только переданные поля.

| Поле | Тип | Описание |
|------|-----|----------|
| `name` | string \| null | Название группы; `null` или отсутствие поля — не менять |
| `description` | string \| null | Описание |
| `graduationYear` | integer \| null | Год выпуска |
| `curatorUserId` | UUID \| null | Id пользователя-куратора; `null` — снять куратора с группы |

**Успешный ответ:** `200 OK` — обновлённый объект **StudentGroupDto**.

**Возможные ошибки:**

- `404 Not Found` — группа не найдена (`"Group not found: <id>"`) или указанный пользователь не найден (`"User not found: <curatorUserId>"`).
- `400 Bad Request` — неверные данные (если применимо).

---

### 3.8 DELETE /api/groups/{id} — удалить группу

**Метод и путь:** `DELETE /api/groups/{id}`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `id` — UUID группы.

**Успешный ответ:** `204 No Content` (тело ответа пустое).

**Ошибка:** `404 Not Found` — группа не найдена (`"Group not found: <id>"`).

---

## 4. Состав группы (Members) — n:m

Студент может состоять в нескольких группах. Связь «студент–группа» хранится в таблице членства; состав группы и список групп студента задаются через эндпоинты ниже.

### 4.1 GET /api/groups/{groupId}/members — список участников группы

**Метод и путь:** `GET /api/groups/{groupId}/members`

**Параметр пути:** `groupId` — UUID группы.

**Успешный ответ:** `200 OK` — массив объектов **GroupMemberDto**. Каждый элемент содержит профиль студента и связанного пользователя (для отображения имени, email и т.д.).

**Структура элемента (GroupMemberDto):**

| Поле | Тип | Описание |
|------|-----|----------|
| `student` | object | Профиль студента — объект **StudentDto** (см. ниже) |
| `user` | object | Учётная запись пользователя — объект **UserDto** (см. раздел 9.1; поля `id`, `email`, `firstName`, `lastName`, `phone`, `birthDate`, `status`, `roles`, `createdAt`, `activatedAt`, `lastLoginAt`) |

**StudentDto (поле student):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id профиля студента |
| `userId` | UUID | Id пользователя (тот же, что в `user.id`) |
| `studentId` | string \| null | Внешний идентификатор студента |
| `chineseName` | string \| null | Имя на китайском |
| `faculty` | string \| null | Факультет |
| `course` | string \| null | Курс |
| `enrollmentYear` | integer \| null | Год поступления |
| `groupName` | string \| null | Название группы (опционально) |
| `createdAt` | string | Дата создания профиля (ISO-8601) |
| `updatedAt` | string | Дата обновления (ISO-8601) |

**Для отображения имени** используйте `user.firstName`, `user.lastName`; при отсутствии — `user.email` или `student.chineseName`.

**Ошибка:** `404 Not Found` — группа не найдена.

### 4.2 POST /api/groups/{groupId}/members — добавить студента в группу

**Метод и путь:** `POST /api/groups/{groupId}/members`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `groupId` — UUID группы.

**Тело запроса:** объект **AddGroupMemberRequest** (JSON). Поле `studentId` (UUID) обязательно — id профиля студента (students.id). При валидации: `"Student id is required"` при отсутствии.

Пример: `{ "studentId": "d4e5f6a7-b8c9-0123-def0-234567890123" }`

**Успешный ответ:** `204 No Content`. Повторный запрос с той же парой (студент уже в группе) тоже возвращает 204 (идемпотентность).

**Ошибки:** `404 Not Found` — группа не найдена. При несуществующем студенте бэкенд может вернуть 400/404 с сообщением вида `"Student not found: <studentId>"` или `"Group not found: <groupId>"`.

### 4.3 POST /api/groups/{groupId}/members/bulk — массовое добавление студентов в группу

**Метод и путь:** `POST /api/groups/{groupId}/members/bulk`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `groupId` — UUID группы.

**Тело запроса:** объект с полем `studentIds` (обязательное) — массив UUID (id профилей студентов, students.id). Пустой массив допустим (no-op). При валидации: `"Student ids are required"` при отсутствии поля.

Пример: `{ "studentIds": [ "d4e5f6a7-b8c9-0123-def0-234567890123", "e5f6a7b8-c9d0-1234-ef01-345678901234" ] }`

**Успешный ответ:** `204 No Content`. Для каждого студента из списка: если он ещё не в группе — добавляется; если уже в группе — пропускается (идемпотентность по каждому студенту).

**Ошибки:** `404 Not Found` — группа не найдена. `400 Bad Request` — при отсутствии поля `studentIds` (VALIDATION_FAILED, `"Student ids are required"`) или при несуществующем студенте в списке (`"Student not found: <studentId>"`, `"Group not found: <groupId>"`).

### 4.4 DELETE /api/groups/{groupId}/members/{studentId} — исключить студента из группы

**Метод и путь:** `DELETE /api/groups/{groupId}/members/{studentId}`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметры пути:** `groupId` — UUID группы, `studentId` — UUID профиля студента (students.id).

**Успешный ответ:** `204 No Content`.

**Ошибка:** `404 Not Found` — группа не найдена (при отсутствии студента в группе удаление всё равно считается успешным).

---

## 5. Старосты группы (Group Leaders)

Староста и заместитель — роли студентов в группе. У одной группы может быть несколько записей (например, один староста и один заместитель; комбинация группа–студент–роль уникальна). Студент должен состоять в группе (через members), чтобы его можно было назначить старостой.

### 5.1 Модель данных записи старосты (GroupLeaderDto и GroupLeaderDetailDto)

При **GET /api/groups/{groupId}/leaders** возвращается массив **GroupLeaderDetailDto**: запись старосты вместе с полными данными студента и пользователя (для отображения имени в UI).

**GroupLeaderDetailDto** (ответ GET /api/groups/{groupId}/leaders):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id записи старосты |
| `groupId` | UUID | Id группы |
| `studentId` | UUID | Id студента (профиль студента) |
| `role` | string | Роль: `"headman"` (староста) или `"deputy"` (заместитель) |
| `fromDate` | string \| null | Дата начала полномочий (LocalDate, ISO-8601) |
| `toDate` | string \| null | Дата окончания полномочий (LocalDate, ISO-8601) |
| `createdAt` | string | Дата и время создания записи (ISO-8601) |
| `student` | object \| null | Профиль студента — **StudentDto** (структура как в п. 4.1); может быть `null`, если профиль недоступен |
| `user` | object \| null | Учётная запись пользователя — **UserDto** (раздел 9.1; имя, email и т.д.); может быть `null`, если пользователь недоступен |

**GroupLeaderDto** — используется в ответе **POST /api/groups/{groupId}/leaders** (создание записи): те же поля `id`, `groupId`, `studentId`, `role`, `fromDate`, `toDate`, `createdAt` без вложенных `student` и `user`.

**Для отображения имени старосты** в списке используйте `user.firstName`, `user.lastName` или `user.email`, либо `student.chineseName`.

---

### 5.2 GET /api/groups/{groupId}/leaders — список старост группы

**Метод и путь:** `GET /api/groups/{groupId}/leaders`

**Параметр пути:** `groupId` — UUID группы.

**Успешный ответ:** `200 OK` — массив объектов **GroupLeaderDetailDto** (запись старосты + вложенные `student` и `user` для отображения ФИО и контактов). Порядок: по роли, затем по дате создания.

---

### 5.3 POST /api/groups/{groupId}/leaders — добавить старосту/заместителя

**Метод и путь:** `POST /api/groups/{groupId}/leaders`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `groupId` — UUID группы.

**Тело запроса:** объект **AddGroupLeaderRequest** (JSON).

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `studentId` | UUID | да | Id студента (профиль студента) |
| `role` | string | да | Роль: только `headman` или `deputy`; регистр не важен (на бэкенде приводится к нижнему) |
| `fromDate` | string | нет | Дата начала (формат даты ISO-8601, например `"2025-02-05"`) |
| `toDate` | string | нет | Дата окончания (формат даты ISO-8601) |

**Успешный ответ:** `201 Created` — созданный объект **GroupLeaderDto** (с заполненными `id`, `groupId`, `createdAt`).

**Возможные ошибки:**

- `400 Bad Request` — не указан studentId или role; либо роль не из разрешённых. Сообщение: `"Role must be headman or deputy"` или сообщения валидации по полям (например, `"Student id is required"`, `"Role is required"`).
- `404 Not Found` — группа или студент не найдены. Сообщения: `"Group not found: <groupId>"`, `"Student not found: <studentId>"`.
- `409 Conflict` — для данной группы и студента уже есть запись с такой ролью. Сообщение: `"Leader with this role already exists for group/student"`.

---

### 5.4 DELETE /api/groups/leaders/{id} — удалить запись старосты

**Метод и путь:** `DELETE /api/groups/leaders/{id}`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `id` — UUID записи старосты (поле `id` из **GroupLeaderDto**).

**Успешный ответ:** `204 No Content`.

**Ошибка:** `404 Not Found` — запись не найдена (`"Group leader not found: <id>"`).

---

## 6. Оверрайды учебного плана группы (Curriculum Overrides)

Оверрайд — изменение учебного плана для конкретной группы: добавление, удаление или замена элемента плана (предмета, типа аттестации, длительности и т.п.).

### 6.1 Модель данных оверрайда (GroupCurriculumOverrideDto)

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | Id оверрайда |
| `groupId` | UUID | Id группы |
| `curriculumSubjectId` | UUID \| null | Id элемента учебного плана (предмет в плане); для действия REMOVE/REPLACE |
| `subjectId` | UUID \| null | Id предмета (subject); для действия ADD |
| `action` | string | Действие: `"ADD"`, `"REMOVE"` или `"REPLACE"` |
| `newAssessmentTypeId` | UUID \| null | Id нового типа аттестации (при необходимости) |
| `newDurationWeeks` | integer \| null | Новая длительность в неделях |
| `reason` | string \| null | Причина оверрайда |
| `createdAt` | string | Дата и время создания (ISO-8601) |

---

### 6.2 GET /api/groups/{groupId}/overrides — список оверрайдов группы

**Метод и путь:** `GET /api/groups/{groupId}/overrides`

**Параметр пути:** `groupId` — UUID группы.

**Успешный ответ:** `200 OK` — массив объектов **GroupCurriculumOverrideDto**. Порядок: по дате создания, новые первые.

---

### 6.3 POST /api/groups/{groupId}/overrides — создать оверрайд

**Метод и путь:** `POST /api/groups/{groupId}/overrides`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `groupId` — UUID группы.

**Тело запроса:** объект **CreateOverrideRequest** (JSON).

| Поле | Тип | Обязательный | Описание |
|------|-----|--------------|----------|
| `action` | string | да | Действие: `ADD`, `REMOVE` или `REPLACE`; регистр не важен (на бэкенде — верхний) |
| `curriculumSubjectId` | UUID | для REMOVE и REPLACE | Id элемента учебного плана; обязателен для действий REMOVE и REPLACE |
| `subjectId` | UUID | для ADD | Id предмета; обязателен для действия ADD |
| `newAssessmentTypeId` | UUID | нет | Id нового типа аттестации (при замене и т.п.) |
| `newDurationWeeks` | integer | нет | Новая длительность в неделях |
| `reason` | string | нет | Причина оверрайда |

**Правила по действиям:**

- **REMOVE** — удаление элемента плана для группы. Обязательно передать `curriculumSubjectId`.
- **ADD** — добавление предмета в план группы. Обязательно передать `subjectId`.
- **REPLACE** — замена элемента плана (например, тип аттестации или длительность). Обязательно передать `curriculumSubjectId`; при необходимости — `newAssessmentTypeId`, `newDurationWeeks`, `reason`.

**Успешный ответ:** `201 Created` — созданный объект **GroupCurriculumOverrideDto** (с заполненными `id`, `groupId`, `action` в нормализованном виде, `createdAt`).

**Возможные ошибки:**

- `400 Bad Request` — не указано action или нарушены правила по действиям. Сообщения: `"Action is required"`, `"Action must be ADD, REMOVE, or REPLACE"`, `"curriculumSubjectId is required for REMOVE"`, `"subjectId is required for ADD"`, `"curriculumSubjectId is required for REPLACE"`.
- `404 Not Found` — группа не найдена (`"Group not found: <groupId>"`).

---

### 6.4 DELETE /api/groups/overrides/{id} — удалить оверрайд

**Метод и путь:** `DELETE /api/groups/overrides/{id}`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Параметр пути:** `id` — UUID оверрайда (поле `id` из **GroupCurriculumOverrideDto**).

**Успешный ответ:** `204 No Content`.

**Ошибка:** `404 Not Found` — оверрайд не найден (`"Override not found: <id>"`).

---

## 7. Сводка кодов ошибок и HTTP-статусов

| HTTP | code | Когда возникает |
|------|------|------------------|
| 400 | BAD_REQUEST | Неверные или отсутствующие данные (программа, учебный план, код, год, роль, action, обязательные поля для REMOVE/ADD/REPLACE) |
| 400 | VALIDATION_FAILED | Ошибка аннотационной валидации; в `details` — объект «поле → сообщение» |
| 401 | UNAUTHORIZED | Нет или невалидный JWT |
| 403 | FORBIDDEN | Недостаточно прав (нет роли MODERATOR/ADMIN/SUPER_ADMIN) |
| 404 | NOT_FOUND | Группа, программа, учебный план, пользователь-куратор, студент, запись старосты или оверрайд не найдены (конкретное сообщение в `message`) |
| 409 | CONFLICT | Группа с таким кодом уже существует; или для группы/студента уже есть запись старосты с такой ролью |

Точные формулировки сообщений для групп, учителей, старост и оверрайдов приведены в соответствующих разделах выше.

---

## 8. Примеры запросов и ответов

### 8.1 GET /api/account/teachers — ответ 200 (фрагмент)

**Запрос:** `GET /api/account/teachers?limit=2`

**Ответ 200 OK (сокращённо):**

```json
{
  "items": [
    {
      "profile": {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "teacherId": "T-001",
        "faculty": "ФИТ",
        "englishName": "Ivan Petrov",
        "position": "доцент",
        "createdAt": "2024-01-15T10:00:00",
        "updatedAt": "2025-02-01T12:00:00"
      },
      "displayName": "Ivan Petrov"
    }
  ],
  "nextCursor": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

Для куратора группы в `curatorUserId` подставляется `profile.userId`: `"b2c3d4e5-f6a7-8901-bcde-f12345678901"` (из примера выше — userId первого учителя).

---

### 8.2 POST /api/groups — тело запроса и ответ 201

**Запрос:** `POST /api/groups`  
**Тело:**

```json
{
  "programId": "550e8400-e29b-41d4-a716-446655440001",
  "curriculumId": "550e8400-e29b-41d4-a716-446655440002",
  "code": "CS-2024-1",
  "name": "Поток 1, 2024",
  "description": "Первая группа набора 2024",
  "startYear": 2024,
  "graduationYear": 2028,
  "curatorUserId": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

**Ответ 201 Created:**

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440010",
  "programId": "550e8400-e29b-41d4-a716-446655440001",
  "curriculumId": "550e8400-e29b-41d4-a716-446655440002",
  "code": "CS-2024-1",
  "name": "Поток 1, 2024",
  "description": "Первая группа набора 2024",
  "startYear": 2024,
  "graduationYear": 2028,
  "curatorUserId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "createdAt": "2025-02-05T12:00:00",
  "updatedAt": "2025-02-05T12:00:00"
}
```

---

### 8.3 PUT /api/groups/{id} — частичное обновление

**Запрос:** `PUT /api/groups/660e8400-e29b-41d4-a716-446655440010`  
**Тело (только смена куратора и года выпуска):**

```json
{
  "graduationYear": 2029,
  "curatorUserId": "c3d4e5f6-a7b8-9012-cdef-123456789012"
}
```

**Ответ 200 OK** — полный объект **StudentGroupDto** с обновлёнными полями.

---

### 8.4 POST /api/groups/{groupId}/members — добавить студента в группу

**Тело:** `{ "studentId": "d4e5f6a7-b8c9-0123-def0-234567890123" }`  
**Ответ:** `204 No Content`

### 8.5 POST /api/groups/{groupId}/members/bulk — массовое добавление

**Тело:** `{ "studentIds": [ "d4e5f6a7-b8c9-0123-def0-234567890123", "e5f6a7b8-c9d0-1234-ef01-345678901234" ] }`  
**Ответ:** `204 No Content`

### 8.6 POST /api/groups/{groupId}/leaders

**Запрос:** `POST /api/groups/660e8400-e29b-41d4-a716-446655440010/leaders`  
**Тело:**

```json
{
  "studentId": "d4e5f6a7-b8c9-0123-def0-234567890123",
  "role": "headman",
  "fromDate": "2025-02-01",
  "toDate": "2028-06-30"
}
```

**Ответ 201 Created** — объект **GroupLeaderDto** с заполненными `id`, `groupId`, `studentId`, `role`, `fromDate`, `toDate`, `createdAt`.

---

### 8.7 POST /api/groups/{groupId}/overrides — действия REMOVE, ADD, REPLACE

**REMOVE (удаление предмета из плана для группы):**

```json
{
  "action": "REMOVE",
  "curriculumSubjectId": "770e8400-e29b-41d4-a716-446655440020",
  "reason": "Предмет перенесён в другой семестр"
}
```

**ADD (добавление предмета):**

```json
{
  "action": "ADD",
  "subjectId": "880e8400-e29b-41d4-a716-446655440021",
  "newAssessmentTypeId": "990e8400-e29b-41d4-a716-446655440022",
  "newDurationWeeks": 14,
  "reason": "Дополнительный модуль по выбору"
}
```

**REPLACE (замена типа аттестации/длительности):**

```json
{
  "action": "REPLACE",
  "curriculumSubjectId": "770e8400-e29b-41d4-a716-446655440020",
  "newAssessmentTypeId": "990e8400-e29b-41d4-a716-446655440022",
  "newDurationWeeks": 16,
  "reason": "Изменение формы контроля"
}
```

В каждом случае ответ **201 Created** — объект **GroupCurriculumOverrideDto** с заполненными `id`, `groupId`, `action`, `createdAt` и переданными полями.

---

## 9. Список всех пользователей (для назначения куратора группы)

Эндпоинт возвращает постраничный список **всех пользователей** системы. Используется на фронтенде для выбора ответственного за группу (куратора): в поле `curatorUserId` при создании/обновлении группы подставляется **`id`** пользователя из этого списка.

### 9.1 GET /api/account/users

**Назначение:** постраничное получение списка пользователей (курсорная пагинация). Для выпадающего списка «Ответственный за группу» — когда куратором может быть любой пользователь (администратор, сотрудник, учитель).

**Метод и путь:** `GET /api/account/users`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `cursor` | UUID | нет | Курсор пагинации: значение `nextCursor` из предыдущего ответа. Для первой страницы параметр не передавать. |
| `limit`  | integer | нет | Максимальное количество элементов на странице. По умолчанию 30, верхняя граница 30 (значения больше 30 трактуются как 30). |

**Пример запроса первой страницы:**  
`GET /api/account/users?limit=30`

**Пример запроса следующей страницы:**  
`GET /api/account/users?cursor=<значение nextCursor из предыдущего ответа>&limit=30`

**Успешный ответ:** `200 OK`

Тело ответа — объект **UserPage**:

| Поле | Тип | Описание |
|------|-----|----------|
| `items` | array | Массив объектов **UserDto** |
| `nextCursor` | UUID \| null | Если не `null`, следующая страница запрашивается с `cursor=nextCursor`. Если `null`, это последняя страница. |

**Структура элемента массива items (UserDto):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | **Id пользователя.** Это значение передаётся в поле `curatorUserId` при создании (POST /api/groups) и обновлении (PUT /api/groups/{id}) группы. |
| `email` | string | Email пользователя (уникален) |
| `roles` | array of string | Роли пользователя. Возможные значения: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `TEACHER`, `STAFF`, `STUDENT`. У пользователя не более одной из ролей: STAFF, MODERATOR, ADMIN, SUPER_ADMIN. |
| `status` | string | Статус аккаунта: `PENDING` (приглашён, ещё не активировал), `ACTIVE` (может входить), `DISABLED` (отключён администратором) |
| `firstName` | string \| null | Имя |
| `lastName` | string \| null | Фамилия |
| `phone` | string \| null | Телефон |
| `birthDate` | string \| null | Дата рождения (ISO-8601, только дата: `"1990-05-15"`) |
| `createdAt` | string | Дата и время создания учётной записи (ISO-8601) |
| `activatedAt` | string \| null | Дата и время активации (когда пользователь впервые установил пароль); при отсутствии — `null` |
| `lastLoginAt` | string \| null | Дата и время последнего входа; при отсутствии — `null` (ISO-8601) |

**Для куратора группы:** при выборе ответственного из списка пользователей подставляйте в `curatorUserId` значение **`id`** из объекта **UserDto**. Для отображения в UI можно использовать `firstName` + `lastName` или `email`.

**Ошибки:** при отсутствии прав — `403 Forbidden` (код `FORBIDDEN`); при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).

---

## 10. Список всех студентов (для добавления в группу)

Эндпоинт возвращает постраничный список **всех студентов** системы. Используется на фронтенде при добавлении участников в группу: при вызове POST /api/groups/{groupId}/members или POST /api/groups/{groupId}/members/bulk передаётся **id профиля студента** (`profile.id` из элемента списка), а не `userId`.

### 10.1 GET /api/account/students

**Назначение:** постраничное получение списка студентов с отображаемым именем. Для экрана «Добавить студента в группу» — выбор одного или нескольких студентов из списка.

**Метод и путь:** `GET /api/account/students`

**Права доступа:** `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.

**Query-параметры:**

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `cursor` | UUID | нет | Курсор пагинации: значение `nextCursor` из предыдущего ответа. Для первой страницы параметр не передавать. |
| `limit`  | integer | нет | Максимальное количество элементов на странице. По умолчанию 30, верхняя граница 30 (значения больше 30 трактуются как 30). |

**Пример запроса первой страницы:**  
`GET /api/account/students?limit=30`

**Пример запроса следующей страницы:**  
`GET /api/account/students?cursor=<значение nextCursor из предыдущего ответа>&limit=30`

**Успешный ответ:** `200 OK`

Тело ответа — объект **StudentListPage**:

| Поле | Тип | Описание |
|------|-----|----------|
| `items` | array | Массив объектов **StudentProfileItem** |
| `nextCursor` | UUID \| null | Если не `null`, следующая страница запрашивается с `cursor=nextCursor`. Если `null`, это последняя страница. |

**Структура элемента массива items (StudentProfileItem):**

| Поле | Тип | Описание |
|------|-----|----------|
| `profile` | object | Профиль студента — объект **StudentDto** |
| `displayName` | string | Отображаемое имя для UI: приоритетно `chineseName` студента, иначе ФИО пользователя |

**Структура объекта StudentDto (profile):**

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | **Id профиля студента.** Это значение передаётся в поле `studentId` при добавлении в группу: POST /api/groups/{groupId}/members (тело `{ "studentId": "<id>" }`) или в массиве `studentIds` для POST /api/groups/{groupId}/members/bulk. |
| `userId` | UUID | Id пользователя (аккаунта), к которому привязан профиль студента |
| `studentId` | string \| null | Внешний идентификатор студента |
| `chineseName` | string \| null | Имя на китайском |
| `faculty` | string \| null | Факультет |
| `course` | string \| null | Курс |
| `enrollmentYear` | integer \| null | Год поступления |
| `groupName` | string \| null | Название группы (опционально; при связи студента с несколькими группами может не использоваться) |
| `createdAt` | string | Дата и время создания профиля (ISO-8601) |
| `updatedAt` | string | Дата и время последнего обновления (ISO-8601) |

**Для добавления в группу:** при выборе студента из списка используйте **`profile.id`** как `studentId` в запросе добавления участника (POST /api/groups/{groupId}/members или массив `studentIds` в bulk). В UI для отображения используйте `displayName`.

**Ошибки:** при отсутствии прав — `403 Forbidden` (код `FORBIDDEN`); при невалидном или отсутствующем JWT — `401 Unauthorized` (код `UNAUTHORIZED`).
