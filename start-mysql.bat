@echo off
if not defined MYSQL_HOME (
    echo ERROR: MYSQL_HOME environment variable not set.
    echo Please set MYSQL_HOME to your MySQL installation directory.
    echo Example: set MYSQL_HOME=D:\Program Files\MySQL\mysql-8.0.46-winx64
    pause
    exit /b 1
)
echo Starting MySQL...
"%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%~dp0.local-mysql\my-project.ini" --console
pause
