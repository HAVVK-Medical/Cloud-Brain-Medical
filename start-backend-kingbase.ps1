param(
  [int]$Port = 8088
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$env:SPRING_PROFILES_ACTIVE = 'kingbase'
if (-not $env:DB_URL) {
  $env:DB_URL = 'jdbc:postgresql://127.0.0.1:54321/cloudbrain_medical?currentSchema=public'
}
if (-not $env:DB_USERNAME) {
  $env:DB_USERNAME = 'cloudbrain'
}
if (-not $env:DB_PASSWORD) {
  $env:DB_PASSWORD = 'cloudbrain_dev'
}
$env:SERVER_PORT = "$Port"

& (Join-Path $repoRoot 'mvnw.cmd') -pl backend spring-boot:run
