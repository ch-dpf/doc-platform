package com.knowbase.ingest.service;

import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.dto.ParsePreviewResponse;
import com.knowbase.ingest.parse.DocumentParseOptions;
import com.knowbase.ingest.support.MimeTypeAllowlist;
import com.knowbase.library.service.LibraryConfigResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ParsePreviewService {

    private static final int MAX_PREVIEW_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PREVIEW_CHARS = 50_000;

    private final DocumentParseService parseService;
    private final List<String> allowedMimeTypes;
    private final LibraryConfigResolver libraryConfigResolver;

    public ParsePreviewService(
            DocumentParseService parseService,
            IngestProperties ingestProperties,
            LibraryConfigResolver libraryConfigResolver) {
        this.parseService = parseService;
        this.allowedMimeTypes = ingestProperties.getAllowedMimeTypes();
        this.libraryConfigResolver = libraryConfigResolver;
    }

    public ParsePreviewResponse preview(MultipartFile file) throws IOException {
        return preview(file, null);
    }

    public ParsePreviewResponse preview(MultipartFile file, UUID libraryId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择文件");
        }
        if (file.getSize() > MAX_PREVIEW_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "预览文件不能超过 " + (MAX_PREVIEW_BYTES / 1024 / 1024) + "MB");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        byte[] bytes = file.getBytes();
        String mimeType = parseService.detectMimeType(bytes, fileName);
        if (!MimeTypeAllowlist.isAllowed(mimeType, fileName, allowedMimeTypes)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "不支持的文件类型: " + mimeType + "（仅文档类型可预览解析）");
        }
        DocumentParseOptions options = libraryId != null
                ? libraryConfigResolver.parseOptionsFor(libraryId)
                : DocumentParseOptions.disabled();
        String text = parseService.extractText(bytes, fileName, mimeType, options);
        boolean truncated = false;
        if (text.length() > MAX_PREVIEW_CHARS) {
            text = text.substring(0, MAX_PREVIEW_CHARS);
            truncated = true;
        }
        return new ParsePreviewResponse(fileName, mimeType, text.length(), truncated, text);
    }
}
