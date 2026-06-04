package com.docplatform.ingest.service;

import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.domain.SourceType;
import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.support.DocMetadataStore;
import com.docplatform.ingest.support.MimeTypeAllowlist;
import com.docplatform.ingest.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadService {

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentParseService parseService;
    private final DocumentPipelineService pipelineService;
    private final IngestProperties ingestProperties;

    public UploadService(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentParseService parseService,
            DocumentPipelineService pipelineService,
            IngestProperties ingestProperties) {
        this.repository = repository;
        this.storageService = storageService;
        this.parseService = parseService;
        this.pipelineService = pipelineService;
        this.ingestProperties = ingestProperties;
    }

    /**
     * 上传文档：按内容 checksum 去重，同内容复用 docId 并递增版本，否则新建记录。
     */
    @Transactional
    public DocumentResponse upload(String tenantId, MultipartFile file, boolean autoIndex) throws IOException {
        // 1. 读取文件并计算 SHA-256，用于同租户内容去重
        byte[] bytes = file.getBytes();
        String checksum = sha256(bytes);
        // 2. 检测 MIME 类型并校验是否在允许列表中
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String mimeType = parseService.detectMimeType(bytes, fileName);
        validateMimeType(mimeType, fileName);

        // 3. 同租户下已存在相同内容且未删除的文档 → 复用 docId，升版本后重新处理
        Optional<DocMetadata> existing = repository.findByTenantIdAndChecksumSha256AndDeletedFalse(tenantId, checksum);
        if (existing.isPresent()) {
            DocMetadata doc = existing.get();
            doc.setVersion(doc.getVersion() + 1);
            doc.setParseStatus(ParseStatus.PENDING);
            doc.setIndexRequested(autoIndex);
            doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
            doc.setStorageKey(buildStorageKey(doc, fileName));
            repository.save(doc);
            storeAndProcess(doc, bytes, fileName, mimeType);
            return DocumentResponse.from(doc);
        }

        // 4. 新内容：创建文档元数据（version=1）
        UUID docId = UUID.randomUUID();
        DocMetadata doc = new DocMetadata();
        doc.setDocId(docId);
        doc.setTenantId(tenantId);
        doc.setSourceType(SourceType.UPLOAD);
        doc.setFileName(fileName);
        doc.setMimeType(mimeType);
        doc.setSizeBytes(bytes.length);
        doc.setChecksumSha256(checksum);
        doc.setParseStatus(ParseStatus.PENDING);
        doc.setVersion(1);
        doc.setIndexRequested(autoIndex);
        doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
        doc.setDeleted(false);
        doc.setStorageKey(buildStorageKey(doc, doc.getFileName()));

        repository.save(doc);
        // 5. 写入对象存储并触发异步解析/索引流水线
        storeAndProcess(doc, bytes, doc.getFileName(), mimeType);
        return DocumentResponse.from(doc);
    }

    /** 按版本路径存储原始文件，并异步触发后续处理。 */
    private void storeAndProcess(DocMetadata doc, byte[] bytes, String fileName, String mimeType) {
        String storageKey = buildStorageKey(doc, fileName);
        doc.setStorageKey(storageKey);
        storageService.putObject(storageKey, new ByteArrayInputStream(bytes), bytes.length, mimeType);
        repository.save(doc);
        pipelineService.scheduleProcessAfterCommit(doc.getDocId(), doc.getVersion(), bytes, fileName);
    }

    private static String buildStorageKey(DocMetadata doc, String fileName) {
        return doc.getTenantId() + "/" + doc.getDocId() + "/v" + doc.getVersion() + "/raw/" + fileName;
    }

    private void validateMimeType(String mimeType, String fileName) {
        if (!MimeTypeAllowlist.isAllowed(mimeType, fileName, ingestProperties.getAllowedMimeTypes())) {
            throw new InvalidDocumentException("MIME type not allowed: " + mimeType);
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
