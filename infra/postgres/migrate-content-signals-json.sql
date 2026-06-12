-- v2: doc_metadata 内容结构探测快照（ContentSignals）
-- 已有库执行一次即可；新库由 init.sql 建表时自带该列。

ALTER TABLE doc_metadata
    ADD COLUMN IF NOT EXISTS content_signals_json JSONB;

COMMENT ON COLUMN doc_metadata.content_signals_json IS
    'v2 解析后 ContentSignals 快照（标题密度、短文判定等），与 ingest_report_json 互补';
