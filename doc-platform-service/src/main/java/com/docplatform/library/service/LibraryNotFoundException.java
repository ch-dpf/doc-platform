package com.docplatform.library.service;

import java.util.UUID;

public class LibraryNotFoundException extends RuntimeException {

    public LibraryNotFoundException(UUID libraryId) {
        super("向量库不存在: " + libraryId);
    }
}
