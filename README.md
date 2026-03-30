# InterHub backend (interhubdev)

Spring Boot API, Docker-образ и compose для VPS. Публичный репозиторий: [github.com/AlexeyMuratov2/interhubdev](https://github.com/AlexeyMuratov2/interhubdev).

## Фронтенды

| Приложение | Репозиторий | Деплой (ветка) |
|------------|-------------|----------------|
| Основной SPA | [InterHub-frontend](https://github.com/AlexeyMuratov2/InterHub-frontend) | `test_deploy` |
| Telegram Mini App | [InterHub-frontend](https://github.com/AlexeyMuratov2/InterHub-frontend) | `test_deploy_miniapp` |

## Ветки бэкенда и CI

- **`master`** — основная линия разработки (на GitHub по умолчанию может называться `master` или `main`).
- **`test_deploy`** — push → **Deploy Backend (test_deploy)** (SCP + `docker compose` на VPS).
- **`miniapp`** — push → **Deploy Backend (miniapp)** (тот же пайплайн; удобно вести рядом со вторым фронтом).

Подробно про CORS, cookies и miniapp: [docs/miniapp-frontend.md](docs/miniapp-frontend.md).

## Документация

- [DEPLOYMENT.md](DEPLOYMENT.md) — секреты, переменные, VPS.
- [docs/miniapp-frontend.md](docs/miniapp-frontend.md) — требования бэкенда для Mini App.
- Caddy: `deploy/caddy/Caddyfile.vps.example`.

## Быстрый локальный запуск

```bash
./mvnw -DskipTests spring-boot:run
```

Скопируйте `.env.example` в `.env` и подстройте переменные при необходимости.
