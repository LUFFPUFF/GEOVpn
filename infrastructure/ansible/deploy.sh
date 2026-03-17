#!/bin/bash
SERVER_IP="193.104.33.209"
USER="root"
TARGET_DIR="/root/vpn-deploy"

echo "Подготовка сервера..."
ssh $USER@$SERVER_IP "mkdir -p $TARGET_DIR"

echo "Копирование скриптов..."
scp ../../vpn-config-service/src/main/resources/sh/*.sh $USER@$SERVER_IP:$TARGET_DIR/

echo "Запуск установки..."
ssh $USER@$SERVER_IP "cd $TARGET_DIR && chmod +x *.sh && ./install-xray-node.sh && ./install-shadowsocks.sh"

echo "Готово!"