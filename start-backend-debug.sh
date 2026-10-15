#!/bin/bash

# Kill any existing backend process
pkill -f "backend.*jar"

# Wait for process to stop
sleep 2

# Load environment variables from .env
set -a
source .env
set +a

# Start backend with debug logging
echo "Starting backend with debug logging..."
echo "S3_BUCKET=$S3_BUCKET"
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar \
  --logging.level.com.tamixa.application.stream=DEBUG \
  --logging.level.com.tamixa.infrastructure.cdn=DEBUG \
  2>&1 | tee backend-debug.log
