#!/bin/bash
# install-hysteria2-antiblock.sh
# Установка Hysteria2 с оптимизацией для обхода блокировок

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ "$EUID" -ne 0 ]; then
   echo -e "${RED}Требуются root права${NC}"
   exit 1
fi

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Установка Hysteria2 (Anti-Block Edition)    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================
# ПАРАМЕТРЫ
# ============================================
HY2_PORT="${HY2_PORT:-8443}"
HY2_PASSWORD="${HY2_PASSWORD:-}"
HY2_OBFS_PASSWORD="${HY2_OBFS_PASSWORD:-}"

# ============================================
# 1. УСТАНОВКА HYSTERIA2
# ============================================
echo -e "${GREEN}[1/5]${NC} Установка Hysteria2..."

bash <(curl -fsSL https://get.hy2.sh/)

echo -e "${GREEN}✓${NC} Hysteria2 установлен"
echo ""

# ============================================
# 2. ГЕНЕРАЦИЯ ПАРОЛЕЙ
# ============================================
echo -e "${GREEN}[2/5]${NC} Генерация паролей..."

if [ -z "$HY2_PASSWORD" ]; then
    HY2_PASSWORD=$(openssl rand -base64 32)
fi

if [ -z "$HY2_OBFS_PASSWORD" ]; then
    HY2_OBFS_PASSWORD=$(openssl rand -base64 16)
fi

echo -e "${YELLOW}Пароли:${NC}"
echo -e "  Auth Password: ${GREEN}$HY2_PASSWORD${NC}"
echo -e "  Obfs Password: ${GREEN}$HY2_OBFS_PASSWORD${NC}"
echo ""

# ============================================
# 3. ГЕНЕРАЦИЯ САМОПОДПИСАННОГО СЕРТИФИКАТА
# ============================================
echo -e "${GREEN}[3/5]${NC} Генерация TLS сертификата..."

mkdir -p /etc/hysteria

# Генерация приватного ключа
openssl ecparam -genkey -name prime256v1 -out /etc/hysteria/server.key

# Генерация самоподписанного сертификата
# ВАЖНО: Используем случайный домен для обхода SNI фильтрации
RANDOM_DOMAIN="www.$(openssl rand -hex 8).com"

openssl req -new -x509 -days 36500 -key /etc/hysteria/server.key \
    -out /etc/hysteria/server.crt \
    -subj "/CN=${RANDOM_DOMAIN}"

echo -e "${GREEN}✓${NC} Сертификат создан (CN: $RANDOM_DOMAIN)"
echo ""

# ============================================
# 4. КОНФИГУРАЦИЯ HYSTERIA2 (ANTI-BLOCK)
# ============================================
echo -e "${GREEN}[4/5]${NC} Создание конфигурации..."

cat > /etc/hysteria/config.yaml <<EOF
# Hysteria2 Anti-Block Configuration

# Listen address
listen: :$HY2_PORT

# TLS configuration
tls:
  cert: /etc/hysteria/server.crt
  key: /etc/hysteria/server.key

# Authentication
auth:
  type: password
  password: $HY2_PASSWORD

# OBFUSCATION (критично для обхода DPI!)
obfs:
  type: salamander
  salamander:
    password: $HY2_OBFS_PASSWORD

# QUIC settings (оптимизация для мобильных сетей)
quic:
  initStreamReceiveWindow: 8388608      # 8 MB
  maxStreamReceiveWindow: 8388608       # 8 MB
  initConnReceiveWindow: 20971520       # 20 MB
  maxConnReceiveWindow: 20971520        # 20 MB
  maxIdleTimeout: 30s
  maxIncomingStreams: 1024
  disablePathMTUDiscovery: false

# Bandwidth settings
bandwidth:
  up: 1 gbps
  down: 1 gbps

# Speed test
speedTest: true

# Ignore client bandwidth
ignoreClientBandwidth: false

# Disable UDP (опционально, если UDP блокируется)
# disableUDP: false

# MASQUERADE (маскировка под HTTP/3)
masquerade:
  type: proxy
  proxy:
    url: https://www.bing.com
    rewriteHost: true

# ACL (Access Control List) - опционально
# acl:
#   file: /etc/hysteria/acl.txt

# Logging
log:
  level: info
  file: /var/log/hysteria/server.log
EOF

chmod 600 /etc/hysteria/config.yaml

echo -e "${GREEN}✓${NC} Конфигурация создана"
echo ""

# ============================================
# 5. SYSTEMD SERVICE
# ============================================
echo -e "${GREEN}[5/5]${NC} Создание systemd service..."

cat > /etc/systemd/system/hysteria-server.service <<'SERVICE'
[Unit]
Description=Hysteria2 Server
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/hysteria server -c /etc/hysteria/config.yaml
Restart=on-failure
RestartSec=5s
LimitNOFILE=1048576

# Security
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
SERVICE

mkdir -p /var/log/hysteria

systemctl daemon-reload
systemctl enable hysteria-server
systemctl start hysteria-server

sleep 2

if systemctl is-active --quiet hysteria-server; then
    echo -e "${GREEN}✓${NC} Hysteria2 запущен"
else
    echo -e "${RED}✗${NC} Ошибка запуска!"
    systemctl status hysteria-server
    exit 1
fi

echo ""

# ============================================
# 6. FIREWALL
# ============================================
echo -e "${GREEN}Настройка firewall...${NC}"

if command -v ufw &> /dev/null; then
    ufw allow $HY2_PORT/udp
    echo -e "${GREEN}✓${NC} UFW правило добавлено"
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=$HY2_PORT/udp
    firewall-cmd --reload
    echo -e "${GREEN}✓${NC} Firewalld правило добавлено"
fi

echo ""

# ============================================
# СОХРАНЕНИЕ ПАРАМЕТРОВ
# ============================================
cat > /root/hysteria2-config.txt <<CONF
# Hysteria2 Configuration
# Generated: $(date)

SERVER=$(curl -s ifconfig.me || hostname -I | awk '{print $1}')
PORT=$HY2_PORT
PASSWORD=$HY2_PASSWORD
OBFS_PASSWORD=$HY2_OBFS_PASSWORD
OBFS_TYPE=salamander
TLS_INSECURE=true

# Hysteria2 URI (для клиента):
# hysteria2://$HY2_PASSWORD@$(curl -s ifconfig.me):$HY2_PORT?obfs=salamander&obfs-password=$HY2_OBFS_PASSWORD&sni=www.bing.com&insecure=1#Hysteria2-AntiBlock

# Для Backend используй эти параметры
CONF

echo -e "${GREEN}✓${NC} Параметры сохранены в /root/hysteria2-config.txt"
echo ""

# ============================================
# ФИНАЛЬНАЯ ИНФОРМАЦИЯ
# ============================================
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           УСТАНОВКА ЗАВЕРШЕНА!                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Hysteria2 Anti-Block Configuration:${NC}"
echo -e "  Server:   $(curl -s ifconfig.me)"
echo -e "  Port:     ${HY2_PORT} (UDP)"
echo -e "  Password: ${HY2_PASSWORD}"
echo -e "  Obfs:     salamander"
echo -e "  Obfs Pwd: ${HY2_OBFS_PASSWORD}"
echo ""
echo -e "${GREEN}Anti-Block Features:${NC}"
echo -e "  ✓ Salamander obfuscation (обход DPI)"
echo -e "  ✓ QUIC protocol (маскировка под HTTP/3)"
echo -e "  ✓ Random TLS cert (обход SNI фильтрации)"
echo -e "  ✓ Masquerade as Bing.com"
echo -e "  ✓ Optimized for LTE/5G"
echo ""
echo -e "${GREEN}Файлы:${NC}"
echo -e "  Config:   /etc/hysteria/config.yaml"
echo -e "  Cert:     /etc/hysteria/server.crt"
echo -e "  Params:   /root/hysteria2-config.txt"
echo ""
echo -e "${GREEN}Управление:${NC}"
echo -e "  Status:   systemctl status hysteria-server"
echo -e "  Logs:     journalctl -u hysteria-server -f"
echo -e "  Restart:  systemctl restart hysteria-server"
echo ""
