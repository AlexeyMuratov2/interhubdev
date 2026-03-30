# Деплой приложения (тестовый)

Один актуальный Compose-файл для стека на VPS: **`docker-compose.test-deploy.yml`**.

- **GitHub Actions** (ветка `test_deploy`): репозиторий копируется на сервер, на месте выполняется  
  `docker compose -f docker-compose.test-deploy.yml up -d --build`. Подробности — в [DEPLOYMENT.md](../DEPLOYMENT.md).
- **Локальная разработка без контейнера приложения**: только зависимости — **`compose.yaml`** (`docker compose up`).
- Отдельной «продакшен»-версии compose в репозитории нет (по запросу добавим позже).

---

## Ручной сценарий: Docker + tar (без CI)

### Требования

- Локально: Docker
- На сервере: Docker и Docker Compose plugin

### 1. Сборка и экспорт образа

Тег образа должен совпадать с `image` в `docker-compose.test-deploy.yml` (по умолчанию `interhubdev-backend:latest`).

```bash
docker build -t interhubdev-backend:latest .
docker save interhubdev-backend:latest -o interhubdev-backend.tar
```

### 2. Файлы на сервере

- `interhubdev-backend.tar`
- `docker-compose.test-deploy.yml`
- `.env` (из `.env.example`, секреты заполнить)
- каталог `clamav/` с `clamd.conf`

Пример:

```bash
scp interhubdev-backend.tar user@server:/opt/interhubdev/
scp docker-compose.test-deploy.yml user@server:/opt/interhubdev/
scp .env user@server:/opt/interhubdev/
scp -r clamav user@server:/opt/interhubdev/
```

### 3. Запуск

```bash
cd /opt/interhubdev

docker load -i interhubdev-backend.tar

docker compose -f docker-compose.test-deploy.yml up -d
```

Сборка на сервере не нужна, если образ уже загружен и тег `interhubdev-backend:latest` совпадает.

### 4. Проверка

Бэкенд по умолчанию слушает **только loopback** (например `127.0.0.1:18080`), см. `BACKEND_HOST_PORT` в `.env`.

```bash
curl http://127.0.0.1:18080/actuator/health
```

Ожидается `"status":"UP"`.

### 5. Остановка и обновление

```bash
docker compose -f docker-compose.test-deploy.yml down
```

После нового tar:

```bash
docker load -i interhubdev-backend.tar
docker compose -f docker-compose.test-deploy.yml up -d --force-recreate backend
```

### 6. Порты (текущий test-deploy)

| Назначение        | Как проброшен |
|-------------------|----------------|
| Spring Boot       | `127.0.0.1:BACKEND_HOST_PORT` → 8080 в контейнере (по умолчанию 18080) |
| PostgreSQL, MinIO, Redis, ClamAV | только сеть Docker, наружу не публикуются |

Наружу приложение обычно выводит **Caddy** на хосте (отдельная настройка).
