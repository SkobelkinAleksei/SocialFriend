# Запуск скрипта первого админа.
# 1) Заполните данные в scripts/seed-first-admin.sql
# 2) Дождитесь, пока user и security один раз поднимутся (таблицы создаст Hibernate)
# 3) В корне репозитория:  powershell -File scripts/seed-first-admin.ps1

$ErrorActionPreference = "Stop"
$sql = Join-Path $PSScriptRoot "seed-first-admin.sql"

if (-not (Test-Path $sql)) {
    throw "Не найден $sql"
}

$docker = Get-Command docker -ErrorAction SilentlyContinue
if ($docker) {
    $pg = docker ps --format "{{.Names}}" | Where-Object { $_ -eq "sf-postgres" }
    if ($pg) {
        Get-Content -Raw $sql | docker exec -i sf-postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw "docker exec psql завершился с кодом $LASTEXITCODE" }
        Write-Host "Готово в Docker (sf-postgres). Войдите email/паролем из seed-first-admin.sql"
        exit 0
    }
}

$psql = Get-Command psql -ErrorAction SilentlyContinue
if ($psql) {
    & psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f $sql
    if ($LASTEXITCODE -ne 0) { throw "psql завершился с кодом $LASTEXITCODE" }
    Write-Host "Готово в локальном Postgres. Войдите email/паролем из seed-first-admin.sql"
    exit 0
}

throw "Нужен контейнер sf-postgres или psql в PATH. Файл с данными: scripts/seed-first-admin.sql"
