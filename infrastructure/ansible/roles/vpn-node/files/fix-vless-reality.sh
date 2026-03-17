#!/bin/bash
# fix-vless-reality.sh
# Исправление VLESS Reality конфигурации

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}╔════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║      Исправление VLESS Reality конфига         ║${NC}"
echo -e "${YELLOW}╚════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================
# 1. ГЕНЕРАЦИЯ НОВЫХ REALITY КЛЮЧЕЙ
# ============================================
echo -e "${GREEN}[1/4]${NC} Генерация новых Reality ключей..."

KEYS=$(/usr/local/bin/xray x25519)
REALITY_PRIVATE_KEY=$(echo "$KEYS" | grep 'Private key:' | awk '{print $3}')
REALITY_PUBLIC_KEY=$(echo "$KEYS" | grep 'Public key:' | awk '{print $3}')

echo ""
echo -e "${YELLOW}Новые ключи:${NC}"
echo -e "  Private Key: ${GREEN}${REALITY_PRIVATE_KEY}${NC}"
echo -e "  Public Key:  ${GREEN}${REALITY_PUBLIC_KEY}${NC}"
echo ""

# ============================================
# 2. BACKUP ТЕКУЩЕГО КОНФИГА
# ============================================
echo -e "${GREEN}[2/4]${NC} Создание backup..."

cp /usr/local/etc/xray/config.json /usr/local/etc/xray/config.json.backup.$(date +%Y%m%d_%H%M%S)

echo -e "${GREEN}✓${NC} Backup создан"
echo ""

# ============================================
# 3. ОБНОВЛЕНИЕ КОНФИГА
# ============================================
echo -e "${GREEN}[3/4]${NC} Обновление конфигурации..."

cat > /usr/local/etc/xray/config.json <<EOF
{
  "log": { "loglevel": "warning" },
  "stats": {},
  "api": { "tag": "api", "services": ["StatsService"] },
  "inbounds": [
    {
      "port": 443,
      "protocol": "vless",
      "tag": "vless-reality",
      "settings": {
        "clients": [
          {
            "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "email": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "flow": "xtls-rprx-vision"
          },
          {
            "id": "f0e1d2c3-b4a5-6789-cdef-0123456789ab",
            "email": "f0e1d2c3-b4a5-6789-cdef-0123456789ab",
            "flow": "xtls-rprx-vision"
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "dest": "www.microsoft.com:443",
          "serverNames": [
            "www.microsoft.com",
            "www.apple.com",
            "www.icloud.com"
          ],
          "privateKey": "${REALITY_PRIVATE_KEY}",
          "shortIds": ["c039559afc623939"]
        }
      },
      "sniffing": {
        "enabled": true,
        "destOverride": ["http", "tls", "quic"]
      }
    },
    {
      "listen": "127.0.0.1",
      "port": 10085,
      "protocol": "dokodemo-door",
      "tag": "api-stats",
      "settings": { "address": "127.0.0.1" }
    }
  ],
  "outbounds": [
    { "protocol": "freedom", "tag": "direct" },
    { "protocol": "blackhole", "tag": "block" }
  ],
  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      { "type": "field", "inboundTag": ["api-stats"], "outboundTag": "api" },
      { "type": "field", "ip": ["geoip:private"], "outboundTag": "block" },
      { "type": "field", "protocol": ["bittorrent"], "outboundTag": "block" },
      { "type": "field", "ip": ["geoip:ru"], "outboundTag": "direct" },
      { "type": "field", "domain": ["geosite:category-ru"], "outboundTag": "direct" }
    ]
  }
}
EOF

echo -e "${GREEN}✓${NC} Конфиг обновлен"
echo ""

# ============================================
# 4. ПЕРЕЗАПУСК XRAY
# ============================================
echo -e "${GREEN}[4/4]${NC} Перезапуск Xray..."

systemctl restart xray

sleep 2

if systemctl is-active --quiet xray; then
    echo -e "${GREEN}✓${NC} Xray успешно перезапущен"
else
    echo -e "${RED}✗${NC} Ошибка запуска Xray!"
    echo -e "${YELLOW}Восстанавливаем из backup...${NC}"
    cp /usr/local/etc/xray/config.json.backup.* /usr/local/etc/xray/config.json
    systemctl restart xray
    exit 1
fi

echo ""

# ============================================
# ФИНАЛЬНАЯ ИНФОРМАЦИЯ
# ============================================
echo -e "${YELLOW}╔════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║              ИСПРАВЛЕНИЕ ЗАВЕРШЕНО!            ║${NC}"
echo -e "${YELLOW}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Новые Reality ключи:${NC}"
echo -e "  Private Key: ${REALITY_PRIVATE_KEY}"
echo -e "  Public Key:  ${REALITY_PUBLIC_KEY}"
echo -e "  Short ID:    c039559afc623939"
echo ""
echo -e "${YELLOW}ВАЖНО:${NC}"
echo "1. Обнови PUBLIC KEY в Backend (VPN Config Service)"
echo "2. Пересоздай VLESS конфигурации через API"
echo "3. Старые ссылки работать НЕ БУДУТ!"
echo ""
echo -e "${GREEN}Новая VLESS ссылка (пример):${NC}"
echo "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@193.104.33.209:443?security=reality&sni=www.microsoft.com&fp=chrome&pbk=${REALITY_PUBLIC_KEY}&sid=c039559afc623939&type=tcp&flow=xtls-rprx-vision#Netherlands-01"
echo ""
echo -e "${GREEN}Логи Xray:${NC}"
echo "  journalctl -u xray -f"
echo ""

# Сохраняем ключи
cat > /root/xray-reality-keys-NEW.txt <<KEYS_EOF
# NEW Reality Keys
# Generated: $(date)

REALITY_PRIVATE_KEY=$REALITY_PRIVATE_KEY
REALITY_PUBLIC_KEY=$REALITY_PUBLIC_KEY
REALITY_SHORT_ID=c039559afc623939

# Используй PUBLIC_KEY в Backend!
KEYS_EOF

echo -e "${GREEN}✓${NC} Ключи сохранены в /root/xray-reality-keys-NEW.txt"