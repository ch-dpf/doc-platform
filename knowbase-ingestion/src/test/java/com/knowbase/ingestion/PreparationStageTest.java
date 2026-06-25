package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparationStageTest {

    @Test
    void fromAcceptsPostProcessAliases() {
        assertEquals(PreparationStage.POST_PROCESS, PreparationStage.from("post_process"));
        assertEquals(PreparationStage.POST_PROCESS, PreparationStage.from("post-process"));
        assertEquals(PreparationStage.POST_PROCESS, PreparationStage.from("postprocess"));
    }

    @Test
    void fromAcceptsDocumentSummaryAliases() {
        assertEquals(PreparationStage.DOCUMENT_SUMMARY, PreparationStage.from("document_summary"));
        assertEquals(PreparationStage.DOCUMENT_SUMMARY, PreparationStage.from("summarize"));
    }

    @Test
    void documentSummaryRunsOnlyForDedicatedPrepareStage() {
        assertTrue(PreparationStage.DOCUMENT_SUMMARY.runsDocumentSummary());
        assertFalse(PreparationStage.CHUNK.runsDocumentSummary());
        assertFalse(PreparationStage.NORMALIZE.runsDocumentSummary());
        assertEquals(PreparationStage.DOCUMENT_SUMMARY, PreparationStage.DOCUMENT_SUMMARY.executionStage());
    }

    @Test
    void allExecutesThroughChunkWithoutPostProcess() {
        assertEquals(PreparationStage.CHUNK, PreparationStage.ALL.executionStage());
        assertFalse(PreparationStage.ALL.runsPostProcess());
        assertFalse(PreparationStage.CHUNK.runsPostProcess());
    }
}
