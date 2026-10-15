@echo off
rem ---------------------------------------------------------------------------
rem Builds and runs the app WITHOUT Maven.
rem Requires: JDK 17+ on PATH, and mysql-connector-j-*.jar inside the lib\ folder.
rem (With Maven installed, prefer:  mvn package  then  java -jar target\loan-management-system.jar)
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    set "PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin;%PATH%"
)

if not exist lib\*.jar (
    echo [!] No JAR found in lib\. Download mysql-connector-j from https://dev.mysql.com/downloads/connector/j/
    echo     and copy mysql-connector-j-^<version^>.jar into the lib\ folder.
    exit /b 1
)

if exist out rmdir /s /q out
mkdir out

dir /s /b src\main\java\*.java > out\sources.txt
javac -d out -cp "lib\*" @out\sources.txt
if errorlevel 1 (
    echo [!] Compilation failed.
    exit /b 1
)
xcopy /s /y /q src\main\resources\* out\ > nul

java -cp "out;lib\*" com.loanmanager.Main
endlocal
