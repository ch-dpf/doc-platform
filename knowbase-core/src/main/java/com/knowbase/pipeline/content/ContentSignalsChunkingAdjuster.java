package com.knowbase.pipeline.content;

import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Component;

/**
 * 基于 {@link ContentSignals} 在族群默认策略之上做分块策略二次路由。
 * 仅调整 {@link ChunkingStrategy}，不改 chunkSize/overlap（库级 + ingest profile 管辖）。
 */
@Component
public class ContentSignalsChunkingAdjuster {

    public void apply(ContentFamily family, ContentSignals signals, ChunkingProperties chunking) {
        if (chunking == null || signals == null || signals.isEmpty()) {
            return;
        }
        ContentFamily resolvedFamily = family != null ? family : ContentFamily.UNKNOWN;
        ChunkingStrategy current = chunking.getStrategy() != null
                ? chunking.getStrategy()
                : ChunkingStrategy.PARAGRAPH_FIRST;

        ChunkingStrategy adjusted = current;
        String reason = null;

        if (resolvedFamily == ContentFamily.TABULAR) {
            adjusted = ChunkingStrategy.PARAGRAPH_FIRST;
            if (adjusted != current) {
                reason = "tabular-family-force-paragraph-first";
            }
        } else if (signals.isCodeFences() && current == ChunkingStrategy.SEMANTIC) {
            adjusted = ChunkingStrategy.PARAGRAPH_FIRST;
            reason = "code-fences-downgrade-semantic";
        } else if (resolvedFamily == ContentFamily.DOCUMENT && signals.isShortDocument()
                && current == ChunkingStrategy.HEADING_LEVEL) {
            adjusted = ChunkingStrategy.PARAGRAPH_FIRST;
            reason = "short-document-downgrade-heading";
        } else if (shouldUpgradeToHeading(resolvedFamily, signals, current)) {
            adjusted = ChunkingStrategy.HEADING_LEVEL;
            reason = headingUpgradeReason(resolvedFamily, signals);
        }

        if (adjusted != current) {
            chunking.setStrategy(adjusted);
            signals.setAdjustedChunkingStrategy(adjusted);
            signals.setChunkingAdjustmentReason(reason);
        }
    }

    private static boolean shouldUpgradeToHeading(
            ContentFamily family, ContentSignals signals, ChunkingStrategy current) {
        if (current != ChunkingStrategy.PARAGRAPH_FIRST) {
            return false;
        }
        boolean richHeadings = signals.getHeadingLineRatio() >= ContentSignals.HEADING_RATIO_THRESHOLD
                || signals.isMarkdownHeadings();
        if (!richHeadings) {
            return false;
        }
        if (signals.isShortDocument()) {
            return false;
        }
        return family == ContentFamily.DOCUMENT
                || family == ContentFamily.PLAIN;
    }

    private static String headingUpgradeReason(ContentFamily family, ContentSignals signals) {
        if (family == ContentFamily.DOCUMENT) {
            return "document-heading-density-upgrade";
        }
        if (signals.isMarkdownHeadings()) {
            return "markdown-headings-upgrade";
        }
        return "plain-heading-density-upgrade";
    }
}
