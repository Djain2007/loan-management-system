#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Builds and runs the app WITHOUT Maven.
# Requires: JDK 17+ on PATH, and mysql-connector-j-*.jar inside the lib/ folder.
# (With Maven installed, prefer:  mvn package  then  java -jar target/loan-management-system.jar)
# ---------------------------------------------------------------------------
set -euo pipefail
cd "$(dirname "$0")"

if ! ls lib/*.jar >/dev/null 2>&1; then
    echo "[!] No JAR found in lib/. Download mysql-connector-j from https://dev.mysql.com/downloads/connector/j/"
    echo "    and copy mysql-connector-j-<version>.jar into the lib/ folder."
    exit 1
fi

rm -rf out && mkdir -p out
find src/main/java -name "*.java" > out/sources.txt
javac -d out -cp "lib/*" @out/sources.txt
cp -r src/main/resources/. out/

java -cp "out:lib/*" com.loanmanager.Main
