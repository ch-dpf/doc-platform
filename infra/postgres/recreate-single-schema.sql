-- 从双 schema（ingest / vector_idx）迁移到单 schema public 时使用
-- 警告：将删除旧 schema 下全部数据，请先备份

DROP SCHEMA IF EXISTS vector_idx CASCADE;
DROP SCHEMA IF EXISTS ingest CASCADE;

-- 随后执行: psql ... -f infra/postgres/init.sql
