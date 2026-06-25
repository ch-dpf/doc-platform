package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadingOrderHttpClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reading-order", exchange -> {
            byte[] response = """
                    {
                      "orders": [
                        {"index": 1, "readingOrder": 0},
                        {"index": 0, "readingOrder": 1}
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
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/reading-order";
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void appliesRemoteReadingOrder() {
        List<StructuralBlock> blocks = List.of(
                block("Second", 1, 200),
                block("First", 0, 100)
        );
        ReadingOrderHttpClient client = new ReadingOrderHttpClient();
        List<StructuralBlock> ordered = client.order(baseUrl, blocks);
        assertEquals("First", ordered.get(0).content());
        assertEquals("remote-http", ordered.get(0).metadata().get("readingOrderSource"));
    }

    private static StructuralBlock block(String content, int index, double top) {
        return new StructuralBlock(
                "paragraph",
                0,
                content,
                index,
                Map.of("pageNumber", 1, "bbox", List.of(72d, top, 200d, 20d))
        );
    }
}
