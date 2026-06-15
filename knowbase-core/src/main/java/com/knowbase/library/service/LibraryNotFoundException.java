package com.knowbase.library.service;

import java.util.UUID;

public class LibraryNotFoundException extends RuntimeException {

    public LibraryNotFoundException(UUID libraryId) {
        super("知识库不存在: " + libraryId);
    }
}
