param(
  [string]$KsqlPath = "ksql",
  [string]$HostName = "127.0.0.1",
  [int]$Port = 54321,
  [string]$AdminUser = "SYSTEM",
  [string]$Database = "TEST",
  [string]$AppUser = "cloudbrain",
  [string]$AppPassword = "cloudbrain_dev",
  [string]$AppDatabase = "cloudbrain_medical"
)

$ErrorActionPreference = "Stop"

$sql = @"
DO `$`$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$AppUser') THEN
    EXECUTE format('CREATE USER %I WITH PASSWORD %L', '$AppUser', '$AppPassword');
  END IF;
END
`$`$;
"@

& $KsqlPath -h $HostName -p $Port -U $AdminUser -d $Database -c $sql

$dbExists = & $KsqlPath -h $HostName -p $Port -U $AdminUser -d $Database -tAc "SELECT 1 FROM pg_database WHERE datname = '$AppDatabase'"
if (-not ($dbExists -match "1")) {
  & $KsqlPath -h $HostName -p $Port -U $AdminUser -d $Database -c "CREATE DATABASE $AppDatabase OWNER $AppUser ENCODING 'UTF8'"
}

Write-Host "Kingbase database '$AppDatabase' and user '$AppUser' are ready."
