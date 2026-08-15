-- Initialize PostgreSQL databases for RuoYi-Cloud
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('ry_cloud', 'ry_config') AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS ry_cloud;
DROP DATABASE IF EXISTS ry_config;
CREATE DATABASE ry_cloud ENCODING 'UTF8';
CREATE DATABASE ry_config ENCODING 'UTF8';
