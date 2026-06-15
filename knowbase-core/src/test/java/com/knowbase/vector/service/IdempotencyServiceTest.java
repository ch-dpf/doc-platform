package com.knowbase.vector.service;

import com.knowbase.event.DocumentEventType;
import com.knowbase.event.IdempotencyKeys;
import com.knowbase.vector.domain.ProcessedEvent;
import com.knowbase.vector.mapper.ProcessedEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedEventMapper mapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void marksProcessedOncePerKey() {
        UUID docId = UUID.randomUUID();
        // isProcessed → markProcessed(内含 isProcessed) → isProcessed → markProcessed(内含 isProcessed)
        when(mapper.selectCount(any())).thenReturn(0L, 0L, 1L, 1L);

        assertFalse(idempotencyService.isProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX));
        idempotencyService.markProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX);
        assertTrue(idempotencyService.isProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX));

        idempotencyService.markProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX);
        verify(mapper, times(1)).insert(any(ProcessedEvent.class));
    }

    @Test
    void insertUsesExpectedKey() {
        UUID docId = UUID.randomUUID();
        when(mapper.selectCount(any())).thenReturn(0L);

        idempotencyService.markProcessed(docId, 2, DocumentEventType.DOCUMENT_DELETED);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(mapper).insert(captor.capture());
        assertEquals(
                IdempotencyKeys.forEvent(docId, 2, DocumentEventType.DOCUMENT_DELETED),
                captor.getValue().getIdempotencyKey());
    }

    @Test
    void skipInsertWhenAlreadyProcessed() {
        UUID docId = UUID.randomUUID();
        when(mapper.selectCount(any())).thenReturn(1L);

        assertTrue(idempotencyService.isProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX));
        idempotencyService.markProcessed(docId, 1, DocumentEventType.DOCUMENT_READY_FOR_INDEX);
        verify(mapper, never()).insert(any(ProcessedEvent.class));
    }
}
