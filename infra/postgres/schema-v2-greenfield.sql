-- v2 Greenfield 全量重建（无数据兼容）。在空库或开发环境执行。
-- Usage: psql -f schema-v2-greenfield.sql

DROP TABLE IF EXISTS chat_message CASCADE;
DROP TABLE IF EXISTS chat_conversation CASCADE;
DROP TABLE IF EXISTS document_chunk CASCADE;
DROP TABLE IF EXISTS document_index_job CASCADE;
DROP TABLE IF EXISTS processed_event CASCADE;
DROP TABLE IF EXISTS doc_metadata CASCADE;
DROP TABLE IF EXISTS upload_task CASCADE;
DROP TABLE IF EXISTS vector_library CASCADE;

\i init.sql
