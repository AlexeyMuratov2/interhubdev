# Локальный запуск backend из IDE

Цель: приложение запускается **из IDE**, а зависимости (PostgreSQL, Redis, MinIO, ClamAV) — в Docker.

## 1) Подготовить env

```bash
cp .env.local.example .env.local
```

При необходимости поменяй значения в `.env.local`.

## 2) Поднять зависимости

```bash
docker compose up -d
```

Используется `compose.yaml` (только зависимости, без контейнера backend).

## 3) Запустить backend из IDE

- Добавь env-file `.env.local` в Run Configuration.
- Убедись, что активен профиль `local` (`SPRING_PROFILES_ACTIVE=local`).

## 4) Проверка

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
