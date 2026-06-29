@echo off
set SPRING_PROFILES_ACTIVE=mysql
set DB_URL=jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
set DB_USERNAME=cloudbrain
set DB_PASSWORD=cloudbrain_dev
set SERVER_PORT=8088
cd /d "%~dp0"
echo Starting Spring Boot...
call mvnw.cmd -pl backend spring-boot:run
