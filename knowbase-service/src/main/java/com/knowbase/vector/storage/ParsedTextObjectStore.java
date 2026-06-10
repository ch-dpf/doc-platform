package com.knowbase.vector.storage;

import com.knowbase.ingest.storage.ObjectStorageService;
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
