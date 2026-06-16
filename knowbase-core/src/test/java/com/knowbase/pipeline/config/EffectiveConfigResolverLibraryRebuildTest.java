package com.knowbase.pipeline.config;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.pipeline.content.ContentSignalsChunkingAdjuster;
import com.knowbase.pipeline.content.DefaultContentSignalsDetector;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveConfigResolverLibraryRebuildTest {

    private static final UUID LIBRARY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID DOC_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @Mock
    private DocMetadataStore docMetadataStore;

    private EffectiveConfigResolver resolver;

    @BeforeEach
    void setUp() {
        OcrProperties ocrProperties = new OcrProperties();
        resolver = new EffectiveConfigResolver(
                libraryConfigResolver,
                docMetadataStore,
                new MimeTypePipelineDefaults(new ContentFamilyPipelineDefaults(ocrProperties)),
                ocrProperties,
                new TextNormalizationProperties(),
                new DefaultContentSignalsDetector(),
                new ContentSignalsChunkingAdjuster(),
                new ParserEngineRegistry());
    }

    @Test
    void libraryRebuildIgnoresIngestChunkOverrides() {
        VectorLibraryConfig library = new VectorLibraryConfig();
        library.setConfigVersion(1);
        library.setChunkSize(500);
        library.setChunkOverlap(50);
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setChunkSize(500);
        chunking.setOverlap(50);
        when(libraryConfigResolver.config(LIBRARY_ID)).thenReturn(library);
        when(libraryConfigResolver.chunkingFor(LIBRARY_ID)).thenReturn(chunking);

        IngestProfile profile = new IngestProfile();
        profile.setChunkSize(1200);
        profile.setChunkOverlap(120);
        DocMetadata doc = new DocMetadata();
        doc.setDocId(DOC_ID);
        doc.setMimeType("text/plain");
        doc.setIngestProfileJson(JsonSupport.toJson(profile));
        when(docMetadataStore.findById(DOC_ID)).thenReturn(Optional.of(doc));

        String text = "段落一。\n\n段落二。";
        EffectivePipelineConfig withOverlay =
                resolver.forDocumentWithContent(LIBRARY_ID, DOC_ID, text);
        EffectivePipelineConfig libraryRebuild =
                resolver.forDocumentLibraryRebuild(LIBRARY_ID, DOC_ID, text);

        assertEquals(1200, withOverlay.chunking().getChunkSize());
        assertEquals(500, libraryRebuild.chunking().getChunkSize());
        assertNotEquals(withOverlay.chunking().getChunkSize(), libraryRebuild.chunking().getChunkSize());
    }
}
