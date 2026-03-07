#!/bin/bash

set -e

echo "Starting deployment"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

COMPOSE_FILE="infrastructure/docker/docker-compose.prod.yml"
BACKUP_DIR="/opt/backups/deployments"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

print_status() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

print_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

mkdir -p ${BACKUP_DIR}

print_status "Creating backup"
docker-compose -f ${COMPOSE_FILE} config > ${BACKUP_DIR}/docker-compose.${TIMESTAMP}.yml
docker-compose -f ${COMPOSE_FILE} ps > ${BACKUP_DIR}/services.${TIMESTAMP}.txt

print_status "Pulling latest images"
docker-compose -f ${COMPOSE_FILE} pull

SERVICES=("postgres" "redis" "server-management-service" "user-service" "vpn-config-service" "api-gateway")

for SERVICE in "${SERVICES[@]}"; do
    print_status "Deploying ${SERVICE}..."

    docker-compose -f ${COMPOSE_FILE} up -d --no-deps --build ${SERVICE}

    print_status "Waiting for ${SERVICE} to be healthy"
    RETRY_COUNT=0
    MAX_RETRIES=30

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if docker-compose -f ${COMPOSE_FILE} ps ${SERVICE} | grep -q "healthy\|Up"; then
            print_status "${SERVICE} is healthy!"
            break
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 2
    done

    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        print_error "${SERVICE} failed to become healthy!"
        exit 1
    fi

    sleep 5
done

print_status "Cleaning up old images"
docker image prune -af --filter "until=72h"

print_status "Deployment complete! Running services:"
docker-compose -f ${COMPOSE_FILE} ps

print_status "Deployment successful!"