param(
  [int]$Port = 8088,
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 54321,
  [string]$DbName = "cloudbrain_medical",
  [string]$DbUser = "cloudbrain",
  [string]$DbPassword = "cloudbrain_dev"
)

$ErrorActionPreference = "Stop"

$env:SPRING_PROFILES_ACTIVE = "kingbase"
$env:SERVER_PORT = "$Port"
$env:DB_URL = "jdbc:kingbase8://$DbHost`:$DbPort/$DbName?currentSchema=public"
$env:DB_USERNAME = $DbUser
$env:DB_PASSWORD = $DbPassword

.\mvnw.cmd -pl backend spring-boot:run
