# Архитектура модуля Composition

Документ описывает архитектуру модуля `com.example.interhubdev.composition`. При добавлении новых эндпоинтов (use case) в этот модуль необходимо следовать описанной ниже структуре и шагам.

---

## 1. Назначение модуля

- **Read-only агрегация данных** из нескольких модулей в один ответ для сложных UI-экранов.
- Модуль **не содержит бизнес-логики** и **не изменяет данные** — только читает через API других модулей и собирает результат.
- Один эндпоинт = один use case (один экран или одна таблица на фронте).

---

## 2. Архитектурный паттерн: per-use-case services + тонкий фасад

Чтобы модуль не превращался в «свалку» эндпоинтов и один большой сервис с десятками зависимостей:

- **Каждый use case** реализован в **отдельном классе-сервисе** в `internal/` (например, `LessonFullDetailsService`, `TeacherStudentGroupsService`).
- Каждый такой сервис **инжектит только те API**, которые нужны именно этому use case (обычно 6–9 зависимостей вместо 14+ в одном классе).
- **Фасад**: публичный `CompositionApi` и `CompositionServiceImpl` в `internal/` — только делегируют вызовы в соответствующий use-case сервис (одна строка на метод).

Таким образом добавление нового эндпоинта = в основном **один новый файл** (use-case сервис) + минимальные правки в фасаде и контроллере.

---

## 3. Структура модуля

```
composition/
├── package-info.java              # @ApplicationModule, allowedDependencies, Javadoc модуля
├── CompositionApi.java           # Публичный интерфейс API (фасад)
├── *Dto.java                     # Публичные DTO ответов (в корне пакета)
└── internal/
    ├── CompositionController.java          # REST: один контроллер, тонкий слой
    ├── CompositionServiceImpl.java         # Реализация CompositionApi — только делегирование
    ├── LessonFullDetailsService.java       # Use case: полные детали урока
    ├── LessonRosterAttendanceService.java  # Use case: посещаемость по уроку
    ├── LessonHomeworkSubmissionsService.java # Use case: сдачи ДЗ по уроку
    └── TeacherStudentGroupsService.java    # Use case: группы преподавателя
```

- **Публичный контракт**: только `CompositionApi`, DTO и типы в корне пакета. Всё остальное в `internal/`.
- **Контроллер** зависит только от `CompositionApi`, вызывает его методы и возвращает `ResponseEntity` с DTO.
- **CompositionServiceImpl** не содержит логики агрегации — только инжектит use-case сервисы и вызывает у них один метод (например, `execute(...)`).

---

## 4. Правила для use-case сервисов

- **Имя класса**: по смыслу use case, суффикс `Service` (например, `LessonFullDetailsService`, `TeacherStudentGroupsService`).
- **Пакет**: `com.example.interhubdev.composition.internal` (класс без `public` — package-private).
- **Аннотации**: `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`.
- **Один метод с логикой**: обычно `execute(...)` с параметрами use case (например, `lessonId`, `requesterId`) и возвратом одного DTO.
- **Зависимости**: инжектить только те `*Api` других модулей, которые реально нужны этому use case. Не инжектить «на будущее».
- **Ошибки**: через `com.example.interhubdev.error.Errors` (например, `Errors.unauthorized(...)`, `Errors.notFound(...)`). Не создавать собственные форматы ошибок.
- **Javadoc**: кратко описать назначение сервиса (для какого экрана/таблицы).

---

## 5. Как добавить новый эндпоинт (чек-лист для агента)

При добавлении нового use case в модуль composition выполнить по порядку.

### 5.1. DTO ответа

- Если нужен новый формат ответа — создать **публичный** DTO в корне пакета `composition` (например, `XyzAggregateDto.java`).
- По возможности переиспользовать DTO других модулей внутри своего DTO (record с полями типа `LessonDto`, `StudentGroupDto` и т.д.), а не дублировать структуры.

### 5.2. Use-case сервис

- Создать новый класс в `internal/`, например `XyzAggregateService.java`.
- Аннотации: `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`.
- Инжектить **только** те API, которые нужны для этого use case.
- Реализовать один метод (например, `XyzAggregateDto execute(UUID id, UUID requesterId)`), перенести в него всю логику агрегации.
- В начале метода при необходимости проверить `requesterId` и бросить `Errors.unauthorized(...)`.
- Все «not found» и прочие ошибки — через `Errors.*`.

### 5.3. Публичный API (фасад)

- В `CompositionApi.java`: добавить метод с полным Javadoc (`@param`, `@return`, `@throws`).
- В `CompositionServiceImpl.java`: добавить поле с новым use-case сервисом и в соответствующем методе фасада — одну строку: `return xyzAggregateService.execute(...);`

### 5.4. REST

- В `CompositionController.java`: добавить один метод-обработчик (`@GetMapping` и т.д.):
  - получить `requesterId` через `authApi.getCurrentUser(request).orElseThrow(() -> Errors.unauthorized(...))`;
  - вызвать метод `CompositionApi`;
  - вернуть `ResponseEntity.ok(dto)`.
- Путь выбирать по смыслу: уроки — под `/api/composition/lessons/...`, преподаватель — под `/api/composition/teacher/...` и т.д.

### 5.5. Зависимости модуля

- Если новый use case использует API модуля, которого ещё нет в `allowedDependencies` в `package-info.java`, — добавить этот модуль в список.

### 5.6. Документация

- В `package-info.java` в секции Use Cases (если есть) можно добавить пункт про новый use case.
- При необходимости обновить контракт API для фронта (например, `docs/api-composition-contract.md`).

---

## 6. Чего не делать

- **Не добавлять логику агрегации** в `CompositionServiceImpl` — только делегирование в use-case сервисы.
- **Не добавлять новые эндпоинты** в один общий «God»-сервис с десятками зависимостей — каждый use case в отдельном сервисе.
- **Не выносить** сущности JPA, репозитории или внутренние типы в публичный API модуля.
- **Не дублировать** обработку ошибок: использовать только `Errors` / модуль `error`, единый формат ответа об ошибке.

---

## 7. Пример: что изменить при добавлении пятого use case

| Что сделать | Файл |
|-------------|------|
| Новый DTO ответа | `composition/SomeNewDto.java` (новый файл) |
| Вся логика агрегации | `internal/SomeNewService.java` (новый файл) |
| Сигнатура + Javadoc | `CompositionApi.java` (+1 метод) |
| Делегирование | `CompositionServiceImpl.java` (+1 поле, +1 строка в методе) |
| Эндпоинт | `CompositionController.java` (+1 метод с auth и вызовом API) |
| При необходимости | `package-info.java` (allowedDependencies, описание use case) |

Итог: один новый содержательный класс (use-case сервис) и минимальные точечные правки в трёх существующих файлах.
