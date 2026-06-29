package com.knowbase.ingestion.external;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.ExternalDocumentParser;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.ParsedDocument;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDocumentParserHttpContractTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retries503ThenSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        String successBody = loadMockResponse();
        startServer(exchange -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                respond(exchange, 503, "busy");
                return;
            }
            respond(exchange, 200, successBody);
        });

        ExternalDocumentParser parser = new ExternalDocumentParser(
                java.net.http.HttpClient.newHttpClient(),
                endpoint(),
                List.of()
        );
        ParsedDocument parsed = parser.parse(source(Map.of(
                "externalParserMaxAttempts", 2
        )));

        assertEquals(2, attempts.get());
        assertEquals(2, parsed.metadata().get("externalParserAttempts"));
        assertNotNull(parsed.metadata().get("externalParseMs"));
        assertFalse(Boolean.TRUE.equals(parsed.metadata().get("externalParserFallbackUsed")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Quarterly Report")));
    }

    @Test
    void sendsBearerAuthorizationHeader() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                respond(exchange, 200, loadMockResponse());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });

        ExternalDocumentParser parser = new ExternalDocumentParser(
                java.net.http.HttpClient.newHttpClient(),
                endpoint(),
                List.of()
        );
        parser.parse(source(Map.of("externalParserBearerToken", "secret-token")));

        assertEquals("Bearer secret-token", authorization.get());
    }

    @Test
    void failOnExternalErrorSkipsFallback() throws Exception {
        startServer(exchange -> respond(exchange, 503, "busy"));

        ExternalDocumentParser parser = new ExternalDocumentParser(
                java.net.http.HttpClient.newHttpClient(),
                endpoint(),
                List.of(new PdfLayoutParser())
        );

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> parser.parse(source(Map.of(
                "failOnExternalError", true,
                "externalParserMaxAttempts", 1
        ))));
        assertTrue(failure.getMessage().contains("外部解析器"));
    }

    @Test
    void mapsTableCellCoordinatesFromStructuredCells() throws Exception {
        String body = """
                {
                  "schemaVersion": "1.1",
                  "tables": [{
                    "tableRegionId": 9,
                    "pageNumber": 1,
                    "rows": [{
                      "rowRole": "DATA",
                      "cells": [
                        {"text": "East", "columnSpan": 1},
                        {"text": "1.2M", "merged": false, "bbox": [80, 540, 60, 16]}
                      ]
                    }]
                  }]
                }
                """;
        startServer(exchange -> respond(exchange, 200, body));

        ExternalDocumentParser parser = new ExternalDocumentParser(
                java.net.http.HttpClient.newHttpClient(),
                endpoint(),
                List.of()
        );
        ParsedDocument parsed = parser.parse(source(Map.of()));

        var tableRow = parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> coordinates = (List<Map<String, Object>>) tableRow.metadata().get("cellCoordinates");
        assertNotNull(coordinates);
        assertEquals(2, coordinates.size());
        assertEquals("R1C1", coordinates.get(0).get("coordinate"));
        assertEquals("East", coordinates.get(0).get("value"));
        assertEquals("engine", coordinates.get(1).get("bboxSource"));
        assertEquals("unavailable", tableRow.metadata().get("bboxSource"));
    }

    private DocumentSource source(Map<String, Object> metadata) {
        return new DocumentSource(
                "memory://contract.pdf",
                "contract.pdf",
                "application/pdf",
                new ByteArrayInputStream("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8)),
                metadata
        );
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/parse", handler);
        server.start();
    }

    private String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/parse";
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        try {
            writeResponse(exchange, status, body);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String loadMockResponse() throws Exception {
        return new String(ExternalDocumentParserHttpContractTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/external/mock-docling-response.json")
                .readAllBytes(), StandardCharsets.UTF_8);
    }
}
