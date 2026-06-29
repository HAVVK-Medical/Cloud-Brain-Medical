$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Test-Path env:MYSQL_HOME)) {
  throw "MYSQL_HOME environment variable not set. Please set MYSQL_HOME to your MySQL installation directory.`nExample: `$env:MYSQL_HOME = 'D:\Program Files\MySQL\mysql-8.0.46-winx64'"
}
$mysqlHome = $env:MYSQL_HOME
$mysqlData = Join-Path $repoRoot '.local-mysql\data'
$mysqlUploads = Join-Path $repoRoot '.local-mysql\uploads'
$mysqlConfig = Join-Path $repoRoot '.local-mysql\my-project.ini'
$mysqlExe = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysqlClient = Join-Path $mysqlHome 'bin\mysql.exe'

if (-not (Test-Path $mysqlExe)) {
  throw "MySQL server not found at $mysqlExe"
}

# Generate my-project.ini dynamically with correct paths for this machine
$mysqlBasedir = $mysqlHome.Replace('\', '/')
$mysqlDatadir = $mysqlData.Replace('\', '/')
$mysqlUploadsDir = $mysqlUploads.Replace('\', '/')
$mysqlLogErr = (Join-Path $repoRoot '.local-mysql\mysql.err').Replace('\', '/')
$mysqlPid = (Join-Path $repoRoot '.local-mysql\mysql.pid').Replace('\', '/')

$iniContent = @"
[client]
port=3307

[mysqld]
port=3307
basedir=$mysqlBasedir
datadir=$mysqlDatadir
socket=mysql-cloudbrain
default-storage-engine=INNODB
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
lower_case_table_names=1
log-error=$mysqlLogErr
pid-file=$mysqlPid
secure-file-priv=$mysqlUploadsDir
mysqlx=0
"@

New-Item -ItemType Directory -Force -Path (Split-Path $mysqlConfig) | Out-Null
Set-Content -Path $mysqlConfig -Value $iniContent -Encoding ASCII
Write-Host "Generated MySQL config at $mysqlConfig"

if (-not (Test-Path $mysqlData)) {
  New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
  New-Item -ItemType Directory -Force -Path $mysqlUploads | Out-Null
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
