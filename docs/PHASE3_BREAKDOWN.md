# 🌐 ФАЗА 3: Mobile Network Core и Smart Mode

**Цель:** Создать интеллектуальную систему обхода блокировок, работающую в условиях активного DPI и мобильных сетей России.

**Сложность:** ⭐⭐⭐⭐⭐ (Очень высокая)  
**Приоритет:** Критический (без этого VPN бесполезен в РФ)  
**Время:** ~4-6 недель  

---

## 📋 ОГЛАВЛЕНИЕ

1. [Обзор проблемы](#обзор-проблемы)
2. [Архитектура решения](#архитектура-решения)
3. [Этапы разработки](#этапы-разработки)
4. [Технологический стек](#технологический-стек)

---

## 🔍 ОБЗОР ПРОБЛЕМЫ

### **Что блокируют в России (2026):**

1. **DPI (Deep Packet Inspection)**
   - Анализ TLS ClientHello (SNI, ALPN, Ciphers)
   - Детект VPN протоколов (OpenVPN, WireGuard signatures)
   - Statistical analysis трафика (packet size, timing)
   - ActiveProbing (заворачивают TLS handshake на свой proxy)

2. **DNS Poisoning**
   - Подмена DNS ответов
   - Блокировка DoH/DoT серверов
   - MITM атаки на незашифрованный DNS

3. **IP Blocking**
   - Блокировка VPN серверов по IP
   - Блокировка облачных провайдеров (AWS, GCP, DigitalOcean)
   - Rate limiting на подозрительные IP

4. **RST Injection**
   - Сброс TCP соединений (RST пакеты)
   - Throttling скорости
   - Packet loss injection

5. **Мобильные операторы (МТС, Билайн, Мегафон)**
   - Агрессивный DPI на уровне GGSN/P-GW
   - Блокировка портов (особенно UDP)
   - Transparent proxy для HTTP/HTTPS
   - Traffic shaping

### **Почему Reality + VLESS + Vision НЕ ВСЕГДА достаточно:**

✅ **Работает:**
- Маскировка под легитимный TLS трафик
- Bypass SNI-based блокировок
- Защита от пассивного DPI

❌ **Не работает:**
- ActiveProbing (если завернут на свой proxy - сломается handshake)
- Statistical fingerprinting (размеры пакетов, timing)
- IP blocking VPN серверов
- DNS poisoning (если клиент использует провайдерский DNS)
- Агрессивный мобильный DPI

### **Решение: Smart Mode + Split Tunneling + Multiple Fallbacks**

---

## 🏗️ АРХИТЕКТУРА РЕШЕНИЯ

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID CLIENT                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   SMART ROUTING ENGINE (Kotlin)                      │  │
│  │                                                        │  │
│  │  • Domain Classifier (ML-based)                      │  │
│  │  • GeoIP Resolver                                    │  │
│  │  • Rule Engine (whitelist/blacklist)                │  │
│  │  • Fallback Manager                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   VPN TUNNEL MANAGER                                 │  │
│  │                                                        │  │
│  │  Primary:   Reality/VLESS + Vision                   │  │
│  │  Fallback1: Shadowsocks (obfs)                       │  │
│  │  Fallback2: Hysteria2 (QUIC-based, UDP)            │  │
│  │  Fallback3: TUIC (over UDP)                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   DNS RESOLVER (with fallbacks)                      │  │
│  │                                                        │  │
│  │  1. DoH (Cloudflare, Google)                        │  │
│  │  2. DoT (1.1.1.1, 8.8.8.8)                          │  │
│  │  3. DNSCrypt                                         │  │
│  │  4. Direct (only for .ru domains)                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   ANTI-CENSORSHIP LAYER                              │  │
│  │                                                        │  │
│  │  • TLS Fingerprint Randomization                     │  │
│  │  • ECH (Encrypted Client Hello)                      │  │
│  │  • Traffic Obfuscation (random padding)             │  │
│  │  • Timing Jitter                                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          ↓
                 ══════════════════
                   INTERNET
                 ══════════════════
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   BACKEND SERVICES                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   DOMAIN LISTS SERVICE (API)                         │  │
│  │                                                        │  │
│  │  • Blocked domains (auto-updated from Roskomnadzor) │  │
│  │  • Russian domains (.ru, .рф) - direct              │  │
│  │  • VPN-only domains (social networks, news)         │  │
│  │  • Custom user rules                                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   SERVER HEALTH MONITOR                              │  │
│  │                                                        │  │
│  │  • Reachability tests from RU mobile networks       │  │
│  │  • DPI detection (ActiveProbing simulation)         │  │
│  │  • Auto-failover to clean IPs                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │   ANALYTICS & TELEMETRY                              │  │
│  │                                                        │  │
│  │  • Connection success rate by ISP                    │  │
│  │  • Protocol effectiveness                            │  │
│  │  • Blocked domains detection                         │  │
│  │  • Performance metrics                               │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📅 ЭТАПЫ РАЗРАБОТКИ

## **ЭТАП 3.1: Domain Classification System** (~1 неделя)

**Цель:** Автоматически определять какие домены нужно пускать через VPN, а какие напрямую.

### **Подэтап 3.1.1: Blocked Domains Database**
⏱️ **2-3 дня**

**Задачи:**
1. Создать БД таблицу `blocked_domains`
2. Парсер списков Roskomnadzor (dump.csv)
3. Категоризация доменов (social, news, vpn, adult, etc)
4. API endpoint для получения списка
5. Кеширование в Redis (TTL 24h)

**Deliverables:**
- Миграция `V11__create_blocked_domains.sql`
- `BlockedDomain.java` entity
- `BlockedDomainRepository.java`
- `DomainClassificationService.java`
- REST API: `GET /api/v1/domains/blocked`
- Parser script: `parse_roskomnadzor.py`

**Технологии:**
- PostgreSQL (хранение)
- Redis (кеш)
- Python (парсер списков)
- Spring Scheduler (автообновление)

---

### **Подэтап 3.1.2: GeoIP & Russian Domains Bypass**
⏱️ **2 дня**

**Задачи:**
1. Интеграция MaxMind GeoIP2 database
2. Определение российских доменов (.ru, .рф, .su)
3. Whitelist для банков, госуслуг, яндекс
4. Rule Engine для custom rules

**Deliverables:**
- `GeoIpService.java`
- `DomainRuleEngine.java`
- `RussianDomainsWhitelist.txt`
- API: `POST /api/v1/domains/classify`

**Пример классификации:**
```json
{
  "domain": "youtube.com",
  "action": "VPN",
  "reason": "BLOCKED_BY_RKN",
  "category": "SOCIAL"
}

{
  "domain": "sberbank.ru",
  "action": "DIRECT",
  "reason": "RUSSIAN_DOMAIN",
  "category": "FINANCE"
}

{
  "domain": "google.com",
  "action": "DIRECT",
  "reason": "NOT_BLOCKED",
  "category": "SEARCH"
}
```

---

### **Подэтап 3.1.3: Auto-update from Roskomnadzor**
⏱️ **1-2 дня**

**Задачи:**
1. Scheduled job (каждые 6 часов)
2. Скачивание dump.csv
3. Парсинг и обновление БД
4. Очистка кеша Redis
5. Уведомление клиентов (через FCM)

**Deliverables:**
- `RoskomnadzorSyncJob.java`
- `DomainDiffCalculator.java` (только новые домены)
- FCM notification service

---

## **ЭТАП 3.2: Smart Routing Engine (Android)** (~1.5 недели)

**Цель:** Реализовать split tunneling на Android с умной маршрутизацией.

### **Подэтап 3.2.1: VPN Service Architecture**
⏱️ **3 дня**

**Задачи:**
1. Переработать `VpnService` с split tunneling
2. Packet interceptor (анализ DNS запросов)
3. Routing decision engine
4. Integration с Xray-core Android library

**Deliverables:**
- `SmartVpnService.kt`
- `PacketInterceptor.kt`
- `RoutingEngine.kt`
- `DomainCache.kt` (LRU cache)

**Технологии:**
- Android VpnService API
- Kotlin Coroutines
- Xray-core Android (libxray.so)
- DNS parsing library

---

### **Подэтап 3.2.2: DNS Resolution with DoH/DoT**
⏱️ **3 дня**

**Задачи:**
1. DoH client (Cloudflare, Google)
2. DoT client (dns.google, 1.1.1.1)
3. Fallback chain: DoH → DoT → Direct
4. DNS cache (TTL aware)
5. DNSSEC validation

**Deliverables:**
- `DohClient.kt`
- `DotClient.kt`
- `DnsResolver.kt` (facade)
- `DnsCache.kt`

**Libraries:**
- OkHttp (DoH)
- dnsjava (DoT, DNSSEC)

---

### **Подэтап 3.2.3: Rule Engine & Custom Rules**
⏱️ **2 дня**

**Задачи:**
1. Загрузка rules из Backend API
2. User custom rules (whitelist/blacklist)
3. Priority system (user > auto > default)
4. Rule persistence (SQLite)
5. UI для управления правилами

**Deliverables:**
- `RuleEngine.kt`
- `RuleRepository.kt` (Room database)
- UI: `CustomRulesFragment.kt`
- API: `POST /api/v1/domains/rules` (user rules)

---

## **ЭТАП 3.3: Multiple Protocols & Fallback** (~2 недели)

**Цель:** Добавить альтернативные протоколы на случай блокировки Reality.

### **Подэтап 3.3.1: Shadowsocks Integration**
⏱️ **3-4 дня**

**Задачи:**
1. Shadowsocks-libev Android library
2. AEAD ciphers (chacha20-ietf-poly1305, aes-256-gcm)
3. Simple-obfs plugin (http/tls obfuscation)
4. Конфигурация в Backend
5. Генерация SS ссылок

**Deliverables:**
- `ShadowsocksManager.kt`
- Backend: `ShadowsocksConfigGenerator.java`
- Migration: `V12__add_ss_configs.sql`
- API: `GET /api/v1/configs/shadowsocks`

**Зачем:**
- Fallback если Reality заблокирован
- Работает на портах 80/443 (TCP)
- Хорошо проходит DPI с obfs

---

### **Подэтап 3.3.2: Hysteria2 (QUIC-based)**
⏱️ **4-5 дней**

**Задачи:**
1. Hysteria2 Android library (или Go mobile)
2. UDP-based транспорт (обход TCP RST)
3. Brutal congestion control (для плохих сетей)
4. Port hopping (защита от rate limiting)
5. Backend конфигурация

**Deliverables:**
- `Hysteria2Manager.kt`
- Backend: `Hysteria2ConfigGenerator.java`
- Xray node: install hysteria2 server
- API: `GET /api/v1/configs/hysteria2`

**Зачем:**
- **UDP** → обходит TCP RST injection
- Отличная скорость в мобильных сетях
- Сложнее детектить DPI

---

### **Подэтап 3.3.3: Protocol Auto-Selection**
⏱️ **2-3 дня**

**Задачи:**
1. Health check для каждого протокола
2. Fallback chain: Reality → Shadowsocks → Hysteria2
3. Automatic switching при разрывах
4. Metrics collection (success rate)
5. User notification

**Deliverables:**
- `ProtocolSelector.kt`
- `ConnectionHealthMonitor.kt`
- `FallbackManager.kt`
- UI: Protocol status indicator

**Логика:**
```kotlin
1. Try Reality (primary)
   ↓ если timeout/blocked
2. Try Shadowsocks + obfs
   ↓ если не работает
3. Try Hysteria2 (UDP)
   ↓ если всё падает
4. Show error + suggest manual server change
```

---

## **ЭТАП 3.4: Anti-Censorship Layer** (~1.5 недели)

**Цель:** Обход продвинутого DPI и ActiveProbing.

### **Подэтап 3.4.1: TLS Fingerprint Randomization**
⏱️ **3 дня**

**Задачи:**
1. uTLS library integration (Go)
2. Randomize ClientHello (ciphers, extensions, curve)
3. Mimic popular browsers (Chrome, Firefox, Safari)
4. Periodic rotation

**Deliverables:**
- `TlsFingerprintRandomizer.kt`
- uTLS JNI bridge
- Configuration profiles

**Зачем:**
- DPI анализирует TLS fingerprint
- Randomization делает детект сложнее

---

### **Подэтап 3.4.2: ECH (Encrypted Client Hello)**
⏱️ **4 дня**

**Задачи:**
1. ECH support в Xray-core (если есть)
2. ECHConfig получение через DoH
3. Fallback на обычный SNI если ECH недоступен
4. Backend: nginx с ECH support

**Deliverables:**
- ECH integration в SmartVpnService
- Nginx ECH configuration
- Testing suite

**Зачем:**
- SNI шифруется → DPI не видит целевой домен
- Критично для обхода SNI-based блокировок

---

### **Подэтап 3.4.3: Traffic Obfuscation**
⏱️ **2-3 дня**

**Задачи:**
1. Random padding в пакетах
2. Timing jitter (random delays)
3. Traffic shaping (mimic HTTP/HTTPS patterns)
4. Packet fragmentation

**Deliverables:**
- `TrafficObfuscator.kt`
- Configurable obfuscation levels (low/medium/high)

**Зачем:**
- Statistical fingerprinting обход
- Маскировка под обычный HTTPS

---

## **ЭТАП 3.5: Mobile Network Optimizations** (~1 неделя)

**Цель:** Оптимизация для работы в 4G/5G мобильных сетях.

### **Подэтап 3.5.1: Network State Monitoring**
⏱️ **2 дня**

**Задачи:**
1. Детект смены сети (Wi-Fi ↔ Mobile)
2. Carrier detection (МТС, Билайн, Мегафон)
3. Auto-reconnect при смене сети
4. Adaptive protocol selection (по оператору)

**Deliverables:**
- `NetworkStateMonitor.kt`
- `CarrierDetector.kt`
- `AdaptiveProtocolSelector.kt`

**Логика:**
```
МТС → prefer Hysteria2 (агрессивный DPI на TCP)
Билайн → Reality + obfs
Мегафон → Shadowsocks
Wi-Fi → Reality (работает хорошо)
```

---

### **Подэтап 3.5.2: Battery & Data Optimization**
⏱️ **2-3 дня**

**Задачи:**
1. Adaptive keepalive (зависит от сети)
2. Suspend VPN при неактивности
3. Data compression (если возможно)
4. Battery usage optimization

**Deliverables:**
- `BatteryOptimizer.kt`
- `DataUsageTracker.kt`
- Settings UI для оптимизаций

---

### **Подэтап 3.5.3: Split Tunneling UI**
⏱️ **2 дня**

**Задачи:**
1. Per-app VPN (выбор приложений)
2. Domain-based rules UI
3. Statistics (сколько трафика через VPN vs Direct)
4. Quick toggle для режимов (All/Smart/Off)

**Deliverables:**
- `SplitTunnelingFragment.kt`
- `AppSelectorDialog.kt`
- Statistics dashboard

---

## **ЭТАП 3.6: Backend Monitoring & Analytics** (~1 неделя)

**Цель:** Мониторинг эффективности обхода блокировок.

### **Подэтап 3.6.1: Telemetry Collection**
⏱️ **3 дня**

**Задачи:**
1. Клиент отправляет metrics (protocol, carrier, success/fail)
2. Backend aggregation
3. Database schema для telemetry
4. API endpoint для приема данных

**Deliverables:**
- `TelemetryService.kt` (Android)
- `TelemetryController.java` (Backend)
- Migration: `V13__create_telemetry.sql`

**Metrics:**
```json
{
  "userId": 123456789,
  "timestamp": "2026-03-10T10:00:00Z",
  "carrier": "MTS",
  "protocol": "REALITY",
  "connectionSuccess": true,
  "latency": 45,
  "failureReason": null
}
```

---

### **Подэтап 3.6.2: Server Reachability Monitor**
⏱️ **3 дня**

**Задачи:**
1. Periodic checks из разных ISP (МТС, Билайн, etc)
2. DPI detection (ActiveProbing simulation)
3. Auto-mark servers as blocked/clean
4. Telegram alerts для админов

**Deliverables:**
- `ServerReachabilityMonitor.java`
- `DpiDetector.java`
- `TelegramAlertService.java`

---

### **Подэтап 3.6.3: Analytics Dashboard**
⏱️ **1-2 дня**

**Задачи:**
1. Connection success rate by ISP
2. Protocol effectiveness charts
3. Most blocked domains
4. Geographic distribution

**Deliverables:**
- Admin dashboard (React)
- API: `GET /api/v1/admin/analytics`

---

## **ЭТАП 3.7: Testing & Quality Assurance** (~1 неделя)

### **Подэтап 3.7.1: Real-world Testing**
⏱️ **3 дня**

**Задачи:**
1. Тестирование на МТС/Билайн/Мегафон SIM
2. Проверка блокировки YouTube, Instagram, Twitter
3. Performance testing (скорость, latency)
4. Battery drain testing

**Deliverables:**
- Test report по каждому оператору
- Bug fixes

---

### **Подэтап 3.7.2: Edge Cases & Fallbacks**
⏱️ **2 дня**

**Задачи:**
1. Сеть полностью недоступна
2. Все протоколы заблокированы
3. DNS poisoning
4. ActiveProbing атаки

**Deliverables:**
- Graceful degradation
- User-friendly error messages

---

### **Подэтап 3.7.3: Documentation**
⏱️ **2 дня**

**Задачи:**
1. Архитектура Smart Mode
2. Troubleshooting guide
3. Deployment instructions
4. User manual

**Deliverables:**
- `SMART_MODE_ARCHITECTURE.md`
- `TROUBLESHOOTING.md`
- `USER_GUIDE.md`

---

## 🛠️ ТЕХНОЛОГИЧЕСКИЙ СТЕК

### **Android:**
- Kotlin 1.9+
- Android VpnService API
- Xray-core Android (libxray.so)
- uTLS (Go mobile)
- Shadowsocks-libev
- Hysteria2
- dnsjava (DNS)
- OkHttp (DoH)
- Room (local DB)
- WorkManager (background tasks)

### **Backend:**
- Spring Boot 3.2
- PostgreSQL (domain lists)
- Redis (cache)
- Python (парсер RKN)
- MaxMind GeoIP2
- Prometheus (metrics)

### **Infrastructure:**
- Xray-core 1.8.16+
- Shadowsocks-libev
- Hysteria2 server
- Nginx (with ECH)

---

## 📊 METRICS & KPIs

**Success Criteria:**
- ✅ Connection success rate > 95% на всех мобильных операторах
- ✅ Fallback работает < 5 секунд
- ✅ Battery drain < 5% за час
- ✅ Split tunneling работает корректно
- ✅ DoH/DoT работает даже при DNS блокировке

---

## ⚠️ РИСКИ И МИТИГАЦИЯ

| Риск | Вероятность | Митигация |
|------|------------|-----------|
| **Блокировка всех протоколов** | Средняя | Multiple fallbacks + domain fronting |
| **ActiveProbing атаки** | Высокая | ECH, uTLS randomization |
| **IP блокировка серверов** | Высокая | Rotation pool, CDN fronting |
| **DNS poisoning** | Очень высокая | DoH/DoT обязательно |
| **Throttling скорости** | Средняя | Hysteria2 (brutal congestion control) |

---

## 🎯 ПРИОРИТИЗАЦИЯ

### **MUST HAVE (критично):**
1. ✅ Domain Classification (3.1)
2. ✅ Smart Routing Engine (3.2)
3. ✅ DoH/DoT (3.2.2)
4. ✅ Multiple Protocols (3.3)
5. ✅ Basic telemetry (3.6.1)

### **SHOULD HAVE (важно):**
1. ⚠️ Traffic Obfuscation (3.4.3)
2. ⚠️ Mobile optimizations (3.5)
3. ⚠️ Analytics dashboard (3.6.3)

### **NICE TO HAVE (желательно):**
1. 💡 ECH support (3.4.2)
2. 💡 uTLS (3.4.1)
3. 💡 Per-app VPN (3.5.3)

---

## 📅 ВРЕМЕННАЯ ШКАЛА (ОПТИМИСТИЧНАЯ)

```
Неделя 1:  Этап 3.1 (Domain Classification)
Неделя 2:  Этап 3.2 (Smart Routing)
Неделя 3:  Этап 3.3.1-3.3.2 (Shadowsocks + Hysteria2)
Неделя 4:  Этап 3.3.3 + 3.4.1 (Fallback + uTLS)
Неделя 5:  Этап 3.4.2-3.4.3 (ECH + Obfuscation)
Неделя 6:  Этап 3.5 + 3.6 (Mobile + Monitoring)
Неделя 7:  Этап 3.7 (Testing + Docs)
```

**ИТОГО: ~7 недель** (с запасом)

---

## 🚀 СЛЕДУЮЩИЕ ШАГИ

**Хочешь начать с:**

**ВАРИАНТ A (Backend-first):**
1. Этап 3.1.1 - Blocked Domains Database
2. Этап 3.1.2 - GeoIP Integration
3. Этап 3.1.3 - Auto-update

**ВАРИАНТ B (Android-first):**
1. Этап 3.2.1 - VPN Service Architecture
2. Этап 3.2.2 - DoH/DoT Client
3. Этап 3.2.3 - Rule Engine

**ВАРИАНТ C (Полный цикл):**
1. Сначала Backend (3.1) → потом Android (3.2) → потом Protocols (3.3)

---

**Что выбираешь? Давай начнём с конкретного подэтапа и сделаем его идеально!** 🎯
