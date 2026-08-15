#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('ry_cloud', 'ry_config') AND pid <> pg_backend_pid();
    DROP DATABASE IF EXISTS ry_cloud;
    DROP DATABASE IF EXISTS ry_config;
    CREATE DATABASE ry_cloud ENCODING 'UTF8';
    CREATE DATABASE ry_config ENCODING 'UTF8';
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname ry_cloud -f /docker-entrypoint-initdb.d/ry_cloud.sql
# Docker �����ڷ���ͨ�������� ruoyi-postgres �������ݿ�
sed 's/localhost/ruoyi-postgres/g' /docker-entrypoint-initdb.d/ry_config.sql | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname ry_config
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname ry_config -f /docker-entrypoint-initdb.d/ry_config_supplement.sql
