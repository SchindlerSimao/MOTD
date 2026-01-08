#!/bin/sh

# Wait for flyway marker (if volume is mounted)
if [ -d "/flyway/state" ]; then
  echo "Waiting for Flyway migrations to complete..."
  while [ ! -f /flyway/state/done ]; do
    sleep 1
  done
  echo "Flyway migrations completed."
fi

exec java -Dlog4j2.configurationFile=/app/log4j2.xml -jar /app/motd.jar
