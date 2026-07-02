package com.knowbase.application.service;

public record DocumentPreviewContent(String filename, String mimeType, byte[] content) {
}
