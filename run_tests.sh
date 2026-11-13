#!/bin/sh
# Запуск юнит-тестов логики блокировки на обычной JVM (Android не нужен).
set -e
cd "$(dirname "$0")/app"
rm -rf build/test-classes && mkdir -p build/test-classes
javac -encoding UTF-8 -Xlint:all -Werror -d build/test-classes \
  src/main/java/com/shortsblock/app/core/*.java \
  src/test/java/com/shortsblock/app/core/*.java
java -Dstdout.encoding=UTF-8 -cp build/test-classes com.shortsblock.app.core.DetectorTests
