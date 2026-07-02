package com.knowbase.ingestion.layout;

/**
 * Shared option keys for {@link LayoutAnalysisService} and layout providers.
 */
public final class LayoutAnalysisOptions {

    /** Internal: raw PDF bytes for {@link LocalPdfTextLayoutProvider}. */
    public static final String PDF_BYTES = "__pdfBytes";

    public static final String DEFAULT_LOCAL_PROVIDER = "local-pdf-layout";

    private LayoutAnalysisOptions() {
    }
}
