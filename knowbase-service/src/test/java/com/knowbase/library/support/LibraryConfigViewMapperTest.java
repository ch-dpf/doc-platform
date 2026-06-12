package com.knowbase.library.support;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.dto.config.LibraryConfigView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryConfigViewMapperTest {

    @Test
    void viewHasNoIngestAccessSection() {
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setConfigVersion(2);
        cfg.setMetadataDbType("postgresql");

        LibraryConfigView view = LibraryConfigViewMapper.toView(cfg);

        assertEquals(2, view.configVersion());
        assertEquals("postgresql", view.metadataDbType());
        assertEquals(500, view.indexPipeline().chunkSize());
    }

    @Test
    void mapsIndexPipelineFields() {
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setChunkSize(512);
        cfg.setEmbeddingModel("nomic-embed-text");

        LibraryConfigView view = LibraryConfigViewMapper.toView(cfg);

        assertEquals(512, view.indexPipeline().chunkSize());
        assertEquals("nomic-embed-text", view.indexPipeline().embeddingModel());
    }
}
