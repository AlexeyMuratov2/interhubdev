# Деплой в Китае: настройка почты

Для деплоя приложения в Китае Gmail и многие западные SMTP-сервисы блокируются или работают нестабильно. Рекомендуется использовать **Alibaba Cloud Direct Mail (阿里云邮件推送)** — официальный SMTP-сервис с высокой доставляемостью по китайским провайдерам.

---

## 1. Профиль и переменные окружения

### Активация профиля

При запуске приложения укажите профиль `china`:

```bash
spring.profiles.active=prod,china
```

Либо только `china` для тестирования.

### Переменные окружения

| Переменная       | Описание                                                                 | Пример                           |
|------------------|--------------------------------------------------------------------------|----------------------------------|
| `MAIL_HOST`      | SMTP-хост (по умолчанию для China: `smtpdm.aliyun.com`)                  | `smtpdm.aliyun.com`              |
| `MAIL_PORT`      | Порт (80 без SSL, 465 с SSL)                                             | `80` или `465`                   |
| `MAIL_USERNAME`  | Адрес отправителя из консоли Direct Mail                                | `noreply@your-domain.com`        |
| `MAIL_PASSWORD`  | SMTP-пароль для этого адреса                                             | —                                |
| `MAIL_FROM`      | Адрес отправителя (должен совпадать с `MAIL_USERNAME`)                   | `noreply@your-domain.com`        |
| `MAIL_FROM_NAME` | Имя отправителя в письме                                                | `InterHubDev`                    |
| `MAIL_USE_SSL`   | Использовать SSL (для порта 465)                                         | `true` при порте 465             |
| `MAIL_USE_STARTTLS` | STARTTLS (для Alibaba на порту 80 не используется)                   | `false` (дефолт для China)      |
| `MAIL_ENABLED`   | Включить отправку писем                                                 | `true`                           |
| `MAIL_LOG_ONLY`  | Только логировать письма без отправки (для отладки)                      | `false`                          |
| `APP_BASE_URL`   | Базовый URL для ссылок в письмах (сброс пароля, приглашения)             | `https://app.your-domain.cn`     |

Профиль `china` задаёт дефолты под Alibaba Direct Mail: хост, порт, отключение STARTTLS. Значения `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` **обязательно** задать в деплое.

---

## 2. Настройка Alibaba Cloud Direct Mail

### Шаг 1: Регистрация и доступ к сервису

1. Зарегистрируйтесь в [Alibaba Cloud](https://www.alibabacloud.com/).
2. Откройте Direct Mail (邮件推送): Console → Products → Direct Mail.

### Шаг 2: Верификация домена

1. В Direct Mail перейдите в **Domains** → **Add Domain**.
2. Укажите домен отправителя (например, `your-domain.com`).
3. Добавьте в DNS-записи домена TXT- и CNAME-записи, указанные Alibaba.
4. Дождитесь успешной верификации.

### Шаг 3: Создание адреса отправителя

1. Откройте **Sender Addresses** → **Create Sender Address**.
2. Укажите email (например, `noreply@your-domain.com`) и имя отправителя.
3. Сохраните адрес.

### Шаг 4: SMTP-пароль

1. Выберите созданный адрес отправителя.
2. Нажмите **Set SMTP Password**.
3. Задайте SMTP-пароль — он может отличаться от пароля аккаунта Alibaba.
4. Используйте этот пароль как `MAIL_PASSWORD`.

### Шаг 5: Конфигурация приложения

Подставьте полученные данные в переменные окружения:

```bash
MAIL_HOST=smtpdm.aliyun.com
MAIL_PORT=80
MAIL_USERNAME=noreply@your-domain.com
MAIL_PASSWORD=<ваш SMTP-пароль из консоли>
MAIL_FROM=noreply@your-domain.com
MAIL_FROM_NAME=InterHubDev
APP_BASE_URL=https://app.your-domain.cn
```

При запуске активируйте профиль `china`.

---

## 3. SSL (порт 465, опционально)

Для шифрованного соединения используйте порт 465:

```bash
MAIL_PORT=465
MAIL_USE_SSL=true
MAIL_USE_STARTTLS=false
```

Профиль `china` уже поддерживает переопределение через эти переменные.

---

## 4. Альтернативные провайдеры

Если Alibaba Cloud не подходит, можно использовать другой China-совместимый SMTP:

- **Tencent Cloud** (SES + SMTP)
- **NetEase** (корпоративная почта)
- **Maileroo**, **MXtoChina** (SMTP-реле для Китая)

Укажите их SMTP-хост, порт и учётные данные через `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`. Профиль `china` можно оставить (переменные окружения переопределят дефолты) или не использовать.

---

## 5. Ссылки

- [Alibaba Direct Mail — SMTP (Java)](https://www.alibabacloud.com/help/en/direct-mail/smtp-java)
- [Alibaba Direct Mail — Getting Started](https://www.alibabacloud.com/help/en/direct-mail/getting-started/simplified-procedure-of-sending-by-api-and-smtp)
