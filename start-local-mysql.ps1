$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqlData = Join-Path $repoRoot '.local-mysql\data'
$mysqlUploads = Join-Path $repoRoot '.local-mysql\uploads'
$mysqlIniDest = Join-Path $repoRoot '.local-mysql\my-project.ini'

# --- Discover MySQL installation ---
function Find-MySQL {
    # 1) MYSQL_HOME environment variable
    if ($env:MYSQL_HOME -and (Test-Path (Join-Path $env:MYSQL_HOME 'bin\mysqld.exe'))) {
        return $env:MYSQL_HOME
    }

    # 2) PATH search
    $inPath = (Get-Command mysqld.exe -ErrorAction SilentlyContinue).Source
    if ($inPath) {
        return (Split-Path -Parent (Split-Path -Parent $inPath))
    }

    # 3) Registry — MySQL Server 9.x
    $regPaths = @(
        'HKLM:\SOFTWARE\MySQL AB\MySQL Server 9.7',
        'HKLM:\SOFTWARE\MySQL AB\MySQL Server 9.0',
        'HKLM:\SOFTWARE\MySQL AB\MySQL Server 8.4',
        'HKLM:\SOFTWARE\MySQL AB\MySQL Server 8.0',
        'HKLM:\SOFTWARE\WOW6432Node\MySQL AB\MySQL Server 9.7',
        'HKLM:\SOFTWARE\WOW6432Node\MySQL AB\MySQL Server 9.0',
        'HKLM:\SOFTWARE\WOW6432Node\MySQL AB\MySQL Server 8.4',
        'HKLM:\SOFTWARE\WOW6432Node\MySQL AB\MySQL Server 8.0'
    )
    foreach ($rp in $regPaths) {
        try {
            $loc = (Get-ItemProperty -Path $rp -Name 'Location' -ErrorAction Stop).Location
            if ($loc) { return $loc }
        } catch {}
    }

    # 4) Common install paths
    $common = @(
        'C:\Program Files\MySQL\MySQL Server 9.7',
        'C:\Program Files\MySQL\MySQL Server 9.0',
        'C:\Program Files\MySQL\MySQL Server 8.4',
        'C:\Program Files\MySQL\MySQL Server 8.0',
        'C:\mysql-9.7.1-winx64',
        'C:\mysql-8.4.0-winx64',
        'C:\mysql-8.0.33-winx64'
    )
    foreach ($cp in $common) {
        if (Test-Path (Join-Path $cp 'bin\mysqld.exe')) { return $cp }
    }

    throw @"

MySQL Server not found automatically. Please do one of:
  1) Set MYSQL_HOME environment variable pointing to your MySQL install:
     `$env:MYSQL_HOME = 'C:\Program Files\MySQL\MySQL Server X.Y'`
  2) Add MySQL bin directory to your PATH.
  3) Install MySQL from https://dev.mysql.com/downloads/
"@
}

$mysqlHome = Find-MySQL
$mysqlExe = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysqlClient = Join-Path $mysqlHome 'bin\mysql.exe'

Write-Host "MySQL found at: $mysqlHome"

# --- Generate my-project.ini with correct paths ---
$iniContent = @"
[client]
port=3307

[mysqld]
port=3307
basedir=$( $mysqlHome -replace '\\', '/' )
datadir=$( (Join-Path $repoRoot '.local-mysql\data') -replace '\\', '/' )
socket=mysql-cloudbrain
default-storage-engine=INNODB
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
lower_case_table_names=1
log-error=$( (Join-Path $repoRoot '.local-mysql\mysql.err') -replace '\\', '/' )
pid-file=$( (Join-Path $repoRoot '.local-mysql\mysql.pid') -replace '\\', '/' )
secure-file-priv=$( (Join-Path $repoRoot '.local-mysql\uploads') -replace '\\', '/' )
mysqlx=0
"@

Set-Content -Path $mysqlIniDest -Value $iniContent -Encoding ASCII
Write-Host "Generated: $mysqlIniDest"

# --- Initialize data dir if needed ---
if (-not (Test-Path $mysqlData)) {
    New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
    New-Item -ItemType Directory -Force -Path $mysqlUploads | Out-Null
    & $mysqlExe --defaults-file="$mysqlIniDest" --initialize-insecure --console
    Write-Host "MySQL data directory initialized."
}

# --- Start MySQL if not running ---
$listener = Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    Write-Host "Starting project-local MySQL on port 3307..."

    $outLog = Join-Path $repoRoot '.local-mysql\mysqld.out.log'
    $errLog = Join-Path $repoRoot '.local-mysql\mysqld.err.log'

    Start-Process -FilePath $mysqlExe `
        -ArgumentList @("--defaults-file=$mysqlIniDest", '--console') `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog

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

# --- Create database & user ---
& $mysqlClient -h 127.0.0.1 -P 3307 -u root -e @"
CREATE DATABASE IF NOT EXISTS cloudbrain_medical CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'cloudbrain'@'localhost' IDENTIFIED BY 'cloudbrain_dev';
CREATE USER IF NOT EXISTS 'cloudbrain'@'127.0.0.1' IDENTIFIED BY 'cloudbrain_dev';
GRANT ALL PRIVILEGES ON cloudbrain_medical.* TO 'cloudbrain'@'localhost';
GRANT ALL PRIVILEGES ON cloudbrain_medical.* TO 'cloudbrain'@'127.0.0.1';
FLUSH PRIVILEGES;
"@

Write-Host 'Project MySQL is ready on 127.0.0.1:3307'
