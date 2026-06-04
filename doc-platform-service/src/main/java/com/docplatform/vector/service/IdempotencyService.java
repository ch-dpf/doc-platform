package com.docplatform.vector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docplatform.event.DocumentEventType;
import com.docplatform.event.IdempotencyKeys;
import com.docplatform.vector.domain.ProcessedEvent;
import com.docplatform.vector.mapper.ProcessedEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final ProcessedEventMapper mapper;

    public IdempotencyService(ProcessedEventMapper mapper) {
        this.mapper = mapper;
    }

    public boolean isProcessed(UUID docId, int version, DocumentEventType eventType) {
        String key = IdempotencyKeys.forEvent(docId, version, eventType);
        return mapper.selectCount(new LambdaQueryWrapper<ProcessedEvent>()
                        .eq(ProcessedEvent::getIdempotencyKey, key))
                > 0;
    }

    /** 仅在事件处理成功后调用，避免失败重试被误判为重复而跳过。 */
    @Transactional
    public void markProcessed(UUID docId, int version, DocumentEventType eventType) {
        if (isProcessed(docId, version, eventType)) {
            return;
        }
        ProcessedEvent event = new ProcessedEvent();
        event.setIdempotencyKey(IdempotencyKeys.forEvent(docId, version, eventType));
        event.setProcessedAt(Instant.now());
        mapper.insert(event);
    }
}
