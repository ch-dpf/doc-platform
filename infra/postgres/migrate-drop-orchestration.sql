-- 移除已废弃的 ingest_orchestration 表（流水线已固定为代码内实现，不再持久化编排）
SET search_path TO public;
DROP TABLE IF EXISTS ingest_orchestration CASCADE;
