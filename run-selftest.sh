#!/usr/bin/env bash
# Compiles and runs the Minecraft-independent logic and its tests. Needs a JDK (21+).
# Gson is downloaded from Maven Central unless GSON_JAR points at a local copy.
set -euo pipefail
cd "$(dirname "$0")"
if [ -z "${GSON_JAR:-}" ]; then
  mkdir -p build/tools
  GSON_JAR=build/tools/gson-2.13.1.jar
  [ -f "$GSON_JAR" ] || curl -fsSL -o "$GSON_JAR" https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.1/gson-2.13.1.jar
fi
rm -rf build/selftest && mkdir -p build/selftest
javac -Xlint:all -cp "$GSON_JAR" -d build/selftest \
  $(find selftest-stubs selftest src/main/java/com/example/wallsight/client/logic src/main/java/com/example/wallsight/client/config/WallSightConfig.java -name '*.java')
java -cp "build/selftest:$GSON_JAR" com.example.wallsight.SelfTest
