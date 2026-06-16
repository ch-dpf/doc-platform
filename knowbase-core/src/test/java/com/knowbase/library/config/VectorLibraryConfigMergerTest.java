package com.knowbase.library.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VectorLibraryConfigMergerTest {

    @Test
    void mergeBasicUpdatesTags() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setTags(List.of("old"));

        VectorLibraryConfigMerger.mergeBasic(target, List.of("ops"));

        assertEquals(List.of("ops"), target.getTags());
    }

    @Test
    void lockPipelineStillAllowsMetadataMerge() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setChunkSize(500);

        VectorLibraryConfig incoming = new VectorLibraryConfig();
        incoming.setTags(List.of("ops"));
        incoming.setChunkSize(800);

        VectorLibraryConfigMerger.mergeSafeFields(target, incoming, true);

        assertEquals(List.of("ops"), target.getTags());
        assertEquals(500, target.getChunkSize());
    }

    @Test
    void mergeIndexPipelineUpdatesChunkSize() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setChunkSize(400);

        VectorLibraryConfigMerger.mergeIndexPipeline(
                target, LibraryIndexPipelineDtoFromConfig.pipelineWithChunkSize(900));

        assertEquals(900, target.getChunkSize());
        assertNotEquals(400, target.getChunkSize());
    }

    private static final class LibraryIndexPipelineDtoFromConfig {
        private LibraryIndexPipelineDtoFromConfig() {
        }

        static com.knowbase.library.dto.config.LibraryIndexPipelineDto pipelineWithChunkSize(int size) {
            return new com.knowbase.library.dto.config.LibraryIndexPipelineDto(size, 0, null, 0, true, "", "auto");
        }
    }
}
