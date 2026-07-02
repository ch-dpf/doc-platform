package com.knowbase.model.vision.paddleocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaddleOcrVlVisionDocumentModelClientTest {

    private HttpServer server;
    private String baseUrl;
    private String lastRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/layout-parsing", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = """
                    {
                      "errorCode": 0,
                      "errorMsg": "Success",
                      "result": {
                        "layoutParsingResults": [
                          {
                            "markdown": {
                              "text": "# Title\\n\\nBody line"
                            }
                          }
                        ]
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsBase64ImageAndReturnsMarkdown() {
        PaddleOcrVlVisionDocumentModelClient client = new PaddleOcrVlVisionDocumentModelClient(
                baseUrl,
                "/layout-parsing",
                Duration.ofSeconds(5),
                "PaddleOCR-VL-1.6",
                true,
                false,
                false
        );

        String markdown = client.recognizePage(new byte[] {1, 2, 3}, "image/png", 1, Map.of());

        assertEquals("# Title\n\nBody line", markdown);
        assertTrue(lastRequestBody.contains("\"fileType\":1"));
        assertTrue(lastRequestBody.contains("\"file\":\"AQID\""));
        assertEquals("PaddleOCR-VL-1.6", client.modelName());
    }
}
