---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
---

# Testing Patterns

**Analysis Date:** 2026-06-10

## Test Framework

**Runner (backend):**
- JUnit 5 (Jupiter) via `spring-boot-starter-test` in `knowbase-service/pom.xml`
- Mockito for dependency mocking (`@ExtendWith(MockitoExtension.class)`)
- No Testcontainers, WireMock, or `@SpringBootTest` usage detected

**Assertion Library:**
- JUnit 5 `org.junit.jupiter.api.Assertions` (static imports: `assertEquals`, `assertTrue`, `assertFalse`)
- Mockito verify/verifyNever for interaction assertions

**Run Commands:**
```bash
# From repo root — runs all modules
mvn test

# Single module
mvn test -pl knowbase-service

# Single test class
mvn test -pl knowbase-service -Dtest=RagAnswerGuardTest

# Single test method
mvn test -pl knowbase-service -Dtest=RagServiceTest#noHitsReturnsNotFoundWithoutLlm
```

**Frontend:**
- No test framework configured in `frontend/knowbase-ui/package.json` (no Vitest, Jest, or Cypress)
- No `*.test.js`, `*.spec.js`, or `*.test.vue` files in frontend

## Test File Organization

**Location:**
- All backend tests under `knowbase-service/src/test/java/`
- Mirror production package structure exactly (e.g. `com.knowbase.vector.rag.RagAnswerGuard` → `RagAnswerGuardTest.java` in same package path)
- ~46 unique test classes (~92 paths reported due to Windows path duplicates); no co-located tests in `src/main/`

**Naming:**
- `{ProductionClass}Test.java` — always singular `Test`
- Test methods: camelCase descriptive names, no `test` prefix: `noHitsReturnsNotFoundWithoutLlm`, `detectsDeadlineQuestion`, `mergesBothListsWithRrfAndPrefersDualMatches`

**Structure:**
```
knowbase-service/src/test/java/com/knowbase/
├── ingest/
│   ├── parse/          # OcrFallbackPolicyTest, ParsingExtractionModeTest, HtmlParsingContentProcessorTest
│   └── support/        # ParsedTextNormalizerTest, DocumentCleaningServiceTest, MimeTypeAllowlistTest
├── library/
│   ├── dto/            # VectorLibraryListQueryTest
│   └── service/        # LibraryCapacityValidatorTest, VersionUpdateStrategyTest
└── vector/
    ├── chunk/          # SemanticChunkerTest, HeadingLevelChunkerTest, IndexingChunkFilterTest
    ├── embedding/      # EmbeddingInputFormatterTest
    ├── rag/            # Rag* tests (largest cluster — ~25 classes)
    ├── retrieval/      # HybridSearchFusionTest, ChunkRerankServiceTest, RagRetrievalCacheTest
    └── service/        # RagServiceTest, ChunkingServiceTest, ChunkPreviewServiceTest
```

## Test Structure

**Suite Organization (pure utility — no mocks):**
```java
package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RagAnswerGuardTest {

    @Test
    void detectsDeadlineQuestion() {
        assertTrue(RagQuestionAnalyzer.isDeadlineQuestion("2025年周报截止时间？"));
        assertFalse(RagQuestionAnalyzer.isDeadlineQuestion("2025年8月周报主要内容"));
    }

    @Test
    void rejectsHedgingGuessAnswer() {
        SearchHit hit = new SearchHit(/* ... */);
        String result = RagAnswerGuard.enforceGrounding(bad, "2025年周报截止时间？", List.of(hit));
        assertEquals(RagAnswerTemplates.NO_EXPLICIT_DEADLINE, result);
    }
}
```

**Suite Organization (service with mocks):**
```java
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock private VectorSearchService searchService;
    @Mock private OllamaChatClient chatClient;
    // ...

    private RagService newService(RagProperties props, OllamaProperties ollama) {
        // manual wiring of real collaborators + mocked deps
        return new RagService(/* ... */);
    }

    @Test
    void noHitsReturnsNotFoundWithoutLlm() {
        when(searchService.searchForRag(any(), any())).thenReturn(new SearchResponse(List.of()));
        var response = service.chat(new RagChatRequest(/* ... */));
        assertFalse(response.found());
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }
}
```

**Patterns:**
- Test classes are package-private (no `public` modifier on class)
- Use `@BeforeEach void setUp()` when shared fixture setup needed (`ChunkingServiceTest`, `RagQueryRewriteServiceTest`)
- Inline test data construction — no shared abstract base classes or test fixtures directory
- Chinese test strings for domain-specific RAG scenarios (weekly reports, employee names, deadlines)
- Text blocks (`"""`) for multi-line tabular test content in `RagServiceTest`
- Private static helper methods in test classes for repeated object construction (`HybridSearchFusionTest.hit()`)

## Mocking

**Framework:**
- Mockito via JUnit 5 extension (`mockito-junit-jupiter` bundled in spring-boot-starter-test)
- `@Mock` fields + `@ExtendWith(MockitoExtension.class)` on service orchestration tests

**Classes using Mockito (~8 test files):**
- `RagServiceTest` — mocks search, chat client, prompt builder, metadata store, library service, chunk mapper
- `RagQueryRewriteServiceTest` — mocks `OllamaChatClient`
- `ChunkingServiceTest`, `ChunkPreviewServiceTest`, `ChunkRerankServiceTest`
- `LibraryCapacityValidatorTest`, `IdempotencyServiceTest`

**Patterns:**
```java
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

when(searchService.searchForRag(any(), any())).thenReturn(new SearchResponse(List.of(hit)));
verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
verify(chatClient).chat(anyString(), eq(history), eq("user prompt"), any());
```

**What to Mock:**
- External AI clients: `OllamaChatClient` (LLM calls)
- Database-facing services/mappers: `VectorSearchService`, `DocumentChunkMapper`, `DocMetadataStore`
- Heavy chunkers when testing orchestration: `SemanticChunker` in `ChunkingServiceTest`

**What NOT to Mock:**
- Pure static utility classes under `vector/rag/`, `vector/retrieval/`, `vector/chunk/`, `ingest/parse/`, `ingest/support/`
- Java records used as DTOs (`SearchHit`, `RagChatRequest`, `DocumentParseOptions`)
- Properties/config POJOs — instantiate directly and mutate fields in tests

## Fixtures and Factories

**Test Data:**
- Inline construction in each test method — no shared factory classes
- UUID constants for deterministic IDs: `UUID.fromString("00000000-0000-0000-0000-000000000001")`
- Domain-realistic Chinese content for RAG tests (weekly report rows, employee names, project names)
- Properties objects configured per test: `new RagProperties()`, `new TextNormalizationProperties()` with setters

**Location:**
- No `src/test/resources/` fixtures detected for most tests
- No `tests/fixtures/` or `TestDataBuilder` utilities

## Coverage

**Requirements:**
- No enforced coverage target
- No JaCoCo or Cobertura plugin in `pom.xml`
- No CI pipeline detected (no `.github/workflows/`)

**Configuration:**
- Not configured

**View Coverage:**
- Not available — add JaCoCo to `knowbase-service/pom.xml` if coverage reporting is needed

## Test Types

**Unit Tests (primary — all existing tests):**
- Pure logic tests: call static/instance methods directly, assert outputs
- Examples: `RagQuestionAnalyzerTest`, `OcrFallbackPolicyTest`, `HybridSearchFusionTest`, `ParsedTextNormalizerTest`
- Fast, no Spring context, no database

**Service Orchestration Unit Tests:**
- Wire real service classes with mocked dependencies via manual constructor calls (not `@InjectMocks`)
- Verify interaction boundaries (LLM called/not called, search skipped)
- Examples: `RagServiceTest` (13 test methods), `RagQueryRewriteServiceTest` (7 methods)

**Integration Tests:**
- Not present — no `@SpringBootTest`, `@WebMvcTest`, or `@DataJpaTest`
- No HTTP-level controller tests
- No database integration tests (PostgreSQL/pgvector not spun up in tests)

**E2E Tests:**
- Not used (backend or frontend)

## Domain Test Clusters

**RAG tests (`vector/rag/` — add new RAG logic tests here):**
- Question classification: `RagQuestionAnalyzerTest`, `RagQueryClassifierTest`
- Answer grounding: `RagAnswerGuardTest`
- Employee/project extraction: `RagEmployeeNameExtractorTest`, `RagEmployeeRosterSupportTest`, `ProjectParticipationCanonicalizerTest`
- Weekly report parsing: `RagWeeklyReportWeekSupportTest`, `RagWeeklyReportSummarySupportTest`, `WeeklyReportWorkItemExtractorTest`
- Prompt/query: `RagPromptBuilderTest`, `RagSearchQueryEnhancerTest`, `RagQueryRewriteServiceTest`
- Conversation: `RagConversationSupportTest`

**Retrieval tests (`vector/retrieval/`):**
- `HybridSearchFusionTest`, `RetrievalHitFilterTest`, `ChunkRerankServiceTest`, `RagRetrievalCacheTest`, `MetadataFilterResolverTest`

**Ingest tests (`ingest/` — add parse/normalize tests here):**
- Parse pipeline: `ParsingExtractionModeTest`, `OcrFallbackPolicyTest`, `OcrLanguageMapperTest`, `TikaParseHintsTest`, `HtmlParsingContentProcessorTest`
- Text cleanup: `ParsedTextNormalizerTest`, `DuplicateParagraphCleanerTest`, `DocumentCleaningServiceTest`, `MimeTypeAllowlistTest`

**Service-level RAG:**
- `RagServiceTest` — end-to-end chat flow decisions (rule-based vs LLM, no-hit paths, library stats shortcuts)

## Common Patterns

**Async Testing:**
- Not used — no `@Async` method tests detected

**Error Testing:**
```java
// Assert boolean guard outcomes rather than thrown exceptions for domain utilities
assertFalse(RagAnswerGuard.sourcesMentionExplicitDeadline(List.of(hit)));

// Config disabled path
props.setEnabled(false);
assertEquals("x", normalizer.normalize("  x  "));
```

**Parameterized Tests:**
- Not used — each scenario is a separate `@Test` method with descriptive name

**Snapshot Testing:**
- Not used

## Adding New Tests

**New RAG utility class** (e.g. `RagFooSupport.java`):
- Create `RagFooSupportTest.java` in `knowbase-service/src/test/java/com/knowbase/vector/rag/`
- No mocks; test with inline `SearchHit` / question strings
- Cover edge cases with Chinese domain strings matching existing RAG tests

**New service with external deps** (e.g. wraps LLM or DB):
- Use `@ExtendWith(MockitoExtension.class)` + `@Mock` for external boundaries
- Manually construct service under test in helper method (see `RagServiceTest.newService()`)
- Assert both return values and `verify()` call counts

**New ingest parser/normalizer:**
- Place test in matching `ingest/parse/` or `ingest/support/` package
- Test enabled/disabled config paths where applicable

**Frontend:**
- No test infrastructure — manual verification via `npm run dev` against proxied backend (`vite.config.js` proxies `/api` to `VITE_DEV_PROXY_TARGET`)

---

*Testing analysis: 2026-06-10*
*Update when test patterns change*
