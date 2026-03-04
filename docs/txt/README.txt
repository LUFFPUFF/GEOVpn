# Telegram VPN 
## Telegram-first подход с автоматическим выбором региона

---

## 1. КОНЦЕПЦИЯ ПРОЕКТА

### Пользовательский Flow:

```
1. Пользователь открывает Telegram → находит @YourVPNBot
2. Нажимает /start → автоматическая регистрация
3. Получает реферальную ссылку (бонус 50₽ за друга)
4. Открывает Mini App → видит баланс, устройства, серверы
5. Нажимает "Получить конфиг" → мгновенно получает VLESS ссылку
6. Скачивает мобильное приложение → вставляет ссылку → подключается
7. Приложение автоматически выбирает лучший сервер из 3 регионов
```

### Регионы и выбор сервера:

```
🇫🇮 Финляндия (Helsinki):
   - Близко к России
   - Отличная латентность (~30ms)
   - Хорошая скорость

🇳🇱 Нидерланды (Amsterdam):
   - Лучшая инфраструктура
   - Максимальная скорость
   - Средняя латентность (~50ms)

🇱🇻 Латвия (Riga):
   - Самая близкая
   - Минимальная латентность (~15ms)
   - Хорошо для gaming

Автовыбор на основе:
✓ Ping (латентность)
✓ Загрузка сервера
✓ История блокировок
✓ Тип использования (browsing/streaming/gaming)
```

---

## 2. УНИКАЛЬНАЯ ФИШКА 🚀

### **Smart Mode - AI-powered интеллектуальный VPN**

```yaml
Что это:
  Приложение автоматически определяет какие сайты заблокированы
  и пропускает через VPN ТОЛЬКО их, остальной трафик идет напрямую.
  
Преимущества:
  ✓ Максимальная скорость (большинство трафика без VPN)
  ✓ Не расходуется лимит (только заблокированные)
  ✓ Российские сервисы работают как обычно (банки, госуслуги)
  ✓ Netflix, YouTube premium с российскими ценами
  
Как работает:
  1. Встроенная база заблокированных доменов (обновляется автоматически)
  2. Real-time проверка доступности
  3. Automatic failover если что-то заблокировали
  4. Machine Learning предсказывает что заблокируют завтра
  
Режимы:
  🧠 Smart Mode    - только заблокированное через VPN
  🌍 Full VPN      - весь трафик через VPN  
  🎮 Gaming Mode   - низкая латентность, fast servers
  📺 Streaming     - оптимизация для видео
```

### Дополнительные фишки:

```
🔥 Instant Connect - подключение за 0.5 секунды
   (pre-established connections pool)

🎯 Family Sharing - одна подписка на 5 устройств
   (семейный аккаунт с раздельной статистикой)

💰 Pay-as-you-go - платишь только за использованный трафик
   (100₽/10GB, 200₽/50GB, 500₽/∞)

🤖 Telegram Mini App - полное управление без установки
   (история, статистика, настройки прямо в Telegram)

⚡ Zero-config - просто нажми кнопку
   (никаких сложных настроек)
```

---

## 3. ТЕХНИЧЕСКАЯ АРХИТЕКТУРА

### Высокоуровневая схема:

```
┌─────────────────────────────────────────────────────────┐
│                    TELEGRAM LAYER                        │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Telegram Bot │  │  Mini App    │  │  Webhook     │  │
│  │   (Python)   │  │(React/Next)  │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    BACKEND LAYER                         │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐   │
│  │        Spring Boot Application (Java)            │   │
│  │  - REST API для Mini App                        │   │
│  │  - User Management                               │   │
│  │  - Config Generation (VLESS links)               │   │
│  │  - Billing & Subscriptions                       │   │
│  │  - Analytics & Metrics                           │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    VPN SERVERS                           │
├─────────────────────────────────────────────────────────┤
│  🇫🇮 Helsinki (3 servers)  🇳🇱 Amsterdam (3 servers)   │
│  🇱🇻 Riga (3 servers)                                   │
│                                                          │
│  Each running:                                           │
│  - Xray-core (VLESS + Reality)                          │
│  - Metrics exporter                                      │
│  - Health checker                                        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  MOBILE APPS                             │
├─────────────────────────────────────────────────────────┤
│  📱 iOS (Swift)        📱 Android (Kotlin)              │
│  - Native VPN client   - Native VPN client              │
│  - Smart Mode          - Smart Mode                     │
│  - Auto-selection      - Auto-selection                 │
└─────────────────────────────────────────────────────────┘
```

### Детальная архитектура компонентов:

```
┌───────────────────────────────────────────────────────────┐
│ TELEGRAM BOT (Python + aiogram)                           │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  Commands:                                                 │
│  /start       → Регистрация + приветствие                 │
│  /profile     → Личный кабинет                            │
│  /config      → Получить VLESS ссылку                     │
│  /balance     → Баланс и история                          │
│  /referral    → Реферальная ссылка                        │
│  /support     → Техподдержка                              │
│                                                            │
│  Inline Buttons:                                           │
│  [🔗 Получить конфиг]  [💰 Пополнить]  [📊 Статистика]   │
│  [🎁 Пригласить друга] [⚙️ Настройки]  [❓ Помощь]       │
│                                                            │
│  Mini App Launch:                                          │
│  [🚀 Открыть приложение] → Telegram Mini App              │
│                                                            │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ TELEGRAM MINI APP (React + Telegram Web App SDK)          │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  Main Screen:                                              │
│  ┌────────────────────────────────────────────────┐       │
│  │  👤 User: @username                            │       │
│  │  💰 Баланс: 155₽ (хватит на ≈15 дней)        │       │
│  │                                                │       │
│  │  ┌──────────────┐  ┌──────────────┐           │       │
│  │  │  Пополнить   │  │   История    │           │       │
│  │  └──────────────┘  └──────────────┘           │       │
│  │                                                │       │
│  │  📱 Мои устройства (2/5)                      │       │
│  │  ┌────────────────────────────────────────┐   │       │
│  │  │ 🍎 iPhone 13 Pro (подключено)         │   │       │
│  │  │ 📍 Netherlands · 45ms · 120 MB/s      │   │       │
│  │  │ [Отключить] [Удалить]                 │   │       │
│  │  └────────────────────────────────────────┘   │       │
│  │                                                │       │
│  │  ┌────────────────────────────────────────┐   │       │
│  │  │ 🤖 Samsung Galaxy (offline)           │   │       │
│  │  │ [Получить конфиг]                     │   │       │
│  │  └────────────────────────────────────────┘   │       │
│  │                                                │       │
│  │  [+ Добавить устройство]                      │       │
│  │                                                │       │
│  │  🌍 Серверы:                                  │       │
│  │  🇱🇻 Riga: ⚡ Отлично (12ms)                  │       │
│  │  🇫🇮 Helsinki: ⚡ Хорошо (28ms)               │       │
│  │  🇳🇱 Amsterdam: ⚡ Средне (52ms)              │       │
│  │                                                │       │
│  │  [🧠 Smart Mode] [🌍 Full VPN] [🎮 Gaming]   │       │
│  └────────────────────────────────────────────────┘       │
│                                                            │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ BACKEND API (Spring Boot + Java)                          │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  Core Services:                                            │
│  ┌─────────────────────────────────────────────────┐      │
│  │ UserService                                     │      │
│  │ - Registration via Telegram ID                 │      │
│  │ - Profile management                            │      │
│  │ - Device tracking (max 5 per user)            │      │
│  └─────────────────────────────────────────────────┘      │
│                                                            │
│  ┌─────────────────────────────────────────────────┐      │
│  │ ConfigGeneratorService                          │      │
│  │ - Generate VLESS links                          │      │
│  │ - UUID per user                                 │      │
│  │ - Server selection algorithm                    │      │
│  │ - QR code generation                            │      │
│  └─────────────────────────────────────────────────┘      │
│                                                            │
│  ┌─────────────────────────────────────────────────┐      │
│  │ ServerManagementService                         │      │
│  │ - Health monitoring (ping every 10s)           │      │
│  │ - Load balancing                                │      │
│  │ - Auto-selection based on:                      │      │
│  │   * Latency (ping)                              │      │
│  │   * Server load (connections)                   │      │
│  │   * User location                               │      │
│  │   * Historical performance                      │      │
│  └─────────────────────────────────────────────────┘      │
│                                                            │
│  ┌─────────────────────────────────────────────────┐      │
│  │ BillingService                                  │      │
│  │ - Pay-as-you-go tracking                        │      │
│  │ - Subscription management                       │      │
│  │ - Referral bonuses                              │      │
│  │ - Payment integration (YooMoney, Crypto)       │      │
│  └─────────────────────────────────────────────────┘      │
│                                                            │
│  ┌─────────────────────────────────────────────────┐      │
│  │ AnalyticsService                                │      │
│  │ - Traffic usage per user                        │      │
│  │ - Connection statistics                         │      │
│  │ - Server performance metrics                    │      │
│  │ - Smart Mode optimization data                  │      │
│  └─────────────────────────────────────────────────┘      │
│                                                            │
└───────────────────────────────────────────────────────────┘
```

---

## 4. DATABASE SCHEMA (PostgreSQL)

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    balance INTEGER DEFAULT 0, -- в копейках
    subscription_type VARCHAR(50) DEFAULT 'payg', -- payg, monthly, yearly
    subscription_expires_at TIMESTAMP,
    referral_code VARCHAR(20) UNIQUE,
    referred_by BIGINT REFERENCES users(telegram_id),
    created_at TIMESTAMP DEFAULT NOW(),
    last_active_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_telegram ON users(telegram_id);
CREATE INDEX idx_users_referral ON users(referral_code);

-- Devices table
CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(telegram_id),
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL, -- ios, android
    uuid UUID UNIQUE NOT NULL, -- для VLESS
    is_active BOOLEAN DEFAULT true,
    last_connected_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT max_devices CHECK (
        (SELECT COUNT(*) FROM devices WHERE user_id = user_id AND is_active = true) <= 5
    )
);

CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_uuid ON devices(uuid);

-- Servers table
CREATE TABLE servers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(50) NOT NULL, -- finland, netherlands, latvia
    country_code CHAR(2) NOT NULL, -- FI, NL, LV
    ip_address VARCHAR(45) NOT NULL,
    port INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    max_connections INTEGER DEFAULT 1000,
    current_connections INTEGER DEFAULT 0,
    last_health_check TIMESTAMP,
    avg_latency_ms INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_servers_location ON servers(location);
CREATE INDEX idx_servers_active ON servers(is_active);

-- Connections table (для статистики)
CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(telegram_id),
    device_id BIGINT REFERENCES devices(id),
    server_id INTEGER REFERENCES servers(id),
    connected_at TIMESTAMP DEFAULT NOW(),
    disconnected_at TIMESTAMP,
    bytes_sent BIGINT DEFAULT 0,
    bytes_received BIGINT DEFAULT 0,
    duration_seconds INTEGER
);

CREATE INDEX idx_connections_user ON connections(user_id, connected_at DESC);

-- Transactions table (платежи)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(telegram_id),
    amount INTEGER NOT NULL, -- в копейках
    transaction_type VARCHAR(50) NOT NULL, -- deposit, referral_bonus, usage
    payment_method VARCHAR(50),
    status VARCHAR(50) DEFAULT 'pending', -- pending, completed, failed
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_transactions_user ON transactions(user_id, created_at DESC);

-- Blocked domains (для Smart Mode)
CREATE TABLE blocked_domains (
    id SERIAL PRIMARY KEY,
    domain VARCHAR(255) UNIQUE NOT NULL,
    is_blocked BOOLEAN DEFAULT true,
    last_checked TIMESTAMP DEFAULT NOW(),
    auto_detected BOOLEAN DEFAULT false
);

CREATE INDEX idx_blocked_domains ON blocked_domains(domain);
```

---

## 5. VLESS CONFIGURATION

### Server Configuration (Xray-core)

```json
{
  "log": {
    "loglevel": "warning"
  },
  "inbounds": [
    {
      "port": 443,
      "protocol": "vless",
      "settings": {
        "clients": [],
        "decryption": "none",
        "fallbacks": [
          {
            "dest": 80
          }
        ]
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "dest": "www.google.com:443",
          "serverNames": [
            "www.google.com",
            "www.microsoft.com"
          ],
          "privateKey": "GENERATED_PRIVATE_KEY",
          "shortIds": ["", "0123456789abcdef"]
        }
      }
    }
  ],
  "outbounds": [
    {
      "protocol": "freedom",
      "tag": "direct"
    }
  ]
}
```

### VLESS Link Generation (Java)

```java
// ConfigGeneratorService.java
package com.vpn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfigGeneratorService {
    
    private final ServerManagementService serverService;
    
    public String generateVlessLink(Long userId, String deviceName) {
        // 1. Выбор лучшего сервера
        Server bestServer = serverService.selectBestServer(userId);
        
        // 2. Генерация UUID для устройства
        UUID uuid = UUID.randomUUID();
        
        // 3. Сохранение в БД
        Device device = saveDevice(userId, deviceName, uuid);
        
        // 4. Регистрация клиента на сервере
        registerClientOnServer(bestServer, uuid);
        
        // 5. Генерация VLESS ссылки
        return buildVlessLink(uuid, bestServer);
    }
    
    private String buildVlessLink(UUID uuid, Server server) {
        // vless://UUID@IP:PORT?security=reality&sni=www.google.com&fp=chrome&pbk=PUBLIC_KEY&sid=SHORT_ID&type=tcp&flow=xtls-rprx-vision#ServerName
        
        StringBuilder link = new StringBuilder("vless://");
        link.append(uuid.toString());
        link.append("@");
        link.append(server.getIpAddress());
        link.append(":");
        link.append(server.getPort());
        link.append("?security=reality");
        link.append("&sni=www.google.com");
        link.append("&fp=chrome");
        link.append("&pbk=").append(server.getRealityPublicKey());
        link.append("&sid=").append(server.getRealityShortId());
        link.append("&type=tcp");
        link.append("&flow=xtls-rprx-vision");
        link.append("#").append(urlEncode(server.getName()));
        
        return link.toString();
    }
    
    /**
     * Алгоритм выбора лучшего сервера
     */
    public Server selectBestServer(Long userId) {
        List<Server> activeServers = serverRepository
            .findByIsActiveTrue();
        
        // Получаем локацию пользователя (примерно)
        String userCountry = geoIpService.getCountry(userId);
        
        Server bestServer = null;
        double bestScore = 0;
        
        for (Server server : activeServers) {
            double score = calculateServerScore(server, userCountry);
            
            if (score > bestScore) {
                bestScore = score;
                bestServer = server;
            }
        }
        
        return bestServer;
    }
    
    private double calculateServerScore(Server server, String userCountry) {
        double score = 100.0;
        
        // 1. Латентность (чем меньше, тем лучше)
        if (server.getAvgLatencyMs() != null) {
            score -= server.getAvgLatencyMs() * 0.5;
        }
        
        // 2. Загрузка сервера (чем меньше, тем лучше)
        double loadPercentage = (double) server.getCurrentConnections() 
                                / server.getMaxConnections();
        score -= loadPercentage * 30;
        
        // 3. География (Латвия ближе для России)
        if ("RU".equals(userCountry)) {
            if ("LV".equals(server.getCountryCode())) {
                score += 20; // Латвия самая близкая
            } else if ("FI".equals(server.getCountryCode())) {
                score += 15; // Финляндия тоже близко
            } else if ("NL".equals(server.getCountryCode())) {
                score += 10; // Нидерланды дальше
            }
        }
        
        return Math.max(score, 0);
    }
}
```

---

## 6. TELEGRAM BOT IMPLEMENTATION

### Python Bot (aiogram)

```python
# bot.py
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command
from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton, WebAppInfo
import aiohttp
import asyncio

API_TOKEN = 'YOUR_BOT_TOKEN'
API_BASE_URL = 'http://backend:8080/api'
MINI_APP_URL = 'https://your-mini-app.com'

bot = Bot(token=API_TOKEN)
dp = Dispatcher()

@dp.message(Command('start'))
async def start_command(message: types.Message):
    """
    Регистрация пользователя и приветствие
    """
    telegram_id = message.from_user.id
    username = message.from_user.username
    first_name = message.from_user.first_name
    
    # Проверка реферального кода
    referral_code = None
    if len(message.text.split()) > 1:
        referral_code = message.text.split()[1]
    
    # Регистрация через API
    async with aiohttp.ClientSession() as session:
        async with session.post(
            f'{API_BASE_URL}/users/register',
            json={
                'telegram_id': telegram_id,
                'username': username,
                'first_name': first_name,
                'referral_code': referral_code
            }
        ) as resp:
            user_data = await resp.json()
    
    # Приветственное сообщение
    welcome_text = f"""
👋 Привет, {first_name}!

Добро пожаловать в самый быстрый VPN!

💰 Ваш баланс: {user_data['balance']}₽
🎁 Бонус за регистрацию: 50₽

🚀 Что дальше?
1. Скачайте приложение для вашего устройства
2. Получите конфигурацию (нажмите кнопку ниже)
3. Подключитесь одним касанием!

🧠 Smart Mode - наша уникальная фишка!
VPN включается только для заблокированных сайтов.
Экономия трафика и максимальная скорость!

⚡ 3 региона: Латвия, Финляндия, Нидерланды
Приложение автоматически выберет лучший!
"""
    
    keyboard = InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(
                text="🚀 Открыть приложение",
                web_app=WebAppInfo(url=MINI_APP_URL)
            )
        ],
        [
            InlineKeyboardButton(
                text="🔗 Получить конфиг",
                callback_data="get_config"
            )
        ],
        [
            InlineKeyboardButton(
                text="💰 Пополнить баланс",
                callback_data="add_balance"
            ),
            InlineKeyboardButton(
                text="📊 Статистика",
                callback_data="stats"
            )
        ],
        [
            InlineKeyboardButton(
                text="🎁 Пригласить друга (+50₽)",
                callback_data="referral"
            )
        ],
        [
            InlineKeyboardButton(
                text="📱 Скачать приложение",
                url="https://your-app-download.com"
            )
        ]
    ])
    
    await message.answer(welcome_text, reply_markup=keyboard)

@dp.callback_query(lambda c: c.data == 'get_config')
async def get_config_callback(callback_query: types.CallbackQuery):
    """
    Генерация VLESS конфигурации
    """
    telegram_id = callback_query.from_user.id
    
    # Получение конфига через API
    async with aiohttp.ClientSession() as session:
        async with session.post(
            f'{API_BASE_URL}/config/generate',
            json={
                'telegram_id': telegram_id,
                'device_name': 'Telegram Device'
            }
        ) as resp:
            if resp.status == 200:
                config_data = await resp.json()
                vless_link = config_data['vless_link']
                qr_code_url = config_data['qr_code_url']
                
                config_text = f"""
✅ Конфигурация готова!

🔗 VLESS ссылка:
`{vless_link}`

📋 Как использовать:
1. Скачайте приложение (кнопка ниже)
2. Откройте приложение
3. Нажмите "Добавить конфигурацию"
4. Вставьте ссылку выше
5. Подключитесь!

🌍 Сервер: {config_data['server_name']}
📍 Локация: {config_data['server_location']}
⚡ Пинг: {config_data['latency']}ms

💡 Совет: включите Smart Mode для экономии трафика!
"""
                
                keyboard = InlineKeyboardMarkup(inline_keyboard=[
                    [
                        InlineKeyboardButton(
                            text="📱 Скачать приложение",
                            url="https://your-app-download.com"
                        )
                    ],
                    [
                        InlineKeyboardButton(
                            text="🔄 Сменить сервер",
                            callback_data="change_server"
                        )
                    ],
                    [
                        InlineKeyboardButton(
                            text="📊 Мои устройства",
                            callback_data="devices"
                        )
                    ]
                ])
                
                # Отправка QR кода
                await callback_query.message.answer_photo(
                    photo=qr_code_url,
                    caption=config_text,
                    reply_markup=keyboard,
                    parse_mode='Markdown'
                )
            else:
                error_data = await resp.json()
                await callback_query.message.answer(
                    f"❌ Ошибка: {error_data['message']}"
                )
    
    await callback_query.answer()

@dp.callback_query(lambda c: c.data == 'referral')
async def referral_callback(callback_query: types.CallbackQuery):
    """
    Реферальная программа
    """
    telegram_id = callback_query.from_user.id
    
    async with aiohttp.ClientSession() as session:
        async with session.get(
            f'{API_BASE_URL}/users/{telegram_id}/referral'
        ) as resp:
            referral_data = await resp.json()
    
    referral_link = f"https://t.me/{bot._me.username}?start={referral_data['code']}"
    
    referral_text = f"""
🎁 Реферальная программа

Приглашайте друзей и получайте бонусы!

💰 Вы получите: 50₽ за каждого друга
🎉 Ваш друг получит: 50₽ при регистрации

Ваша реферальная ссылка:
{referral_link}

📊 Статистика:
Приглашено: {referral_data['total_referrals']} человек
Заработано: {referral_data['total_earned']}₽
"""
    
    keyboard = InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(
                text="📤 Поделиться ссылкой",
                url=f"https://t.me/share/url?url={referral_link}"
            )
        ]
    ])
    
    await callback_query.message.answer(
        referral_text,
        reply_markup=keyboard
    )
    await callback_query.answer()

@dp.callback_query(lambda c: c.data == 'stats')
async def stats_callback(callback_query: types.CallbackQuery):
    """
    Статистика использования
    """
    telegram_id = callback_query.from_user.id
    
    async with aiohttp.ClientSession() as session:
        async with session.get(
            f'{API_BASE_URL}/users/{telegram_id}/stats'
        ) as resp:
            stats_data = await resp.json()
    
    stats_text = f"""
📊 Ваша статистика

💰 Текущий баланс: {stats_data['balance']}₽
📱 Устройств: {stats_data['active_devices']}/5

📈 За последние 30 дней:
├ Использовано: {stats_data['traffic_used_gb']} GB
├ Сэкономлено (Smart Mode): {stats_data['traffic_saved_gb']} GB
├ Сессий: {stats_data['sessions_count']}
└ Среднее время: {stats_data['avg_session_duration']} мин

🌍 Любимый сервер: {stats_data['favorite_server']}
⚡ Средний пинг: {stats_data['avg_latency']}ms

💡 Smart Mode сэкономил вам {stats_data['money_saved']}₽!
"""
    
    keyboard = InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(
                text="📈 Подробная статистика",
                web_app=WebAppInfo(url=f"{MINI_APP_URL}/stats")
            )
        ]
    ])
    
    await callback_query.message.answer(stats_text, reply_markup=keyboard)
    await callback_query.answer()

async def main():
    await dp.start_polling(bot)

if __name__ == '__main__':
    asyncio.run(main())
```

---

## 7. СТОИМОСТЬ И ЦЕНООБРАЗОВАНИЕ

### Инфраструктура (9 серверов):

```
🇱🇻 Латвия (Riga):
  - 3 × Hetzner CPX21 (€8.90/мес) = €26.70
  
🇫🇮 Финляндия (Helsinki):
  - 3 × Hetzner CPX21 (€8.90/мес) = €26.70
  
🇳🇱 Нидерланды (Amsterdam):
  - 3 × Hetzner CPX21 (€8.90/мес) = €26.70

Backend (Java):
  - 1 × Hetzner CPX31 (€12.90/мес)

Database (PostgreSQL):
  - 1 × Hetzner CPX21 (€8.90/мес)

CDN (Cloudflare):
  - Free tier

ИТОГО: ~€110/месяц (~11,000₽)
```

### Тарифы для пользователей:

```
💰 Pay-as-you-go:
  - 100₽ = 10 GB
  - 200₽ = 50 GB (скидка 60%)
  - 500₽ = ∞ (1 месяц)

📱 Подписки:
  - 1 месяц: 300₽
  - 3 месяца: 750₽ (скидка 17%)
  - 12 месяцев: 2,500₽ (скидка 30%)

🎁 Бонусы:
  - Регистрация: +50₽
  - Реферал: +50₽ (вам и другу)
  - Первое пополнение >200₽: +50₽
```

### Break-even Point:

```
При 100 активных пользователей:
  - Средний чек: 300₽/мес
  - Доход: 30,000₽/мес
  - Расходы: 11,000₽/мес
  - Прибыль: 19,000₽/мес

При 1,000 пользователей:
  - Доход: 300,000₽/мес
  - Расходы: ~40,000₽/мес (больше серверов)
  - Прибыль: 260,000₽/мес
```

---

## 8. ROADMAP

### Month 1: MVP

- ✓ Telegram Bot (регистрация, команды)
- ✓ Backend API (Java Spring Boot)
- ✓ 3 сервера (по одному в каждом регионе)
- ✓ VLESS конфигурация
- ✓ База данных (PostgreSQL)
- ✓ Простая оплата (YooMoney)

### Month 2: Mobile Apps

- ✓ iOS app (Swift + Network Extension)
- ✓ Android app (Kotlin + VpnService)
- ✓ Auto server selection
- ✓ Telegram Mini App (React)
- ✓ Реферальная система

### Month 3: Smart Mode

- ✓ База заблокированных доменов
- ✓ Split-tunneling реализация
- ✓ Auto-update blocked list
- ✓ Machine Learning для предсказаний
- ✓ Gaming mode optimization

### Month 4+: Scaling

- Multi-region expansion
- Advanced analytics
- Family sharing
- Business accounts
- API для интеграций

Продолжить с кодом мобильных приложений или детализацией Smart Mode?