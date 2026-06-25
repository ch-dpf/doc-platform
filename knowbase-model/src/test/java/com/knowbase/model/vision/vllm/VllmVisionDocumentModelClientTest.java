package com.knowbase.model.vision.vllm;

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

class VllmVisionDocumentModelClientTest {

    private HttpServer server;
    private String baseUrl;
    private String lastRequestBody;
    private String lastAuthorization;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] response = """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "## Section\\n\\nParsed text"
                          }
                        }
                      ]
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
    void postsOpenAiVisionPayload() {
        VllmVisionDocumentModelClient client = new VllmVisionDocumentModelClient(
                baseUrl,
                "/v1/chat/completions",
                Duration.ofSeconds(5),
                "PaddleOCR-VL-1.6-0.9B",
                "test-key",
                0.1d
        );

        String markdown = client.recognizePage(new byte[] {9, 8}, "image/png", 2, Map.of());

        assertEquals("## Section\n\nParsed text", markdown);
        assertTrue(lastRequestBody.contains("\"model\":\"PaddleOCR-VL-1.6-0.9B\""));
        assertTrue(lastRequestBody.contains("data:image/png;base64,"));
        assertEquals("Bearer test-key", lastAuthorization);
    }
}
