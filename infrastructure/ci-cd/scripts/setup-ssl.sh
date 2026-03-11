#!/bin/bash

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║              🔒 SSL CERTIFICATE SETUP (Let's Encrypt) 🔒         ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"
echo ""

DOMAIN=${1:-yourvpn.com}
EMAIL=${2:-admin@yourvpn.com}
STAGING=${3:-false}

CERT_DIR="infrastructure/nginx/certs"
WEBROOT_DIR="infrastructure/nginx/html"

echo -e "${YELLOW}Configuration:${NC}"
echo "  Domain: $DOMAIN"
echo "  Email: $EMAIL"
echo "  Staging: $STAGING"
echo ""

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker не установлен!${NC}"
    exit 1
fi

mkdir -p "$CERT_DIR"
mkdir -p "$WEBROOT_DIR/.well-known/acme-challenge"

echo -e "${GREEN}✅ Директории созданы${NC}"
echo ""

echo -e "${YELLOW}📜 Получаем SSL сертификат...${NC}"
echo ""

STAGING_FLAG=""
if [ "$STAGING" = "true" ]; then
    echo -e "${YELLOW}⚠️  STAGING MODE - тестовые сертификаты${NC}"
    STAGING_FLAG="--staging"
fi

docker run -it --rm \
    --name certbot \
    -v "$(pwd)/$CERT_DIR:/etc/letsencrypt" \
    -v "$(pwd)/$WEBROOT_DIR:/var/www/certbot" \
    certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    $STAGING_FLAG \
    -d "$DOMAIN" \
    -d "api.$DOMAIN" \
    -d "admin.$DOMAIN" \
    -d "www.$DOMAIN"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Сертификат успешно получен!${NC}"
    echo ""

    ln -sf "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" "$CERT_DIR/fullchain.pem"
    ln -sf "/etc/letsencrypt/live/$DOMAIN/privkey.pem" "$CERT_DIR/privkey.pem"
    ln -sf "/etc/letsencrypt/live/$DOMAIN/chain.pem" "$CERT_DIR/chain.pem"

    echo -e "${GREEN}✅ Символические ссылки созданы${NC}"

    openssl x509 -in "$CERT_DIR/fullchain.pem" -text -noout | grep -A2 "Validity"

else
    echo ""
    echo -e "${RED}❌ Ошибка получения сертификата${NC}"
    echo ""
    echo -e "${YELLOW}Возможные причины:${NC}"
    echo "1. Домен не указывает на этот сервер"
    echo "2. Порт 80 недоступен извне"
    echo "3. Nginx не запущен"
    echo ""
    echo -e "${YELLOW}Попробуй:${NC}"
    echo "1. Проверить DNS: dig $DOMAIN"
    echo "2. Проверить порт: nc -zv $DOMAIN 80"
    echo "3. Запустить Nginx: docker-compose up -d nginx"
    echo "4. Использовать staging режим: $0 $DOMAIN $EMAIL true"
    exit 1
fi

echo ""
echo -e "${YELLOW}🔄 Настраиваем автоматическое обновление...${NC}"
echo ""

cat > infrastructure/scripts/renew-ssl.sh << 'RENEWAL_SCRIPT'
#!/bin/bash
# Auto-renewal script

set -e

echo "🔄 Обновление SSL сертификатов..."

docker run --rm \
    -v "$(pwd)/infrastructure/nginx/certs:/etc/letsencrypt" \
    -v "$(pwd)/infrastructure/nginx/html:/var/www/certbot" \
    certbot/certbot renew

if [ $? -eq 0 ]; then
    echo "✅ Сертификаты обновлены"

    # Перезагружаем Nginx
    docker-compose -f infrastructure/docker/docker-compose.prod.yml exec nginx nginx -s reload

    echo "✅ Nginx перезагружен"
else
    echo "❌ Ошибка обновления сертификатов"
    exit 1
fi
RENEWAL_SCRIPT

chmod +x infrastructure/scripts/renew-ssl.sh

echo -e "${GREEN}✅ Скрипт обновления создан${NC}"
echo ""

echo -e "${YELLOW}📅 Для автоматического обновления добавь в crontab:${NC}"
echo ""
echo "  # Обновление SSL сертификатов каждые 12 часов"
echo "  0 */12 * * * cd /opt/telegram-vpn && ./infrastructure/scripts/renew-ssl.sh >> /var/log/certbot-renew.log 2>&1"
echo ""

echo -e "${YELLOW}🔍 Проверяем конфигурацию Nginx...${NC}"
echo ""

docker-compose -f infrastructure/docker/docker-compose.prod.yml exec nginx nginx -t

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Конфигурация Nginx корректна${NC}"

    echo ""
    echo -e "${YELLOW}🔄 Перезагружаем Nginx...${NC}"
    docker-compose -f infrastructure/docker/docker-compose.prod.yml exec nginx nginx -s reload

    echo -e "${GREEN}✅ Nginx перезагружен${NC}"
else
    echo ""
    echo -e "${RED}❌ Ошибка в конфигурации Nginx${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✅ SSL НАСТРОЕН УСПЕШНО!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}📋 Информация о сертификате:${NC}"
echo ""
openssl x509 -in "$CERT_DIR/fullchain.pem" -text -noout | grep -A2 "Subject:"
echo ""
echo -e "${BLUE}📅 Срок действия:${NC}"
openssl x509 -in "$CERT_DIR/fullchain.pem" -text -noout | grep -A2 "Validity"
echo ""
echo -e "${BLUE}🔗 Домены:${NC}"
openssl x509 -in "$CERT_DIR/fullchain.pem" -text -noout | grep "DNS:" | sed 's/DNS://g'