$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqlHome = 'C:\Program Files\MySQL\MySQL Server 9.7'
$mysqlData = Join-Path $repoRoot '.local-mysql\data'
$mysqlConfig = Join-Path $repoRoot '.local-mysql\my-project.ini'
$mysqlExe = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysqlClient = Join-Path $mysqlHome 'bin\mysql.exe'

if (-not (Test-Path $mysqlExe)) {
  throw "MySQL server not found at $mysqlExe"
}

if (-not (Test-Path $mysqlData)) {
  New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $repoRoot '.local-mysql\uploads') | Out-Null
  & $mysqlExe --defaults-file="$mysqlConfig" --initialize-insecure --console
}

$listener = Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
  $out = Join-Path $repoRoot '.local-mysql\mysqld.out.log'
  $err = Join-Path $repoRoot '.local-mysql\mysqld.err.log'
  Start-Process -FilePath $mysqlExe `
    -ArgumentList @("--defaults-file=$mysqlConfig", '--console') `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $out `
    -RedirectStandardError $err

  $ready = $false
  for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) {
      $ready = $true
      break
    }
  }
  if (-not $ready) {
    throw 'Project MySQL did not start on port 3307. Check .local-mysql/mysql.err'
  }
}

& $mysqlClient -h 127.0.0.1 -P 3307 -u root -e @"
CREATE DATABASE IF NOT EXISTS cloudbrain_medical CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'cloudbrain'@'localhost' IDENTIFIED BY 'cloudbrain_dev';
CREATE USER IF NOT EXISTS 'cloudbrain'@'127.0.0.1' IDENTIFIED BY 'cloudbrain_dev';
GRANT ALL PRIVILEGES ON cloudbrain_medical.* TO 'cloudbrain'@'localhost';
GRANT ALL PRIVILEGES ON cloudbrain_medical.* TO 'cloudbrain'@'127.0.0.1';
FLUSH PRIVILEGES;
"@

Write-Host 'Project MySQL is ready on 127.0.0.1:3307'
