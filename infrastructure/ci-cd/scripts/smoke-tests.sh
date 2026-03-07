#!/bin/bash

set -e

HOST=${1:-localhost}
BASE_URL="https://${HOST}"

echo "Running smoke tests against ${BASE_URL}..."

echo -n "Test 1: Health endpoint"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/health)
if [ "$RESPONSE" == "200" ]; then
    echo "PASSED"
else
    echo "FAILED"
    exit 1
fi

echo -n "Test 2: API Gateway routing"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/api/v1/servers/active \
    -H "X-Internal-Secret: ${INTERNAL_SECRET}")
if [ "$RESPONSE" == "200" ] || [ "$RESPONSE" == "401" ]; then
    echo "PASSED"
else
    echo "FAILED"
    exit 1
fi

echo -n "Test 3: Database connectivity"
RESPONSE=$(curl -s ${BASE_URL}/actuator/health | grep -q "db.*UP" && echo "UP" || echo "DOWN")
if [ "$RESPONSE" == "UP" ]; then
    echo "PASSED"
else
    echo "FAILED"
    exit 1
fi

echo "All smoke tests passed!"
```
