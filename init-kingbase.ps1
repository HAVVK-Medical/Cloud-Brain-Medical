param(
  [string]$HostName = '127.0.0.1',
  [int]$DbPort = 54321,
  [string]$AdminUser = 'system',
  [string]$AdminDatabase = 'test',
  [string]$KsqlPath = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlPath = Join-Path $repoRoot 'init-kingbase.sql'

if (-not $KsqlPath) {
  $candidates = @(
    'C:\Program Files\Kingbase\ES\V9\Server\bin\ksql.exe',
    'C:\Program Files\Kingbase\ES\V8\Server\bin\ksql.exe',
    'D:\Kingbase\ES\V9\Server\bin\ksql.exe',
    'D:\Kingbase\ES\V8\Server\bin\ksql.exe'
  )
  $KsqlPath = ($candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1)
}

if (-not $KsqlPath) {
  throw 'ksql.exe was not found. Pass -KsqlPath with your Kingbase bin\ksql.exe path after installing KingbaseES.'
}

& $KsqlPath -h $HostName -p $DbPort -U $AdminUser -d $AdminDatabase -f $sqlPath
