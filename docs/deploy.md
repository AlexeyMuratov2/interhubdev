# Деплой приложения (Docker + tar)

Тестовый деплой: сборка образа локально, экспорт в tar, загрузка на сервер и запуск полного стека (приложение + PostgreSQL, MinIO, Redis, ClamAV).

## Требования

- Локально: Docker, Maven (опционально, для сборки внутри образа)
- На сервере: Docker и Docker Compose

## 1. Локальная сборка и экспорт образа

В корне проекта:

```bash
# Собрать образ (сборка JAR выполняется внутри Docker)
docker build -t interhubdev:latest .

# Сохранить образ в tar-файл
docker save interhubdev:latest -o interhubdev.tar
```

Загрузить на сервер:

- `interhubdev.tar`
- `docker-compose.production.yaml`
- `.env` (скопировать из `.env.example` и заполнить секреты)
- каталог `clamav/` с файлом `clamd.conf`

Например, через `scp`:

```bash
scp interhubdev.tar user@server:/opt/interhubdev/
scp docker-compose.production.yaml user@server:/opt/interhubdev/
scp .env user@server:/opt/interhubdev/
scp -r clamav user@server:/opt/interhubdev/
```

## 2. Подготовка на сервере

На сервере в каталоге деплоя (например `/opt/interhubdev/`) должны лежать:

- `interhubdev.tar`
- `docker-compose.production.yaml`
- `.env`
- `clamav/clamd.conf`

Проверьте, что в `.env` заданы:

- `DB_PASSWORD` — пароль БД (совпадает с `POSTGRES_PASSWORD` в compose)
- `JWT_SECRET` — не менее 32 символов
- `ADMIN_PASSWORD` — пароль супер-админа (не менее 8 символов)
- При необходимости: `MAIL_*`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `APP_BASE_URL`

## 3. Запуск на сервере

```bash
cd /opt/interhubdev   # или ваш каталог

# Загрузить образ из tar
docker load -i interhubdev.tar

# Запустить стек (postgres, minio, redis, clamav, app)
docker compose -f docker-compose.production.yaml up -d
```

Проверка здоровья приложения:

```bash
curl http://localhost:8080/actuator/health
```

Ожидается ответ с `"status":"UP"`.

## 4. Остановка и обновление

Остановка:

```bash
docker compose -f docker-compose.production.yaml down
```

Обновление приложения (после загрузки нового `interhubdev.tar` и перезагрузки образа):

```bash
docker load -i interhubdev.tar
docker compose -f docker-compose.production.yaml up -d --force-recreate app
```

Данные PostgreSQL, MinIO, Redis и ClamAV хранятся в томах Docker и сохраняются между перезапусками.

## 5. Порты

| Сервис   | Порт  |
|----------|-------|
| App      | 8080  |
| PostgreSQL | 5432 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| Redis    | 6379  |
| ClamAV   | 3310  |

Убедитесь, что порты свободны или настроен reverse proxy (nginx и т.п.) перед приложением.
