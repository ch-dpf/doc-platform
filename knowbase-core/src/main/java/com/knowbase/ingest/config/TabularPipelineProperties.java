package com.knowbase.ingest.config;

import com.knowbase.ingest.parse.TabularRowFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 表格类文档（Excel/CSV）文本处理配置。
 * <p>
 * 行键值化不在分块阶段做：会膨胀行文本、破坏段落合并，且与周报表头过滤启发式不兼容。
 * 默认保持 Tika 输出的 Tab 分隔格式；列语义消歧在 RAG 检索层处理。
 */
@ConfigurationProperties(prefix = "ingest.tabular")
public class TabularPipelineProperties {

    /**
     * 保留配置项供后续扩展（如入库向量化阶段）；当前分块管线不读取此开关。
     */
    private TabularRowFormat rowFormat = TabularRowFormat.TSV_LEGACY;

    public TabularRowFormat getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(TabularRowFormat rowFormat) {
        this.rowFormat = rowFormat != null ? rowFormat : TabularRowFormat.HEADER_PREFIXED;
    }

    public boolean isHeaderPrefixed() {
        return rowFormat == TabularRowFormat.HEADER_PREFIXED;
    }
}
