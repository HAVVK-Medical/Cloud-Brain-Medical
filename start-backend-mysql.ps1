param(
  [int]$Port = 8088
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $repoRoot 'start-local-mysql.ps1')

$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'cloudbrain'
$env:DB_PASSWORD = 'cloudbrain_dev'
$env:SERVER_PORT = "$Port"

& (Join-Path $repoRoot 'mvnw.cmd') -pl backend spring-boot:run
