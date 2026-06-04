-- 清空 public 下全部业务表（保留 extension），供 reset-db.ps1 就地重建
SET search_path TO public;
SET client_encoding TO 'UTF8';

DROP TABLE IF EXISTS document_chunk CASCADE;
DROP TABLE IF EXISTS document_index_job CASCADE;
DROP TABLE IF EXISTS processed_event CASCADE;
DROP TABLE IF EXISTS doc_metadata CASCADE;
DROP TABLE IF EXISTS upload_task CASCADE;
DROP TABLE IF EXISTS vector_library CASCADE;
