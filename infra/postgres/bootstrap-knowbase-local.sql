-- 本机 PostgreSQL：以超级用户执行，重建 knowbase 角色与空库（会删除 docplatform / knowbase 旧库）
SET client_encoding TO 'UTF8';

SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname IN ('docplatform', 'knowbase')
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS knowbase;
DROP DATABASE IF EXISTS docplatform;

DROP ROLE IF EXISTS knowbase;
DROP ROLE IF EXISTS docplatform;

CREATE ROLE knowbase WITH LOGIN PASSWORD 'knowbase';
CREATE DATABASE knowbase
    OWNER knowbase
    ENCODING 'UTF8'
    TEMPLATE template0;

\c knowbase
CREATE EXTENSION IF NOT EXISTS vector;
GRANT ALL ON SCHEMA public TO knowbase;
