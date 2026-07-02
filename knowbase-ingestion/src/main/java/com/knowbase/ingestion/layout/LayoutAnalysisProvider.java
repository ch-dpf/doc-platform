package com.knowbase.ingestion.layout;

public interface LayoutAnalysisProvider {

    String providerCode();

    boolean available();

    boolean supports(LayoutPageRequest request);

    LayoutPageResult analyze(LayoutPageRequest request);
}
