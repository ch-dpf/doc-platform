-- 索引失败后若被错误标记为已处理，可执行本脚本并重新触发索引（或重新上传文档）
SET search_path TO vector_idx;

DELETE FROM processed_event;

-- 可选：清空失败任务后由补偿接口 /api/v1/index/rebuild 重试
-- UPDATE document_index_job SET status = 'QUEUED', error_message = NULL WHERE status = 'FAILED';
