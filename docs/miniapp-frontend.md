# Второй фронт (Telegram Mini App) и бэкенд

Один Spring Boot сервис обслуживает и основной SPA (`interhub.online`), и Mini App (`miniapp.interhub.online`), и отдельный API-хост (`api.interhub.online`), если он проксируется на тот же процесс.

## Репозитории и ветки

| Роль | Репозиторий | Ветка CI |
|------|-------------|----------|
| Mini App (статика) | [InterHub-frontend](https://github.com/AlexeyMuratov2/InterHub-frontend) | `test_deploy_miniapp` |
| Бэкенд (Docker Compose на VPS) | [interhubdev](https://github.com/AlexeyMuratov2/interhubdev) | `miniapp` или `test_deploy` |

Пуш в **`interhubdev`** ветку **`miniapp`** запускает workflow **Deploy Backend (miniapp)** — тот же `docker-compose.test-deploy.yml`, что и для `test_deploy`.

## Обязательные настройки для Mini App

### 1. CORS (иначе 403 на `OPTIONS`)

Браузер шлёт запросы с `Origin: https://miniapp.<домен>` на API (`https://api.<домен>` или тот же хост с `/api`). В **`.env` на VPS** и в **GitHub Actions → Variables** задайте:

```env
JWT_CORS_ALLOWED_ORIGINS=https://interhub.online,https://www.interhub.online,https://miniapp.interhub.online
```

Без origin miniapp-домена префлайт к `/api/auth/login` получит **403**.

### 2. Прокси

```env
JWT_TRUST_PROXY=true
```

Caddy передаёт `X-Forwarded-*`; иначе лимиты и IP могут вести себя неверно.

### 3. Cookies

По умолчанию: **host-only** cookie на хосте API, `SameSite` из `JWT_COOKIE_SAME_SITE` (по умолчанию `Strict`). Для поддоменов одного сайта (`*.interhub.online`) это обычно достаточно.

При необходимости общего cookie на все поддомены:

```env
JWT_COOKIE_DOMAIN=.interhub.online
```

Согласуйте с политикой безопасности; при кросс-сайтовых сценариях может понадобиться `JWT_COOKIE_SAME_SITE=None` и `JWT_COOKIE_SECURE=true` (в `prod` уже включается secure).

### 4. Caddy

Эталон блоков: `deploy/caddy/Caddyfile.vps.example` (в этом репозитории).

## Синхронизация ветки `miniapp` с основной линией

На машине разработчика (пример, если основная ветка на GitHub — `master`):

```bash
git fetch origin
git checkout master
git pull origin master
git checkout miniapp
git merge master -m "sync miniapp with master"
git push origin miniapp
```

Если вы переименуете `master` в `main`, замените имя ветки в командах.
