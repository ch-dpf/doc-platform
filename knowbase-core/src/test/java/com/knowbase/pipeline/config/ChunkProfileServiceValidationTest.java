package com.knowbase.pipeline.config;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkProfileServiceValidationTest {

    @Mock
    private EffectiveConfigResolver effectiveConfigResolver;

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    private ChunkProfileService service;

    @BeforeEach
    void setUp() {
        service = new ChunkProfileService(
                effectiveConfigResolver,
                libraryConfigResolver,
                null,
                null,
                null);
    }

    @Test
    void allowsUploadWithAutoStrategyWhenStoredPrimaryIsStale() {
        UUID libraryId = UUID.randomUUID();
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setChunkingStrategy(ChunkingStrategy.AUTO);
        cfg.setChunkSize(500);
        cfg.setChunkOverlap(120);
        cfg.setPrimaryChunkProfileId("cp_stale_auto_primary");

        when(libraryConfigResolver.config(libraryId)).thenReturn(cfg);
        when(effectiveConfigResolver.forIngest(eq(libraryId), eq("application/pdf"), isNull()))
                .thenReturn(effectiveWithStrategy(ChunkingStrategy.PARAGRAPH_FIRST));

        assertDoesNotThrow(() -> service.validateNewProfileAllowed(libraryId, "application/pdf", null));
    }

    private static EffectivePipelineConfig effectiveWithStrategy(ChunkingStrategy strategy) {
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(strategy);
        chunking.setChunkSize(500);
        chunking.setOverlap(120);
        chunking.setMinParagraphLength(30);
        return new EffectivePipelineConfig(
                true, null, null, chunking, null, 1);
    }
}
