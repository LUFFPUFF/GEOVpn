#!/bin/bash
# Установка Shadowsocks-libev с simple-obfs plugin

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
echo -e "${BLUE}║      Установка Shadowsocks-libev + obfs        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

SS_PORT="${SS_PORT:-8388}"
SS_PASSWORD="${SS_PASSWORD:-}"
SS_METHOD="${SS_METHOD:-chacha20-ietf-poly1305}"

echo -e "${GREEN}[1/6]${NC} Установка зависимостей..."

apt-get update -qq
apt-get install -y \
    build-essential \
    autoconf \
    libtool \
    libssl-dev \
    libpcre3-dev \
    libev-dev \
    asciidoc \
    xmlto \
    automake \
    git \
    wget \
    curl

echo -e "${GREEN}✓${NC} Зависимости установлены"
echo ""

echo -e "${GREEN}[2/6]${NC} Установка Shadowsocks-libev..."

if command -v apt-get &> /dev/null; then
    apt-get install -y shadowsocks-libev
elif command -v yum &> /dev/null; then
    yum install -y epel-release
    yum install -y shadowsocks-libev
else
    echo -e "${RED}Неподдерживаемая ОС${NC}"
    exit 1
fi

echo -e "${GREEN}✓${NC} Shadowsocks-libev установлен"
echo ""

echo -e "${GREEN}[3/6]${NC} Установка simple-obfs plugin..."

cd /tmp
git clone https://github.com/shadowsocks/simple-obfs.git
cd simple-obfs
git submodule update --init --recursive

./autogen.sh
./configure
make
make install

cp /usr/local/bin/obfs-server /usr/bin/obfs-server

echo -e "${GREEN}✓${NC} simple-obfs установлен"
echo ""

echo -e "${GREEN}[4/6]${NC} Генерация конфигурации..."

if [ -z "$SS_PASSWORD" ]; then
    SS_PASSWORD=$(openssl rand -base64 32)
    echo -e "${YELLOW}Сгенерирован пароль:${NC} ${GREEN}$SS_PASSWORD${NC}"
fi

mkdir -p /etc/shadowsocks-libev

cat > /etc/shadowsocks-libev/config.json <<EOF
{
    "server": "0.0.0.0",
    "server_port": $SS_PORT,
    "password": "$SS_PASSWORD",
    "timeout": 300,
    "method": "$SS_METHOD",
    "fast_open": true,
    "nameserver": "1.1.1.1",
    "mode": "tcp_and_udp",
    "plugin": "obfs-server",
    "plugin_opts": "obfs=http"
}
EOF

echo -e "${GREEN}✓${NC} Конфигурация создана"
echo ""

cat > /root/shadowsocks-config.txt <<EOF
# Shadowsocks Configuration
# Generated: $(date)

SS_SERVER=$(curl -s ifconfig.me || hostname -I | awk '{print $1}')
SS_PORT=$SS_PORT
SS_PASSWORD=$SS_PASSWORD
SS_METHOD=$SS_METHOD
SS_PLUGIN=obfs-local
SS_PLUGIN_OPTS=obfs=http;obfs-host=www.bing.com

# SS URI:
# ss://$(echo -n "$SS_METHOD:$SS_PASSWORD" | base64)@$(curl -s ifconfig.me):$SS_PORT/?plugin=obfs-local%3Bobfs%3Dhttp%3Bobfs-host%3Dwww.bing.com

# Используй эти параметры в Backend (VPN Config Service)
EOF

echo -e "${GREEN}✓${NC} Параметры сохранены в /root/shadowsocks-config.txt"
echo ""

echo -e "${GREEN}[5/6]${NC} Создание systemd service..."

cat > /etc/systemd/system/shadowsocks-libev.service <<'SERVICE'
[Unit]
Description=Shadowsocks-libev Server
After=network.target

[Service]
Type=simple
User=nobody
Group=nogroup
ExecStart=/usr/bin/ss-server -c /etc/shadowsocks-libev/config.json
Restart=on-failure
RestartSec=5s

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/run

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable shadowsocks-libev
systemctl start shadowsocks-libev

sleep 2

if systemctl is-active --quiet shadowsocks-libev; then
    echo -e "${GREEN}✓${NC} Shadowsocks успешно запущен"
else
    echo -e "${RED}✗${NC} Ошибка запуска Shadowsocks"
    systemctl status shadowsocks-libev
    exit 1
fi

echo ""

echo -e "${GREEN}[6/6]${NC} Настройка firewall..."

if command -v ufw &> /dev/null; then
    ufw allow $SS_PORT/tcp
    ufw allow $SS_PORT/udp
    echo -e "${GREEN}✓${NC} UFW правила добавлены"
elif command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=$SS_PORT/tcp
    firewall-cmd --permanent --add-port=$SS_PORT/udp
    firewall-cmd --reload
    echo -e "${GREEN}✓${NC} Firewalld правила добавлены"
else
    echo -e "${YELLOW}⚠${NC} Firewall не найден, настрой порт $SS_PORT вручную"
fi

echo ""

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           УСТАНОВКА ЗАВЕРШЕНА!                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Shadowsocks Configuration:${NC}"
echo -e "  Server:   $(curl -s ifconfig.me)"
echo -e "  Port:     ${SS_PORT}"
echo -e "  Password: ${SS_PASSWORD}"
echo -e "  Method:   ${SS_METHOD}"
echo -e "  Plugin:   obfs-local (obfs=http)"
echo ""
echo -e "${GREEN}Конфиги:${NC}"
echo -e "  Config:   /etc/shadowsocks-libev/config.json"
echo -e "  Params:   /root/shadowsocks-config.txt"
echo ""
echo -e "${GREEN}Сервисы:${NC}"
echo -e "  Status:   systemctl status shadowsocks-libev"
echo -e "  Logs:     journalctl -u shadowsocks-libev -f"
echo ""