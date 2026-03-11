#!/bin/bash

set -e

echo "Running health checks..."

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

SERVICES=(
    "http://localhost:8080/actuator/health"
    "http://localhost:8081/actuator/health"
    "http://localhost:8082/actuator/health"
    "http://localhost:8083/actuator/health"
)

FAILED=0

for SERVICE_URL in "${SERVICES[@]}"; do
    echo -n "Checking ${SERVICE_URL}... "

    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" ${SERVICE_URL} || echo "000")

    if [ "$RESPONSE" == "200" ]; then
        echo -e "${GREEN}OK${NC}"
    else
        echo -e "${RED}FAILED (HTTP ${RESPONSE})${NC}"
        FAILED=1
    fi
done

if [ $FAILED -eq 1 ]; then
    echo -e "${RED}Health check failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All services healthy!${NC}"
    exit 0
fi