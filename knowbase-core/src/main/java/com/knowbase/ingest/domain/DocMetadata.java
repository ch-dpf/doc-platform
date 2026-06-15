package com.knowbase.ingest.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.platform.mybatis.PostgresJsonbTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.Instant;
import java.util.UUID;

@TableName(value = "doc_metadata", autoResultMap = true)
public class DocMetadata {

    @TableId(value = "doc_id", type = IdType.INPUT)
    private UUID docId;

    @TableField("library_id")
    private UUID libraryId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("source_type")
    private SourceType sourceType;

    @TableField("file_name")
    private String fileName;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size_bytes")
    private long sizeBytes;

    @TableField("storage_key")
    private String storageKey;

    @TableField("source_url")
    private String sourceUrl;

    @TableField("checksum_sha256")
    private String checksumSha256;

    @TableField("parse_status")
    private ParseStatus parseStatus;

    @TableField("parsed_text_key")
    private String parsedTextKey;

    @TableField("version")
    private int version;

    @TableField("index_requested")
    private boolean indexRequested;

    @TableField("index_status")
    private IndexStatus indexStatus;

    @TableField("deleted")
    private boolean deleted;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableField(value = "custom_metadata", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String customMetadataJson;

    /** v2: 采集级管道覆盖（解析/清洗/分块），与 custom_metadata 语义标签分离 */
    @TableField(value = "ingest_profile_json", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String ingestProfileJson;

    /** v2: 入库质量报告（块数、过滤数、表头占比警告等） */
    @TableField(value = "ingest_report_json", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String ingestReportJson;

    /** v2: 解析后内容结构探测快照（ContentSignals） */
    @TableField(value = "content_signals_json", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String contentSignalsJson;

    /** 入库时固化的分块配置档 ID */
    @TableField("chunk_profile_id")
    private String chunkProfileId;

    public UUID getDocId() {
        return docId;
    }

    public void setDocId(UUID docId) {
        this.docId = docId;
    }

    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public ParseStatus getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(ParseStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParsedTextKey() {
        return parsedTextKey;
    }

    public void setParsedTextKey(String parsedTextKey) {
        this.parsedTextKey = parsedTextKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isIndexRequested() {
        return indexRequested;
    }

    public void setIndexRequested(boolean indexRequested) {
        this.indexRequested = indexRequested;
    }

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(IndexStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCustomMetadataJson() {
        return customMetadataJson;
    }

    public void setCustomMetadataJson(String customMetadataJson) {
        this.customMetadataJson = customMetadataJson;
    }

    public String getIngestProfileJson() {
        return ingestProfileJson;
    }

    public void setIngestProfileJson(String ingestProfileJson) {
        this.ingestProfileJson = ingestProfileJson;
    }

    public String getIngestReportJson() {
        return ingestReportJson;
    }

    public void setIngestReportJson(String ingestReportJson) {
        this.ingestReportJson = ingestReportJson;
    }

    public String getContentSignalsJson() {
        return contentSignalsJson;
    }

    public void setContentSignalsJson(String contentSignalsJson) {
        this.contentSignalsJson = contentSignalsJson;
    }

    public String getChunkProfileId() {
        return chunkProfileId;
    }

    public void setChunkProfileId(String chunkProfileId) {
        this.chunkProfileId = chunkProfileId;
    }
}
