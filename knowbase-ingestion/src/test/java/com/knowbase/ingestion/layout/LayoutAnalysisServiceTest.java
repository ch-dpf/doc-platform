package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutAnalysisServiceTest {

    @Test
    void fallsBackToNextProviderWhenEarlierProviderReturnsEmptyBlocks() {
        LayoutAnalysisProvider emptyProvider = new StubProvider("empty-provider", List.of());
        LayoutAnalysisProvider populatedProvider = new StubProvider(
                "populated-provider",
                List.of(new StructuralBlock("paragraph", 0, "hello", 0, Map.of()))
        );
        LayoutAnalysisService service = new LayoutAnalysisService(
                List.of(emptyProvider, populatedProvider),
                true
        );

        LayoutPageResult result = service.analyzePage(new LayoutPageRequest(
                new byte[] {1, 2, 3},
                "image/png",
                1,
                100d,
                200d,
                "file:///demo.pdf",
                Map.of()
        ));

        assertEquals("populated-provider", result.providerCode());
        assertEquals(1, result.blocks().size());
    }

    @Test
    void prefersRequestedProviderBeforeFallback() {
        LayoutAnalysisProvider requested = new StubProvider(OcrRasterLayoutProvider.PROVIDER_CODE, List.of());
        LayoutAnalysisProvider fallback = new StubProvider(
                "vision-markdown",
                List.of(new StructuralBlock("paragraph", 0, "from-fallback", 0, Map.of()))
        );
        LayoutAnalysisService service = new LayoutAnalysisService(List.of(requested, fallback), true);

        LayoutPageResult result = service.analyzePage(new LayoutPageRequest(
                new byte[] {1, 2, 3},
                "image/png",
                1,
                100d,
                200d,
                "file:///demo.pdf",
                Map.of("layoutProvider", OcrRasterLayoutProvider.PROVIDER_CODE)
        ));

        assertEquals("vision-markdown", result.providerCode());
        assertEquals(1, result.blocks().size());
    }

    private static final class StubProvider implements LayoutAnalysisProvider {

        private final String code;
        private final List<StructuralBlock> blocks;

        private StubProvider(String code, List<StructuralBlock> blocks) {
            this.code = code;
            this.blocks = blocks;
        }

        @Override
        public String providerCode() {
            return code;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public boolean supports(LayoutPageRequest request) {
            return true;
        }

        @Override
        public LayoutPageResult analyze(LayoutPageRequest request) {
            return new LayoutPageResult(
                    code,
                    code,
                    request.pageNumber(),
                    blocks,
                    List.of(),
                    null,
                    null,
                    Map.of()
            );
        }
    }
}
