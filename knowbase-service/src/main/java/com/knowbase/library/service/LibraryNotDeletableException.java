package com.knowbase.library.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.CONFLICT)
public class LibraryNotDeletableException extends RuntimeException {

    public LibraryNotDeletableException(UUID libraryId, String reason) {
        super("知识库 " + libraryId + " 不可删除：" + reason);
    }
}
