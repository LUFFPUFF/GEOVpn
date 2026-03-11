# 📘 Документация по интеграции Telegram VPN Backend

Данный документ описывает правила взаимодействия между микросервисом бота (`telegram-bot-service`) и основным бэкендом системы.

---

## 🔐 1. Система безопасности (Security)

Бэкенд использует кастомную систему аутентификации на основе HTTP-заголовков. Обработка заголовков происходит в модуле `common`.

### Ключевые заголовки (Request Headers)
При каждом запросе бот обязан передавать следующие заголовки:

| Заголовок | Описание | Роль в системе |
| :--- | :--- | :--- |
| `X-Internal-Secret` | Общий секретный ключ микросервисов | `SERVICE` |
| `X-User-Id` | Telegram ID текущего пользователя | `USER` |
| `X-Admin-Token` | Специфичный токен для админ-панели | `ADMIN` |

`internal-secret:` 53a41724a2428714e21b2cbcbb19ce1ff62f4deb322037575f15f450791d54c3

`admin-token:` YRqDAzgXLn29n/LcBnYYu7tQzgYEEikl0X5TYHrrwyiWgo+lBtlaJSD5MGp2j3YLB9u/xZu3jgK+540nyvpESA==

`admins:` 858441917,970667053

### Аннотации контроля доступа
В контроллерах бэкенда методы защищены следующими аннотациями:
*   `@Public`: Доступ разрешен без проверки токенов.
*   `@RequireUser`: Требуется заголовок `X-User-Id`. Метод работает в контексте конкретного пользователя.
*   `@RequireService`: Требуется заголовок `X-Internal-Secret`. Только для системных операций.
*   `@RequireAdmin`: Требуется заголовок `X-Admin-Token`. Только для управления серверами.
---

## 📡 2. Межсервисное взаимодействие (Feign)

Для вызова методов бэкенда используйте **Spring Cloud Feign**.

### Важное правило:
Все Feign-клиенты должны использовать `FeignClientConfiguration.class` из модуля `common`. Это гарантирует автоматическое добавление заголовка `X-Internal-Secret` к каждому запросу.

**Пример реализации клиента в `bot-service`:**

```java
@FeignClient(
    name = "user-service", 
    url = "${services.user-service.url}", 
    configuration = FeignClientConfiguration.class
)
public interface UserServiceClient {

    // Если метод в бэкенде помечен @RequireUser, 
    // бот ОБЯЗАН передать X-User-Id из контекста Telegram
    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyProfile(@RequestHeader("X-User-Id") Long telegramId);
}
```
## 🛠 3. Основные эндпоинты API

### Управление аккаунтами, балансом и устройствами.

| Метод | Путь                               | Описание | Доступ  |
| :--- |:-----------------------------------| :--- |:--------|
| `POST` | /api/v1/users/register             | `Регистрация при первом нажатии /start` | @Public |
| `GET` | /api/v1/users/me | `Данные профиля и баланс` | @RequireUser|
| `POST` | /api/v1/devices | `Регистрация нового устройства юзера` |@RequireUser |
| `GET` | /api/v1/devices | `Список всех устройств текущего юзера` |@RequireUser |
| `DELETE` | /api/v1/devices/{uuid} | `Удаление устройства` |@RequireUser |

### Генерация VPN ключей и ссылок

| Метод | Путь                               | Описание                                       | Доступ  |
| :--- |:-----------------------------------|:-----------------------------------------------|:--------|
| `POST` | /api/v1/configs | `Создать VLESS ссылку и QR-код для устройства` | @RequireUser |
| `GET` | /api/v1/configs | `Список всех активных VPN ссылок юзера`        | @RequireUser|
| `PUT` | /api/v1/configs/{id}/regenerate | `Перевыпустить ключи (сброс ссылки)`|@RequireUser |

## 🛠4. Формат данных и рекомендации

Объект ответа (ApiResponse)

Бэкенд всегда возвращает данные в стандартной обертке:

```json
{
  "success": true,   // Флаг успешности
  "data": { ... },   // Полезная нагрузка (Payload)
  "error": null      // Объект ошибки (если success = false)
}
```

Важные нюансы:
1. Баланс пользователя: Хранится в копейках (Integer).
- Пример: 5000 в JSON означает 50.00 руб. в интерфейсе бота.
2. VPN Ссылки: Приходят в поле vlessLink. Бот должен выводить их в режиме моноширинного шрифта для удобства копирования.
3. QR-Коды: Приходят в поле qrCodeDataUrl как Base64-строка.
- Действие: Бот должен декодировать Base64 и отправлять пользователю полноценное изображение (Photo), а не текст.
4. Device ID: При создании конфига (/api/v1/configs) необходимо передавать deviceId (Long), полученный из User Service.

## ⚠️ 5. Обработка ошибок

Если success: false, обработайте следующие коды:
- SYS_004: Access Denied. Проверьте передачу заголовков X-Internal-Secret и X-User-Id.
- SYS_002: Validation Error. Проверьте правильность заполнения полей в теле запроса.
- USR_001: Пользователь не найден.
- SRV_001: Нет свободных серверов. Уведомите пользователя о тех. работах.

## 6. Структура данных (Entities)

👤 User (user-service)

Основная запись пользователя. Ключом для бота является telegramId.
- id (Long): Внутренний ID.
- telegramId (Long): Уникальный ID из Telegram. Использовать для всех запросов.
- username (String): Юзернейм (без @).
- balance (Integer): Текущий счет в копейках.
- subscriptionType (Enum): Тип подписки (PAYG - по факту, PREPAID - абонентская).
- referralCode (String): Уникальный код этого юзера для приглашений.

📱 Device (user-service)
Устройства пользователя. К одному юзеру может быть привязано до 5 девайсов.
- userId (Long): Ссылка на telegramId.
- deviceName (String): Имя (напр. "Мой iPhone").
- deviceType (Enum): IOS, ANDROID, DESKTOP.
- uuid (UUID): Технический ID устройства.

⚙️ VpnConfiguration (vpn-config-service)
Конкретная ссылка на доступ к VPN.
- deviceId (Long): К какому устройству привязан конфиг.
- serverId (Integer): На какой ноде живет этот конфиг.
- vlessUuid (UUID): Тот самый секретный ключ, который вставляется в VPN-клиент.
- vlessLink (String): Полная готовая ссылка vless://....
- status (Enum): ACTIVE, REVOKED (отозван), SUSPENDED (заморожен за неуплату).

📊 TrafficUsage (server-management-service)
Записи о потреблении данных. Генерируются каждые 5 минут.
- bytesIn / bytesOut (Long): Трафик в байтах.
- bytesTotal (Long): Сумма (вычисляется в БД).
- costKopecks (Integer): Сколько списано денег за эту сессию.

## 7. Структура данных (DTO)

Все объекты обмена данными (DTO) обернуты в стандартный ApiResponse. Разработчику бота необходимо использовать эти модели для формирования запросов и парсинга ответов.

1. Запросы (Requests) — отправляются Ботом

📝 UserRegistrationRequest

Используется при первом взаимодействии пользователя с ботом (команда /start).
- telegramId (Long, Required): Уникальный ID пользователя из Telegram.
- username (String): Юзернейм в Telegram (без @).
- firstName (String): Имя пользователя.
- referralCode (String): Код пригласившего пользователя (если есть).

📱 DeviceCreateRequest

Используется для привязки нового устройства.
- userId (Long, Required): Telegram ID владельца.
- deviceName (String, Required): Человекочитаемое имя (например, "My iPhone 15").
- deviceType (DeviceType, Required): Тип устройства (IOS, ANDROID, DESKTOP).

⚙️ ConfigCreateRequest

Запрос на генерацию новой VLESS-ссылки.
- userId (Long, Required): Telegram ID пользователя.
- deviceId (Long, Required): Внутренний ID устройства (полученный из DeviceResponse).
- preferredCountry (String): Код страны (например, "NL", "DE").
- protocol (String): По умолчанию "VLESS".

2. Ответы (Responses) — получаются Ботом

👤 UserResponse

Основная информация о пользователе.
- telegramId (Long): ID пользователя.
- balance (Integer): Баланс в копейках.
- subscriptionType (SubscriptionType): Тип тарифа (PAYG, MONTHLY).
- hasActiveSubscription (Boolean): Флаг наличия оплаченного доступа.
- referralCode (String): Личный код пользователя для приглашений.

📡 VpnConfigResponse

Самый важный объект для пользователя.
- vlessLink (String): Готовая ссылка для импорта в VPN-клиент.
- qrCodeDataUrl (String): QR-код в формате Base64 (картинка).
- serverName (String): Название сервера (например, "Netherlands-01").
- status (ConfigStatus): Состояние (ACTIVE, REVOKED и т.д.).

🖥 ServerDto

Информация о доступных локациях (если бот предлагает выбор стран).
- name (String): Название.
- location (String): Страна/Город.
- countryCode (String): ISO код (NL, LV).
- loadPercentage (Double): Загруженность сервера (для индикации "свободен/занят").

3. Перечисления (Enums)

Бот должен строго придерживаться этих строковых значений:

DeviceType (Типы устройств)
- IOS: iPhone, iPad.
- ANDROID: Смартфоны и планшеты Android.
- DESKTOP: Windows, MacOS, Linux приложения.
- UNKNOWN: Если тип не определен.

SubscriptionType (Типы подписки)
- PAYG: (Pay-As-You-Go) Оплата за каждый потребленный мегабайт.
- MONTHLY: Месячный безлимитный пакет.
- YEARLY: Годовой безлимитный пакет.

ConfigStatus (Статус конфигурации)
- ACTIVE: Работает, трафик считается.
- REVOKED: Удалена пользователем.
- EXPIRED: Истек срок действия.
- SUSPENDED: Заблокирована (например, при балансе < 0).

4. Вспомогательные структуры

Envelope (ApiResponse)

Любой ответ от бэкенда приходит в этой обертке:

```json
{
  "success": "boolean",
  "data": "TargetObject", 
  "error": {
    "code": "string",
    "message": "string",
    "validationErrors": "map"
  }
}
```

ConfigMetadataDto (Redis Cache)

Технический объект, связывающий UUID ссылки с UserId. Используется внутри бэкенда для мгновенного биллинга, разработчику бота знать о его структуре полезно для понимания логики кэширования.

