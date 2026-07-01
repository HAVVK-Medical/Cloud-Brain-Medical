param(
  [int]$Port = 8088
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# --- Auto-discover JDK ---
function Find-JDK {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    if ($env:JDK_HOME -and (Test-Path (Join-Path $env:JDK_HOME 'bin\java.exe'))) {
        return $env:JDK_HOME
    }
    $common = @(
        'C:\Program Files\Java\jdk-21',
        'C:\Program Files\Java\jdk-17',
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.6.6-hotspot',
        'C:\Program Files\Eclipse Adoptium\jdk-17.0.7.7-hotspot'
    )
    foreach ($cp in $common) {
        if (Test-Path (Join-Path $cp 'bin\java.exe')) { return $cp }
    }
    $inPath = (Get-Command java.exe -ErrorAction SilentlyContinue).Source
    if ($inPath) {
        $resolved = if ((Get-Item $inPath).Target) { (Get-Item $inPath).Target } else { $inPath }
        return Split-Path -Parent (Split-Path -Parent $resolved)
    }
    throw "JDK not found. Set `$env:JAVA_HOME to your JDK installation path."
}

$env:JAVA_HOME = Find-JDK
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "JDK found at: $env:JAVA_HOME"

& (Join-Path $repoRoot 'start-local-mysql.ps1')

$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'cloudbrain'
$env:DB_PASSWORD = 'cloudbrain_dev'
$env:SERVER_PORT = "$Port"

& (Join-Path $repoRoot 'mvnw.cmd') -pl backend spring-boot:run
