@echo off
setlocal
cd /d "%~dp0"
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    set "PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin;%PATH%"
)
if not exist "target\loan-management-system.jar" (
    echo [!] target\loan-management-system.jar not found. Please build first.
    exit /b 1
)
java -jar target\loan-management-system.jar
endlocal
