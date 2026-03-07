#!/bin/bash
# setup-github-secrets-complete.sh
# Полная настройка всех GitHub Secrets для Telegram VPN проекта

set -e

# ============================================
# ЦВЕТА
# ============================================
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║        🔐 TELEGRAM VPN - GITHUB SECRETS SETUP v2.0 🔐           ║
║                                                                  ║
║              Полная настройка всех переменных                    ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"
echo ""

# ============================================
# ПРОВЕРКА ЗАВИСИМОСТЕЙ
# ============================================
echo -e "${CYAN}🔍 Проверка зависимостей...${NC}"

if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI не установлен!${NC}"
    echo "Установи: https://cli.github.com"
    exit 1
fi

if ! command -v openssl &> /dev/null; then
    echo -e "${RED}❌ OpenSSL не установлен!${NC}"
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo -e "${YELLOW}🔑 Требуется авторизация в GitHub CLI${NC}"
    gh auth login
fi

echo -e "${GREEN}✅ Все зависимости установлены${NC}"
echo ""

# ============================================
# ФУНКЦИИ ГЕНЕРАЦИИ СЕКРЕТОВ
# ============================================
generate_jwt_secret() {
    openssl rand -base64 64 | tr -d '\n'
}

generate_internal_secret() {
    openssl rand -hex 32 | tr -d '\n'
}

generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-32 | tr -d '\n'
}

add_secret() {
    local name=$1
    local value=$2
    local description=$3

    echo -e "${YELLOW}📌 ${name}${NC}"
    if [ -n "$description" ]; then
        echo -e "   ${CYAN}ℹ️  ${description}${NC}"
    fi

    gh secret set "$name" --body "$value" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}   ✅ Добавлен${NC}"
    else
        echo -e "${RED}   ❌ Ошибка${NC}"
    fi
    echo ""
}

add_secret_interactive() {
    local name=$1
    local description=$2
    local default=$3

    echo -e "${YELLOW}📌 ${name}${NC}"
    echo -e "   ${CYAN}ℹ️  ${description}${NC}"

    if [ -n "$default" ]; then
        read -p "   Значение [$default]: " value
        value=${value:-$default}
    else
        read -p "   Значение: " value
    fi

    if [ -z "$value" ]; then
        echo -e "${RED}   ⚠️  Пропущено${NC}"
        echo ""
        return
    fi

    gh secret set "$name" --body "$value" 2>/dev/null
    echo -e "${GREEN}   ✅ Добавлен${NC}"
    echo ""
}

# ============================================
# НАЧАЛО НАСТРОЙКИ
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 1: JWT & AUTHENTICATION (4 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Генерируем JWT секреты
JWT_SECRET=$(generate_jwt_secret)
JWT_SECRET_BACKUP=$(generate_jwt_secret)

add_secret "JWT_SECRET" "$JWT_SECRET" "JWT signing key (512 bits, auto-generated)"
add_secret "JWT_SECRET_BACKUP" "$JWT_SECRET_BACKUP" "JWT backup key для ротации"
add_secret "JWT_EXPIRATION" "3600000" "Access token TTL: 1 час (3600000 ms)"
add_secret "JWT_REFRESH_EXPIRATION" "604800000" "Refresh token TTL: 7 дней (604800000 ms)"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 2: INTERNAL SERVICE SECURITY (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

INTERNAL_SECRET=$(generate_internal_secret)
API_GATEWAY_SECRET=$(generate_internal_secret)
ENCRYPTION_KEY=$(generate_jwt_secret)

add_secret "INTERNAL_SECRET" "$INTERNAL_SECRET" "Inter-service authentication (256 bits)"
add_secret "API_GATEWAY_SECRET" "$API_GATEWAY_SECRET" "API Gateway дополнительный секрет"
add_secret "ENCRYPTION_KEY" "$ENCRYPTION_KEY" "Data encryption key для чувствительных данных"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 3: DATABASE (PostgreSQL) (6 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

PRODUCTION_DB_PASSWORD=$(generate_password)

add_secret "PRODUCTION_DB_HOST" "postgres" "Database host (Docker service name)"
add_secret "PRODUCTION_DB_PORT" "5432" "PostgreSQL standard port"
add_secret "PRODUCTION_DB_NAME" "telegram_vpn" "Database name"
add_secret "PRODUCTION_DB_USER" "vpn_admin" "Database user"
add_secret "PRODUCTION_DB_PASSWORD" "$PRODUCTION_DB_PASSWORD" "Database password (auto-generated)"
add_secret "DB_MAX_CONNECTIONS" "20" "Max database connections pool size"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 4: REDIS CACHE (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

PRODUCTION_REDIS_PASSWORD=$(generate_password)

add_secret "PRODUCTION_REDIS_HOST" "redis" "Redis host (Docker service name)"
add_secret "PRODUCTION_REDIS_PORT" "6379" "Redis standard port"
add_secret "PRODUCTION_REDIS_PASSWORD" "$PRODUCTION_REDIS_PASSWORD" "Redis password (auto-generated)"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 5: MICROSERVICES URLS (5 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "USER_SERVICE_URL" "http://user-service:8081" "User Service internal URL"
add_secret "VPN_CONFIG_SERVICE_URL" "http://vpn-config-service:8082" "VPN Config Service internal URL"
add_secret "SERVER_MANAGEMENT_SERVICE_URL" "http://server-management-service:8083" "Server Management internal URL"
add_secret "BILLING_SERVICE_URL" "http://billing-service:8084" "Billing Service internal URL"
add_secret "BLOCKED_DOMAINS_SERVICE_URL" "http://blocked-domains-service:8085" "Blocked Domains Service URL"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 6: SERVICE PORTS (5 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "GATEWAY_PORT" "8080" "API Gateway port"
add_secret "USER_SERVICE_PORT" "8081" "User Service port"
add_secret "VPN_CONFIG_SERVICE_PORT" "8082" "VPN Config Service port"
add_secret "SERVER_MANAGEMENT_SERVICE_PORT" "8083" "Server Management port"
add_secret "BILLING_SERVICE_PORT" "8084" "Billing Service port"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 7: ADMIN CONFIGURATION (1 переменная)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret_interactive "ADMIN_USER_IDS" "Telegram IDs админов (через запятую, например: 123456789,987654321)" "123456789,987654321,999999"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 8: CORS & FRONTEND (1 переменная)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret_interactive "CORS_ALLOWED_ORIGINS" "Разрешенные origins через запятую" "https://yourvpn.com,https://app.yourvpn.com,https://admin.yourvpn.com"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 9: BUSINESS LOGIC (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "REFERRAL_BONUS_REFERRER" "5000" "Бонус рефереру в копейках (50 руб)"
add_secret "REFERRAL_BONUS_NEW_USER" "2500" "Бонус новому пользователю в копейках (25 руб)"
add_secret "REGISTRATION_BONUS" "5000" "Регистрационный бонус в копейках (50 руб)"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 10: VPN CONFIG WEIGHTS (5 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "VPN_LATENCY_WEIGHT" "0.30" "Вес латенси в алгоритме выбора сервера (30%)"
add_secret "VPN_LOAD_WEIGHT" "0.20" "Вес нагрузки в алгоритме (20%)"
add_secret "VPN_GEOGRAPHY_WEIGHT" "0.25" "Вес географии в алгоритме (25%)"
add_secret "VPN_HEALTH_WEIGHT" "0.15" "Вес health в алгоритме (15%)"
add_secret "VPN_PROTOCOL_WEIGHT" "0.10" "Вес протокола в алгоритме (10%)"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 11: FEATURE FLAGS (5 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "MOCK_ENABLED" "false" "Отключить моки в production"
add_secret "ENABLE_REGISTRATION" "true" "Разрешить регистрацию новых пользователей"
add_secret "ENABLE_PAYMENTS" "true" "Включить платежи"
add_secret "ENABLE_REFERRALS" "true" "Включить реферальную систему"
add_secret "ENABLE_RATE_LIMITING" "true" "Включить rate limiting"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 12: SSH DEPLOYMENT - STAGING (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret_interactive "STAGING_HOST" "Staging server hostname (например: staging.yourvpn.com)" ""
add_secret_interactive "STAGING_USER" "SSH user для staging (например: deploy)" "deploy"

echo -e "${YELLOW}📌 STAGING_SSH_KEY${NC}"
echo -e "   ${CYAN}ℹ️  SSH приватный ключ для staging сервера${NC}"
read -p "   Путь к SSH ключу (например: ~/.ssh/id_rsa_staging): " STAGING_SSH_PATH

if [ -n "$STAGING_SSH_PATH" ]; then
    STAGING_SSH_PATH="${STAGING_SSH_PATH/#\~/$HOME}"
    if [ -f "$STAGING_SSH_PATH" ]; then
        gh secret set "STAGING_SSH_KEY" < "$STAGING_SSH_PATH" 2>/dev/null
        echo -e "${GREEN}   ✅ SSH ключ добавлен${NC}"
    else
        echo -e "${RED}   ❌ Файл не найден: $STAGING_SSH_PATH${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️  Пропущено${NC}"
fi
echo ""

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 13: SSH DEPLOYMENT - PRODUCTION (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret_interactive "PRODUCTION_HOST" "Production server hostname (например: yourvpn.com)" ""
add_secret_interactive "PRODUCTION_USER" "SSH user для production" "deploy"

echo -e "${YELLOW}📌 PRODUCTION_SSH_KEY${NC}"
echo -e "   ${CYAN}ℹ️  SSH приватный ключ для production сервера${NC}"
read -p "   Путь к SSH ключу (например: ~/.ssh/id_rsa_production): " PRODUCTION_SSH_PATH

if [ -n "$PRODUCTION_SSH_PATH" ]; then
    PRODUCTION_SSH_PATH="${PRODUCTION_SSH_PATH/#\~/$HOME}"
    if [ -f "$PRODUCTION_SSH_PATH" ]; then
        gh secret set "PRODUCTION_SSH_KEY" < "$PRODUCTION_SSH_PATH" 2>/dev/null
        echo -e "${GREEN}   ✅ SSH ключ добавлен${NC}"
    else
        echo -e "${RED}   ❌ Файл не найден: $PRODUCTION_SSH_PATH${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️  Пропущено${NC}"
fi
echo ""

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 14: TELEGRAM NOTIFICATIONS (2 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${CYAN}ℹ️  Telegram бот для CI/CD уведомлений${NC}"
echo -e "${CYAN}   1. Создай бота через @BotFather${NC}"
echo -e "${CYAN}   2. Создай приватный канал/группу${NC}"
echo -e "${CYAN}   3. Добавь бота в канал как админа${NC}"
echo -e "${CYAN}   4. Получи chat_id через: https://api.telegram.org/bot<TOKEN>/getUpdates${NC}"
echo ""

add_secret_interactive "TELEGRAM_BOT_TOKEN" "Bot token от @BotFather (формат: 123456:ABC-DEF...)" ""
add_secret_interactive "TELEGRAM_CHAT_ID" "Chat/Channel ID (начинается с -, например: -1001234567890)" ""

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 15: TELEGRAM USER BOT (2 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${CYAN}ℹ️  Основной бот для взаимодействия с пользователями${NC}"
echo ""

add_secret_interactive "USER_BOT_TOKEN" "Telegram bot token для пользователей" ""
add_secret_interactive "TELEGRAM_WEBHOOK_URL" "Webhook URL (например: https://yourvpn.com/api/v1/webhooks/telegram)" ""

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 16: PAYMENT PROVIDERS (ОПЦИОНАЛЬНО)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${YELLOW}⚠️  Эти переменные можно пропустить и добавить позже${NC}"
echo ""

add_secret_interactive "YOOMONEY_SHOP_ID" "YooMoney Shop ID (получишь от YooMoney)" ""
add_secret_interactive "YOOMONEY_SECRET_KEY" "YooMoney Secret Key" ""
add_secret_interactive "CRYPTOPAY_API_KEY" "CryptoPay API Key (опционально)" ""

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 17: MONITORING & LOGGING (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "LOG_LEVEL" "INFO" "Logging level (DEBUG, INFO, WARN, ERROR)"
add_secret "SENTRY_DSN" "" "Sentry DSN для error tracking (опционально)"
add_secret "PROMETHEUS_ENABLED" "true" "Включить Prometheus метрики"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  РАЗДЕЛ 18: XRAY & SERVER CONFIG (3 переменных)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

add_secret "XRAY_GRPC_TIMEOUT" "5000" "XRay gRPC timeout в миллисекундах"
add_secret "SERVER_HEALTH_CHECK_INTERVAL" "60" "Интервал проверки серверов в секундах"
add_secret "MAX_DEVICES_PER_USER" "5" "Максимум устройств на пользователя"

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  💾 СОЗДАНИЕ BACKUP ФАЙЛА${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

BACKUP_DIR="infrastructure/secrets"
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/github-secrets-backup-${TIMESTAMP}.env"

cat > "$BACKUP_FILE" << EOF
# ============================================
# TELEGRAM VPN - GitHub Secrets Backup
# Создано: $(date)
# ============================================
# ⚠️  ХРАНИ В БЕЗОПАСНОМ МЕСТЕ!
# ⚠️  НЕ КОММИТЬ В GIT!
# ============================================

# ============================================
# РАЗДЕЛ 1: JWT & AUTHENTICATION
# ============================================
JWT_SECRET=$JWT_SECRET
JWT_SECRET_BACKUP=$JWT_SECRET_BACKUP
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# ============================================
# РАЗДЕЛ 2: INTERNAL SECURITY
# ============================================
INTERNAL_SECRET=$INTERNAL_SECRET
API_GATEWAY_SECRET=$API_GATEWAY_SECRET
ENCRYPTION_KEY=$ENCRYPTION_KEY

# ============================================
# РАЗДЕЛ 3: DATABASE
# ============================================
PRODUCTION_DB_HOST=postgres
PRODUCTION_DB_PORT=5432
PRODUCTION_DB_NAME=telegram_vpn
PRODUCTION_DB_USER=vpn_admin
PRODUCTION_DB_PASSWORD=$PRODUCTION_DB_PASSWORD
DB_MAX_CONNECTIONS=20

# ============================================
# РАЗДЕЛ 4: REDIS
# ============================================
PRODUCTION_REDIS_HOST=redis
PRODUCTION_REDIS_PORT=6379
PRODUCTION_REDIS_PASSWORD=$PRODUCTION_REDIS_PASSWORD

# ============================================
# РАЗДЕЛ 5: MICROSERVICES URLS
# ============================================
USER_SERVICE_URL=http://user-service:8081
VPN_CONFIG_SERVICE_URL=http://vpn-config-service:8082
SERVER_MANAGEMENT_SERVICE_URL=http://server-management-service:8083
BILLING_SERVICE_URL=http://billing-service:8084
BLOCKED_DOMAINS_SERVICE_URL=http://blocked-domains-service:8085

# ============================================
# РАЗДЕЛ 6: SERVICE PORTS
# ============================================
GATEWAY_PORT=8080
USER_SERVICE_PORT=8081
VPN_CONFIG_SERVICE_PORT=8082
SERVER_MANAGEMENT_SERVICE_PORT=8083
BILLING_SERVICE_PORT=8084

# ============================================
# РАЗДЕЛ 7: ADMIN CONFIGURATION
# ============================================
ADMIN_USER_IDS=858441917, 970667053

# ============================================
# РАЗДЕЛ 8: CORS & FRONTEND
# ============================================
CORS_ALLOWED_ORIGINS=https://yourvpn.com,https://app.yourvpn.com

# ============================================
# РАЗДЕЛ 9: BUSINESS LOGIC
# ============================================
REFERRAL_BONUS_REFERRER=5000
REFERRAL_BONUS_NEW_USER=2500
REGISTRATION_BONUS=5000

# ============================================
# РАЗДЕЛ 10: VPN CONFIG WEIGHTS
# ============================================
VPN_LATENCY_WEIGHT=0.30
VPN_LOAD_WEIGHT=0.20
VPN_GEOGRAPHY_WEIGHT=0.25
VPN_HEALTH_WEIGHT=0.15
VPN_PROTOCOL_WEIGHT=0.10

# ============================================
# РАЗДЕЛ 11: FEATURE FLAGS
# ============================================
MOCK_ENABLED=false
ENABLE_REGISTRATION=true
ENABLE_PAYMENTS=true
ENABLE_REFERRALS=true
ENABLE_RATE_LIMITING=true

# ============================================
# РАЗДЕЛ 12-13: DEPLOYMENT (добавь вручную)
# ============================================
# STAGING_HOST=
# STAGING_USER=
# STAGING_SSH_KEY=
# PRODUCTION_HOST=
# PRODUCTION_USER=
# PRODUCTION_SSH_KEY=

# ============================================
# РАЗДЕЛ 14-15: TELEGRAM (добавь вручную)
# ============================================
# TELEGRAM_BOT_TOKEN=
# TELEGRAM_CHAT_ID=
# USER_BOT_TOKEN=
# TELEGRAM_WEBHOOK_URL=

# ============================================
# РАЗДЕЛ 16: PAYMENT PROVIDERS (опционально)
# ============================================
# YOOMONEY_SHOP_ID=
# YOOMONEY_SECRET_KEY=
# CRYPTOPAY_API_KEY=

# ============================================
# РАЗДЕЛ 17: MONITORING
# ============================================
LOG_LEVEL=INFO
SENTRY_DSN=
PROMETHEUS_ENABLED=true

# ============================================
# РАЗДЕЛ 18: XRAY CONFIG
# ============================================
XRAY_GRPC_TIMEOUT=5000
SERVER_HEALTH_CHECK_INTERVAL=60
MAX_DEVICES_PER_USER=5
EOF

echo -e "${GREEN}✅ Backup создан: $BACKUP_FILE${NC}"
echo ""

if command -v gpg &> /dev/null; then
    echo -e "${YELLOW}🔐 Шифрую backup с помощью GPG...${NC}"
    gpg -c "$BACKUP_FILE"

    if [ $? -eq 0 ]; then
        rm "$BACKUP_FILE"
        echo -e "${GREEN}✅ Backup зашифрован: ${BACKUP_FILE}.gpg${NC}"
        echo -e "${CYAN}ℹ️  Расшифровать: gpg ${BACKUP_FILE}.gpg${NC}"
    else
        echo -e "${RED}❌ Ошибка шифрования${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  GPG не установлен, backup не зашифрован${NC}"
    echo -e "${YELLOW}   Установи: brew install gpg${NC}"
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅ НАСТРОЙКА ЗАВЕРШЕНА!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}📊 СТАТИСТИКА:${NC}"
echo ""
gh secret list | head -20
echo ""
echo -e "${CYAN}Всего добавлено переменных: $(gh secret list | wc -l)${NC}"
echo ""