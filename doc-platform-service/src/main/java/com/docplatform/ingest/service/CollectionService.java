package com.docplatform.ingest.service;

import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.domain.SourceType;
import com.docplatform.ingest.dto.CollectRequest;
import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.support.DocMetadataStore;
import com.docplatform.ingest.support.SourceUrlNormalizer;
import com.docplatform.ingest.storage.ObjectStorageService;
import com.docplatform.library.service.LibraryConfigResolver;
import com.docplatform.library.service.VectorLibraryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class CollectionService {

    private final DocMetadataStore repository;
    private final ObjectStorageService storageService;
    private final DocumentPipelineService pipelineService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final VectorLibraryService vectorLibraryService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CollectionService(
            DocMetadataStore repository,
            ObjectStorageService storageService,
            DocumentPipelineService pipelineService,
            LibraryConfigResolver libraryConfigResolver,
            VectorLibraryService vectorLibraryService) {
        this.repository = repository;
        this.storageService = storageService;
        this.pipelineService = pipelineService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.vectorLibraryService = vectorLibraryService;
    }

    /**
     * URL 采集：按「租户 + 来源 URL」识别文档；同一 URL 再次采集则升版本并刷新内容。
     * 不同 URL 即使页面内容相同（如不同端口的 doc.html）也视为不同文档。
     */
    @Transactional
    public DocumentResponse collect(CollectRequest request) {
        libraryConfigResolver.requireCollectAllowed(request.libraryId());
        try {
            String sourceUrl = SourceUrlNormalizer.normalize(request.url());
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(sourceUrl)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw InvalidDocumentException.of(
                        InvalidDocumentException.CODE_FETCH_FAILED,
                        "采集失败：URL 返回 HTTP " + response.statusCode());
            }
            byte[] bytes = response.body();
            String fileName = extractFileName(sourceUrl);
            String mimeType = response.headers().firstValue("Content-Type").orElse("text/plain");
            if (mimeType.contains(";")) {
                mimeType = mimeType.substring(0, mimeType.indexOf(';')).trim();
            }

            UUID libraryId = request.libraryId();
            libraryConfigResolver.requireLibrary(libraryId);
            String tenantId = request.tenantId();
            String checksum = sha256(bytes);
            boolean autoIndex = request.autoIndex();

            Optional<DocMetadata> existing =
                    repository.findByLibraryTenantSourceUrl(libraryId, tenantId, sourceUrl);
            if (existing.isPresent()) {
                DocMetadata doc = existing.get();
                doc.setVersion(doc.getVersion() + 1);
                doc.setFileName(fileName);
                doc.setMimeType(mimeType);
                doc.setSizeBytes(bytes.length);
                doc.setChecksumSha256(checksum);
                doc.setParseStatus(ParseStatus.PENDING);
                doc.setIndexRequested(autoIndex);
                doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
                doc.setStorageKey(buildStorageKey(doc, fileName));
                repository.save(doc);
                storeAndProcess(doc, bytes, fileName, mimeType);
                return DocumentResponse.from(doc);
            }

            UUID docId = UUID.randomUUID();
            DocMetadata doc = new DocMetadata();
            doc.setDocId(docId);
            doc.setLibraryId(libraryId);
            doc.setTenantId(tenantId);
            doc.setSourceType(SourceType.CRAWL);
            doc.setSourceUrl(sourceUrl);
            doc.setFileName(fileName);
            doc.setMimeType(mimeType);
            doc.setSizeBytes(bytes.length);
            doc.setChecksumSha256(checksum);
            doc.setParseStatus(ParseStatus.PENDING);
            doc.setVersion(1);
            doc.setIndexRequested(autoIndex);
            doc.setIndexStatus(autoIndex ? IndexStatus.PENDING : null);
            doc.setDeleted(false);
            doc.setStorageKey(buildStorageKey(doc, fileName));

            repository.save(doc);
            vectorLibraryService.incrementDocumentCount(libraryId, 1);
            storeAndProcess(doc, bytes, fileName, mimeType);
            return DocumentResponse.from(doc);
        } catch (InvalidDocumentException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw InvalidDocumentException.of(InvalidDocumentException.CODE_COLLECTION_FAILED, e.getMessage());
        } catch (Exception e) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_COLLECTION_FAILED, "采集失败：" + e.getMessage());
        }
    }

    private void storeAndProcess(DocMetadata doc, byte[] bytes, String fileName, String mimeType) {
        String storageKey = buildStorageKey(doc, fileName);
        doc.setStorageKey(storageKey);
        storageService.putObject(storageKey, new ByteArrayInputStream(bytes), bytes.length, mimeType);
        repository.save(doc);
        pipelineService.scheduleProcessAfterCommit(doc.getDocId(), doc.getVersion(), bytes, fileName);
    }

    private static String buildStorageKey(DocMetadata doc, String fileName) {
        return doc.getTenantId()
                + "/"
                + doc.getLibraryId()
                + "/"
                + doc.getDocId()
                + "/v"
                + doc.getVersion()
                + "/raw/"
                + fileName;
    }

    private static String extractFileName(String url) {
        String path = URI.create(url).getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "collected.txt";
        }
        int idx = path.lastIndexOf('/');
        return path.substring(idx + 1);
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
