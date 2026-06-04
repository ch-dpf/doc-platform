package com.docplatform.ingest.service;

import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.ingest.dto.ParsePreviewResponse;
import com.docplatform.ingest.support.MimeTypeAllowlist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ParsePreviewService {

    private static final int MAX_PREVIEW_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PREVIEW_CHARS = 50_000;

    private final DocumentParseService parseService;
    private final List<String> allowedMimeTypes;

    public ParsePreviewService(DocumentParseService parseService, IngestProperties ingestProperties) {
        this.parseService = parseService;
        this.allowedMimeTypes = ingestProperties.getAllowedMimeTypes();
    }

    public ParsePreviewResponse preview(MultipartFile file) throws IOException {
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
        String text;
        try (InputStream in = file.getInputStream()) {
            text = parseService.extractText(in, fileName);
        }
        boolean truncated = false;
        if (text.length() > MAX_PREVIEW_CHARS) {
            text = text.substring(0, MAX_PREVIEW_CHARS);
            truncated = true;
        }
        return new ParsePreviewResponse(fileName, mimeType, text.length(), truncated, text);
    }
}