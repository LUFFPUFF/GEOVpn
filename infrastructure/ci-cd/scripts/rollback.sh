#!/bin/bash

set -e

echo "Starting rollback"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

COMPOSE_FILE="infrastructure/docker/docker-compose.prod.yml"
BACKUP_DIR="/opt/backups/deployments"

LATEST_BACKUP=$(ls -t ${BACKUP_DIR}/docker-compose.*.yml 2>/dev/null | head -n 1)

if [ -z "$LATEST_BACKUP" ]; then
    echo -e "${RED}No backup found!${NC}"
    exit 1
fi

echo -e "${GREEN}Found backup: ${LATEST_BACKUP}${NC}"

echo "Restoring services from backup"
cp ${LATEST_BACKUP} ${COMPOSE_FILE}.rollback

docker-compose -f ${COMPOSE_FILE}.rollback down
docker-compose -f ${COMPOSE_FILE}.rollback up -d

echo -e "${GREEN} Rollback complete!${NC}"

./infrastructure/ci-cd/scripts/health-check.sh