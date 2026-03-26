# Deployment guide (test_deploy, Caddy)

## 1. VPS prerequisites

- Ubuntu/Debian VPS with open ports `80` and `443`
- Docker Engine + Docker Compose plugin
- Domain A-record pointing to VPS IP
- SSH access by key (`root` in current workflow)
- Caddy already installed and running on VPS

## 2. GitHub branch

- Deployment is triggered on push to `test_deploy`
- Branch was created in this repository

## 3. Files used for deploy

- `docker-compose.test-deploy.yml` - backend + dependencies for VPS test deploy (Caddy on host)
- `deploy/caddy/Caddyfile.interhub.template` - Caddy site template for this project
- `.github/workflows/deploy-backend-test.yml` - backend deploy pipeline
- `.env.example` - required server environment contract

## 4. GitHub Secrets (Repository -> Settings -> Secrets and variables -> Actions)

### Required secrets

- `VPS_HOST` - VPS IP or DNS name
- `VPS_USER` - SSH user (currently `root`)
- `VPS_SSH_KEY` - private SSH key (PEM)
- `VPS_SSH_PORT` - SSH port (usually `22`)
- `DEPLOY_PATH` - deploy root path on server, e.g. `/opt/interhub`
- `DB_PASSWORD`
- `JWT_SECRET`
- `ADMIN_PASSWORD`
- `REDIS_PASSWORD`
- `MINIO_SECRET_KEY`
- `MINIO_ACCESS_KEY`
- `MAIL_PASSWORD`

### Recommended variables (non-secret)

- `PRIMARY_DOMAIN` - e.g. `app.example.com`
- `APP_BASE_URL` - e.g. `https://app.example.com`
- `JWT_PASSWORD_RESET_BASE_URL` - e.g. `https://app.example.com/reset-password`
- `BACKEND_HOST_PORT` - e.g. `18080` (must be free on VPS)
- `DB_NAME` - e.g. `interhubdev`
- `DB_USER` - e.g. `interhubdev`
- `ADMIN_EMAIL`
- `ADMIN_FIRST_NAME`
- `ADMIN_LAST_NAME`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_FROM`
- `MAIL_FROM_NAME`
- `MAIL_ENABLED` - `true`/`false`
- `MAIL_LOG_ONLY` - `true`/`false`
- `JWT_CORS_ALLOWED_ORIGINS` - e.g. `https://app.example.com`

## 5. Caddy configuration for second project (no conflict)

1. Keep existing Caddy site block for your current project unchanged.
2. Add a separate site block for InterHub on a different domain (example):

```caddyfile
app.dev.newzer.ru {
    encode gzip

    route {
        handle /api/* {
            reverse_proxy http://127.0.0.1:18080
        }
        handle {
            root * /opt/interhub/frontend-dist
            try_files {path} /index.html
            file_server
        }
    }
}
```

**`route`, `handle`, префикс `/api`**

- **`route { ... }`** задаёт **линейный** порядок: сначала то, что вы написали выше, затем ниже. Именно так гарантируется приоритет API над SPA fallback (`try_files` / `index.html`).
- **`handle /api/*`** матчится по **пути запроса** (как пришёл клиент). **`reverse_proxy`** внутри по умолчанию передаёт на Spring **тот же путь**, т.е. с префиксом **`/api/...`**. Так и должно быть, если приложение реально обслуживает URI вида `/api/...` (контроллеры или общий префикс под `/api`).
- **`handle_path /api/*`** перед проксированием **снимает** совпавший префикс: upstream увидит путь **без** ведущего `/api`. Используйте только если контроллеры у вас с корня **`/...`**, а `/api` — чисто внешний префикс у Caddy.

3. Reload Caddy:

```bash
sudo systemctl reload caddy
```

This avoids conflicts:
- old project keeps `127.0.0.1:8080`
- InterHub backend uses `127.0.0.1:18080`

## 6. Frontend delivery contract

Frontend workflow from `interhubfront` uploads `dist` to Caddy static root.
Recommended path: `/opt/interhub/frontend-dist`.
