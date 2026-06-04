package com.docplatform.vector.storage;

import com.docplatform.ingest.storage.ObjectStorageService;
import org.springframework.stereotype.Component;

@Component
public class ParsedTextObjectStore {

    private final ObjectStorageService storageService;

    public ParsedTextObjectStore(ObjectStorageService storageService) {
        this.storageService = storageService;
    }

    public String readAsString(String objectKey) {
        return storageService.readAsString(objectKey);
    }
}
