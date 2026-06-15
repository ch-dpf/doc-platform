package com.knowbase.vector.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalRetrievalEvalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void evalGoldSetParsesAsExpected() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/rag/temporal-retrieval-eval.json")) {
            JsonNode cases = MAPPER.readTree(in);
            for (JsonNode node : cases) {
                String question = node.get("question").asText();
                TemporalQueryScope scope = RagTemporalQueryParser.parse(question, null);
                boolean expectScoped = node.path("expectScoped").asBoolean(false);
                if (!expectScoped) {
                    assertFalse(scope.scoped(), question);
                    continue;
                }
                assertTrue(scope.scoped(), question);
                if (node.has("expectYear")) {
                    assertEquals(node.get("expectYear").asInt(), scope.year(), question);
                }
                if (node.has("expectMonth")) {
                    assertEquals(node.get("expectMonth").asInt(), scope.month(), question);
                }
                if (node.path("expectYearScoped").asBoolean(false)) {
                    assertTrue(scope.yearScoped(), question);
                }
                if (node.has("expectMonthEnd")) {
                    assertEquals(node.get("expectMonthEnd").asInt(), scope.monthEnd(), question);
                }
                if (node.has("expectWeekOfMonth")) {
                    assertEquals(node.get("expectWeekOfMonth").asInt(), scope.weekOfMonth(), question);
                }
                if (node.has("expectDayOfMonth")) {
                    assertEquals(node.get("expectDayOfMonth").asInt(), scope.dayOfMonth(), question);
                }
                if (node.has("expectPerson")) {
                    assertEquals(node.get("expectPerson").asText(), scope.person(), question);
                }
                if (node.has("expectPersons")) {
                    for (JsonNode person : node.get("expectPersons")) {
                        assertTrue(scope.persons().contains(person.asText()), question);
                    }
                }
                if (node.path("expectCompletedWork").asBoolean(false)) {
                    assertTrue(scope.completedWorkOnly(), question);
                }
            }
        }
    }
}
