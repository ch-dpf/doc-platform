package com.docplatform.ingest.controller;



import com.docplatform.ingest.service.DocumentNotFoundException;

import com.docplatform.ingest.service.InvalidDocumentException;

import com.docplatform.library.service.LibraryNotFoundException;
import com.docplatform.library.service.UnsupportedEmbeddingProviderException;


import com.docplatform.vector.client.ChatException;

import com.docplatform.vector.client.EmbeddingException;

import org.springframework.http.HttpStatus;

import org.springframework.http.ProblemDetail;

import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;



import java.util.Map;



@RestControllerAdvice

public class ApiExceptionHandler {



    @ExceptionHandler(DocumentNotFoundException.class)

    public ProblemDetail notFound(DocumentNotFoundException ex) {

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

    }



    @ExceptionHandler(InvalidDocumentException.class)

    public ProblemDetail badRequest(InvalidDocumentException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

        detail.setTitle("文档校验失败");

        detail.setProperty("errorCode", ex.getErrorCode());

        if (ex.getFileName() != null) {

            detail.setProperty("fileName", ex.getFileName());

        }

        if (ex.getDetectedMimeType() != null) {

            detail.setProperty("detectedMimeType", ex.getDetectedMimeType());

        }

        if (ex.getAllowedMimeTypes() != null && !ex.getAllowedMimeTypes().isEmpty()) {

            detail.setProperty("allowedMimeTypes", ex.getAllowedMimeTypes());

        }

        return detail;

    }



    @ExceptionHandler(UnsupportedEmbeddingProviderException.class)

    public ProblemDetail unsupportedEmbeddingProvider(UnsupportedEmbeddingProviderException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

        detail.setTitle("不支持的向量化提供方");

        return detail;

    }



    @ExceptionHandler(LibraryNotFoundException.class)

    public ProblemDetail libraryNotFound(LibraryNotFoundException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        detail.setTitle("向量库不存在");

        return detail;

    }



    @ExceptionHandler(MethodArgumentTypeMismatchException.class)

    public ProblemDetail typeMismatch(MethodArgumentTypeMismatchException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(

                HttpStatus.BAD_REQUEST, "参数格式错误: " + ex.getName());

        detail.setTitle("请求参数无效");

        if (ex.getValue() != null) {

            detail.setProperty("value", String.valueOf(ex.getValue()));

        }

        return detail;

    }



    @ExceptionHandler(ChatException.class)

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)

    public Map<String, String> handleChat(ChatException ex) {

        return Map.of("error", ex.getMessage());

    }



    @ExceptionHandler(EmbeddingException.class)

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)

    public Map<String, String> handleEmbedding(EmbeddingException ex) {

        return Map.of("error", ex.getMessage());

    }



    @ExceptionHandler(IllegalStateException.class)

    public ProblemDetail handleIllegalState(IllegalStateException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

        detail.setTitle("服务内部错误");

        if (ex.getCause() != null) {

            detail.setProperty("cause", ex.getCause().getMessage());

        }

        return detail;

    }

}


