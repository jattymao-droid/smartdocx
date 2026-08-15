@echo off
REM Initialize PostgreSQL databases for RuoYi-Cloud (Windows)
set PGPASSWORD=mm5621528
set PSQL="C:\Program Files\PostgreSQL\16\bin\psql.exe"
set SQLDIR=%~dp0..\sql\postgresql

echo Creating databases...
%PSQL% -U postgres -h localhost -f "%SQLDIR%\init_databases.sql"

echo Importing ry_cloud schema and data...
%PSQL% -U postgres -h localhost -d ry_cloud -f "%SQLDIR%\ry_cloud.sql"

echo Importing ry_config (Nacos) schema and data...
%PSQL% -U postgres -h localhost -d ry_config -f "%SQLDIR%\ry_config.sql"
%PSQL% -U postgres -h localhost -d ry_config -f "%SQLDIR%\ry_config_supplement.sql"

echo Done. Databases: ry_cloud, ry_config
echo User: postgres / Password: mm5621528
