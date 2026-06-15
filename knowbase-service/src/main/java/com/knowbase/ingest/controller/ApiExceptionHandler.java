package com.knowbase.ingest.controller;



import com.knowbase.chat.service.ConversationNotFoundException;
import com.knowbase.ingest.service.DocumentNotFoundException;

import com.knowbase.ingest.service.InvalidDocumentException;

import com.knowbase.library.service.LibraryCapacityExceededException;
import com.knowbase.library.service.LibraryNotFoundException;
import com.knowbase.library.service.PipelineConfigLockedException;
import com.knowbase.library.service.UnsupportedEmbeddingProviderException;


import com.knowbase.vector.client.ChatException;

import com.knowbase.vector.client.EmbeddingException;
import com.knowbase.vector.retrieval.InvalidMetadataFilterException;

import org.springframework.http.HttpStatus;

import org.springframework.http.ProblemDetail;

import org.springframework.web.bind.MethodArgumentNotValidException;

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



    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail conversationNotFound(ConversationNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("会话不存在");
        return detail;
    }

    @ExceptionHandler(LibraryNotFoundException.class)

    public ProblemDetail libraryNotFound(LibraryNotFoundException ex) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        detail.setTitle("知识库不存在");

        return detail;

    }

    @ExceptionHandler(LibraryCapacityExceededException.class)
    public ProblemDetail libraryCapacityExceeded(LibraryCapacityExceededException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("知识库容量超限");
        detail.setProperty("errorCode", ex.getErrorCode());
        return detail;
    }

    @ExceptionHandler(PipelineConfigLockedException.class)
    public ProblemDetail pipelineConfigLocked(PipelineConfigLockedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("索引管道已锁定");
        return detail;
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validationFailed(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数校验失败");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("请求参数无效");
        return problem;
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

    @ExceptionHandler(InvalidMetadataFilterException.class)
    public ProblemDetail invalidMetadataFilter(InvalidMetadataFilterException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("metadata 过滤无效");
        return detail;
    }

    @ExceptionHandler(NullPointerException.class)
    public ProblemDetail nullPointer(NullPointerException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "检索结果映射异常，请检查分块是否已完成向量化");
        detail.setTitle("服务内部错误");
        if (ex.getMessage() != null) {
            detail.setProperty("cause", ex.getMessage());
        }
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail illegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("请求参数无效");
        return detail;
    }

}


