@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MVN_VERSION=3.9.16"
set "MVN_DIR=%SCRIPT_DIR%.mvn\apache-maven-%MVN_VERSION%"
set "MVN_ZIP=%SCRIPT_DIR%.mvn\apache-maven-%MVN_VERSION%-bin.zip"
if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME environment variable not set.
  echo Please set JAVA_HOME to a JDK 17+ installation directory.
  echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-17
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: java.exe not found at "%JAVA_HOME%\bin".
  echo Please verify JAVA_HOME points to a valid JDK 17+ installation.
  exit /b 1
)

:have_java
if not exist "%MVN_DIR%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "$zip = '%MVN_ZIP%';" ^
    "$dir = '%MVN_DIR%';" ^
    "$url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip';" ^
    "New-Item -ItemType Directory -Force -Path (Split-Path $zip) | Out-Null;" ^
    "if (-not (Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }" ^
    "if (Test-Path $dir) { Remove-Item -Recurse -Force $dir }" ^
    "Expand-Archive -LiteralPath $zip -DestinationPath (Split-Path $dir)" || exit /b 1
)

"%MVN_DIR%\bin\mvn.cmd" -f "%SCRIPT_DIR%pom.xml" %*
