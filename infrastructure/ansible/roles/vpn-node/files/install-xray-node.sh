#!/bin/bash
# Установка и настройка Xray ноды с Reality + Routing Rules + Auto-update GeoData

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
echo -e "${BLUE}║   Установка Xray Node с Reality + Routing     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

XRAY_VERSION="${XRAY_VERSION:-1.8.16}"
REALITY_PRIVATE_KEY="${REALITY_PRIVATE_KEY:-}"
REALITY_SHORT_ID="${REALITY_SHORT_ID:-}"

echo -e "${GREEN}[1/7]${NC} Установка Xray ${XRAY_VERSION}..."

bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install --version $XRAY_VERSION

echo -e "${GREEN}✓${NC} Xray установлен"
echo ""

echo -e "${GREEN}[2/7]${NC} Создание директорий..."

mkdir -p /usr/local/etc/xray
mkdir -p /var/log/xray
mkdir -p /usr/local/share/xray
mkdir -p /root/xray-geodata-backups

chmod 755 /var/log/xray

echo -e "${GREEN}✓${NC} Директории созданы"
echo ""

echo -e "${GREEN}[3/7]${NC} Скачивание GeoIP и GeoSite баз данных..."

wget -q --show-progress \
  https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat \
  -O /usr/local/share/xray/geoip.dat

wget -q --show-progress \
  https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat \
  -O /usr/local/share/xray/geosite.dat

chmod 644 /usr/local/share/xray/geoip.dat
chmod 644 /usr/local/share/xray/geosite.dat

echo -e "${GREEN}✓${NC} GeoIP/GeoSite базы скачаны"
echo ""

echo -e "${GREEN}[4/7]${NC} Генерация Reality ключей..."

if [ -z "$REALITY_PRIVATE_KEY" ] || [ -z "$REALITY_SHORT_ID" ]; then
    echo "Генерируем новые ключи..."

    KEYS=$(/usr/local/bin/xray x25519)
    REALITY_PRIVATE_KEY=$(echo "$KEYS" | grep 'Private key:' | awk '{print $3}')
    REALITY_PUBLIC_KEY=$(echo "$KEYS" | grep 'Public key:' | awk '{print $3}')
    REALITY_SHORT_ID=$(openssl rand -hex 8)

    echo ""
    echo -e "${YELLOW}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║                     КЛЮЧИ!                     ║${NC}"
    echo -e "${YELLOW}╠════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║${NC} Private Key: ${GREEN}${REALITY_PRIVATE_KEY}${NC}"
    echo -e "${YELLOW}║${NC} Public Key:  ${GREEN}${REALITY_PUBLIC_KEY}${NC}"
    echo -e "${YELLOW}║${NC} Short ID:    ${GREEN}${REALITY_SHORT_ID}${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════╝${NC}"
    echo ""

    cat > /root/xray-reality-keys.txt <<EOF
# Xray Reality Keys
# Generated: $(date)

REALITY_PRIVATE_KEY=$REALITY_PRIVATE_KEY
REALITY_PUBLIC_KEY=$REALITY_PUBLIC_KEY
REALITY_SHORT_ID=$REALITY_SHORT_ID

# Используй PUBLIC_KEY и SHORT_ID в Backend (VPN Config Service)
EOF

    echo -e "${GREEN}✓${NC} Ключи сохранены в /root/xray-reality-keys.txt"
else
    echo -e "${GREEN}✓${NC} Используются заданные ключи"
fi

echo ""

echo -e "${GREEN}[5/7]${NC} Создание конфигурации Xray..."

cat > /usr/local/etc/xray/config.json <<'EOF'
{
  "log": {
    "loglevel": "warning",
    "access": "/var/log/xray/access.log",
    "error": "/var/log/xray/error.log"
  },

  "stats": {},

  "api": {
    "tag": "api",
    "services": ["StatsService"]
  },

  "policy": {
    "levels": {
      "0": {
        "statsUserUplink": true,
        "statsUserDownlink": true
      }
    },
    "system": {
      "statsInboundUplink": true,
      "statsInboundDownlink": true,
      "statsOutboundUplink": true,
      "statsOutboundDownlink": true
    }
  },

  "inbounds": [
    {
      "port": 443,
      "protocol": "vless",
      "tag": "vless-reality",
      "settings": {
        "clients": [],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "dest": "www.microsoft.com:443",
          "xver": 0,
          "serverNames": [
            "www.microsoft.com",
            "www.apple.com",
            "www.icloud.com"
          ],
          "privateKey": "REALITY_PRIVATE_KEY_PLACEHOLDER",
          "shortIds": ["REALITY_SHORT_ID_PLACEHOLDER"]
        },
        "tcpSettings": {
          "acceptProxyProtocol": false,
          "header": {
            "type": "none"
          }
        }
      },
      "sniffing": {
        "enabled": true,
        "destOverride": ["http", "tls", "quic"],
        "metadataOnly": false
      }
    },
    {
      "listen": "127.0.0.1",
      "port": 10085,
      "protocol": "dokodemo-door",
      "tag": "api-stats",
      "settings": {
        "address": "127.0.0.1"
      }
    }
  ],

  "outbounds": [
    {
      "protocol": "freedom",
      "tag": "direct",
      "settings": {
        "domainStrategy": "UseIPv4"
      }
    },
    {
      "protocol": "blackhole",
      "tag": "block"
    }
  ],

  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      {
        "type": "field",
        "inboundTag": ["api-stats"],
        "outboundTag": "api"
      },
      {
        "type": "field",
        "ip": ["geoip:private"],
        "outboundTag": "block"
      },
      {
        "type": "field",
        "protocol": ["bittorrent"],
        "outboundTag": "block"
      },
      {
        "type": "field",
        "ip": ["geoip:ru"],
        "outboundTag": "direct"
      },
      {
        "type": "field",
        "domain": [
          "geosite:category-ru",
          "domain:yandex.ru",
          "domain:vk.com",
          "domain:mail.ru",
          "domain:ok.ru",
          "domain:gosuslugi.ru",
          "domain:sberbank.ru"
        ],
        "outboundTag": "direct"
      },
      {
        "type": "field",
        "domain": ["geosite:category-ads-all"],
        "outboundTag": "block"
      }
    ]
  },

  "dns": {
    "servers": [
      {
        "address": "https://1.1.1.1/dns-query",
        "domains": ["geosite:geolocation-!cn"]
      },
      {
        "address": "77.88.8.8",
        "domains": ["geosite:category-ru"]
      },
      "1.1.1.1"
    ],
    "queryStrategy": "UseIPv4"
  }
}
EOF

sed -i "s/REALITY_PRIVATE_KEY_PLACEHOLDER/$REALITY_PRIVATE_KEY/g" /usr/local/etc/xray/config.json
sed -i "s/REALITY_SHORT_ID_PLACEHOLDER/$REALITY_SHORT_ID/g" /usr/local/etc/xray/config.json

echo -e "${GREEN}✓${NC} Конфигурация создана"
echo ""

echo -e "${GREEN}[6/7]${NC} Установка auto-update скрипта для GeoData..."

cat > /usr/local/bin/update-geodata.sh <<'GEODATA_SCRIPT'
#!/bin/bash
# (вставь сюда содержимое update-geodata.sh из предыдущего файла)
GEODATA_SCRIPT

chmod +x /usr/local/bin/update-geodata.sh

cat > /etc/systemd/system/xray-geodata-update.service <<'SERVICE'
[Unit]
Description=Update Xray GeoIP and GeoSite databases
After=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/update-geodata.sh
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SERVICE

cat > /etc/systemd/system/xray-geodata-update.timer <<'TIMER'
[Unit]
Description=Weekly Xray GeoIP/GeoSite update
Requires=xray-geodata-update.service

[Timer]
OnCalendar=Sun *-*-* 03:00:00
Persistent=true
RandomizedDelaySec=3600

[Install]
WantedBy=timers.target
TIMER

systemctl daemon-reload
systemctl enable xray-geodata-update.timer
systemctl start xray-geodata-update.timer

echo -e "${GREEN}✓${NC} Auto-update настроен (каждое воскресенье в 03:00)"
echo ""

echo -e "${GREEN}[7/7]${NC} Запуск Xray..."

systemctl enable xray
systemctl start xray

sleep 2

if systemctl is-active --quiet xray; then
    echo -e "${GREEN}✓${NC} Xray успешно запущен"
else
    echo -e "${RED}✗${NC} Ошибка запуска Xray"
    systemctl status xray
    exit 1
fi

echo ""

echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           УСТАНОВКА ЗАВЕРШЕНА!                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Reality Keys:${NC}"
echo -e "  Private: ${REALITY_PRIVATE_KEY}"
echo -e "  Public:  ${REALITY_PUBLIC_KEY}"
echo -e "  ShortID: ${REALITY_SHORT_ID}"
echo ""
echo -e "${GREEN}Конфиги:${NC}"
echo -e "  Xray:    /usr/local/etc/xray/config.json"
echo -e "  Logs:    /var/log/xray/"
echo -e "  GeoData: /usr/local/share/xray/"
echo ""
echo -e "${GREEN}Сервисы:${NC}"
echo -e "  Xray:         systemctl status xray"
echo -e "  GeoData Timer: systemctl status xray-geodata-update.timer"
echo ""