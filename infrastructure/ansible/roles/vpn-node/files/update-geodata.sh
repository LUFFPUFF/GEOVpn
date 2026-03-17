#!/bin/bash
# Автоматическое обновление GeoIP и GeoSite баз данных для Xray
# Запускается через systemd timer раз в неделю

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

DATA_DIR="/usr/local/share/xray"
BACKUP_DIR="/root/xray-geodata-backups"
GEOIP_URL="https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat"
GEOSITE_URL="https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"

LOG_FILE="/var/log/xray/geodata-update.log"

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARN:${NC} $1" | tee -a "$LOG_FILE"
}


if [ "$EUID" -ne 0 ]; then
   log_error "Требуются root права"
   exit 1
fi

mkdir -p "$DATA_DIR"
mkdir -p "$BACKUP_DIR"
mkdir -p "$(dirname $LOG_FILE)"

log "Начало обновления GeoIP/GeoSite баз данных..."
echo ""

log "Создание backup текущих файлов..."

BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)

if [ -f "$DATA_DIR/geoip.dat" ]; then
    cp "$DATA_DIR/geoip.dat" "$BACKUP_DIR/geoip-${BACKUP_TIMESTAMP}.dat"
    log "Backup geoip.dat создан"
fi

if [ -f "$DATA_DIR/geosite.dat" ]; then
    cp "$DATA_DIR/geosite.dat" "$BACKUP_DIR/geosite-${BACKUP_TIMESTAMP}.dat"
    log "Backup geosite.dat создан"
fi

find "$BACKUP_DIR" -name "geo*.dat" -mtime +30 -delete
log "Удалены старые backup'ы (>30 дней)"

echo ""

log "Скачивание geoip.dat..."

if wget -q --show-progress "$GEOIP_URL" -O "$DATA_DIR/geoip.dat.tmp"; then
    FILE_SIZE=$(stat -f%z "$DATA_DIR/geoip.dat.tmp" 2>/dev/null || stat -c%s "$DATA_DIR/geoip.dat.tmp" 2>/dev/null)

    if [ "$FILE_SIZE" -gt 1048576 ]; then
        mv "$DATA_DIR/geoip.dat.tmp" "$DATA_DIR/geoip.dat"
        log "geoip.dat обновлен (размер: $(numfmt --to=iec-i --suffix=B $FILE_SIZE))"
    else
        log_error "geoip.dat слишком маленький ($FILE_SIZE байт), пропускаем"
        rm -f "$DATA_DIR/geoip.dat.tmp"
    fi
else
    log_error "Не удалось скачать geoip.dat"
fi

echo ""

log "Скачивание geosite.dat..."

if wget -q --show-progress "$GEOSITE_URL" -O "$DATA_DIR/geosite.dat.tmp"; then
    FILE_SIZE=$(stat -f%z "$DATA_DIR/geosite.dat.tmp" 2>/dev/null || stat -c%s "$DATA_DIR/geosite.dat.tmp" 2>/dev/null)

    if [ "$FILE_SIZE" -gt 1048576 ]; then
        mv "$DATA_DIR/geosite.dat.tmp" "$DATA_DIR/geosite.dat"
        log "geosite.dat обновлен (размер: $(numfmt --to=iec-i --suffix=B $FILE_SIZE))"
    else
        log_error "geosite.dat слишком маленький ($FILE_SIZE байт), пропускаем"
        rm -f "$DATA_DIR/geosite.dat.tmp"
    fi
else
    log_error "Не удалось скачать geosite.dat"
fi

echo ""

log "Установка прав доступа..."

chmod 644 "$DATA_DIR/geoip.dat" 2>/dev/null || true
chmod 644 "$DATA_DIR/geosite.dat" 2>/dev/null || true

log "Права установлены"

echo ""

log "Перезагрузка Xray для применения изменений..."

if systemctl is-active --quiet xray; then
    systemctl reload xray

    sleep 2

    if systemctl is-active --quiet xray; then
        log "Xray успешно перезагружен"
    else
        log_error "Xray не запустился после reload!"
        log_warn "Восстанавливаем из backup..."

        if [ -f "$BACKUP_DIR/geoip-${BACKUP_TIMESTAMP}.dat" ]; then
            cp "$BACKUP_DIR/geoip-${BACKUP_TIMESTAMP}.dat" "$DATA_DIR/geoip.dat"
        fi

        if [ -f "$BACKUP_DIR/geosite-${BACKUP_TIMESTAMP}.dat" ]; then
            cp "$BACKUP_DIR/geosite-${BACKUP_TIMESTAMP}.dat" "$DATA_DIR/geosite.dat"
        fi

        systemctl restart xray
        exit 1
    fi
else
    log_warn "Xray не запущен, пропускаем перезагрузку"
fi

echo ""

log "Информация о файлах:"

if [ -f "$DATA_DIR/geoip.dat" ]; then
    GEOIP_SIZE=$(stat -f%z "$DATA_DIR/geoip.dat" 2>/dev/null || stat -c%s "$DATA_DIR/geoip.dat" 2>/dev/null)
    GEOIP_DATE=$(stat -f%Sm -t "%Y-%m-%d %H:%M" "$DATA_DIR/geoip.dat" 2>/dev/null || stat -c%y "$DATA_DIR/geoip.dat" 2>/dev/null | cut -d' ' -f1,2)
    log "  geoip.dat: $(numfmt --to=iec-i --suffix=B $GEOIP_SIZE), обновлен: $GEOIP_DATE"
fi

if [ -f "$DATA_DIR/geosite.dat" ]; then
    GEOSITE_SIZE=$(stat -f%z "$DATA_DIR/geosite.dat" 2>/dev/null || stat -c%s "$DATA_DIR/geosite.dat" 2>/dev/null)
    GEOSITE_DATE=$(stat -f%Sm -t "%Y-%m-%d %H:%M" "$DATA_DIR/geosite.dat" 2>/dev/null || stat -c%y "$DATA_DIR/geosite.dat" 2>/dev/null | cut -d' ' -f1,2)
    log "  geosite.dat: $(numfmt --to=iec-i --suffix=B $GEOSITE_SIZE), обновлен: $GEOSITE_DATE"
fi

echo ""

rm -f "$DATA_DIR/*.tmp"

log "Обновление GeoIP/GeoSite завершено успешно!"
log "Backup'ы находятся в: $BACKUP_DIR"

echo ""
exit 0