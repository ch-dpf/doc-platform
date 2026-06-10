---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
---

# Coding Conventions

**Analysis Date:** 2026-06-10

## Naming Patterns

**Files (Java):**
- PascalCase for classes: `RagAnswerGuard.java`, `DocumentController.java`
- Mirror production package under `knowbase-service/src/test/java/` for tests
- Test files: `{ClassName}Test.java` (singular `Test`, not `Tests`)

**Files (Vue / frontend):**
- PascalCase for Vue SFC components: `PageCard.vue`, `CreateLibraryWizard.vue`, `QaView.vue`
- camelCase for JS modules: `client.js`, `useLibraryContext.js`, `ingestErrors.js`
- Views live in `frontend/knowbase-ui/src/views/` with `*View.vue` suffix
- API wrappers in `frontend/knowbase-ui/src/api/` named by domain (`ingest.js`, `library.js`, `search.js`)

**Functions (Java):**
- camelCase for methods: `enforceGrounding`, `searchForRag`, `normalize`
- Static utility methods on `final` classes for stateless logic (RAG analyzers, chunkers, parsers)
- Boolean predicates: `is*`, `has*`, `should*`: `isDeadlineQuestion`, `shouldFallback`, `requiresHtmlPipeline`

**Functions (JavaScript/Vue):**
- camelCase for functions and composables: `useLibraryContext`, `formatApiErrorPayload`, `buildRetrievalPreviewPayload`
- Composables prefixed with `use`: `useLibraryContext.js`, `usePageTitle.js`
- Event handlers in templates use inline `@click="load(1)"` or named handlers like `onLibraryChange`, `submitBatchReindex`

**Variables:**
- camelCase for locals and fields: `libraryId`, `chatSettings`, `retrievalProperties`
- UPPER_SNAKE_CASE for Java constants and error codes: `INGEST_ERROR_CATALOG`, `DEFAULT_LIBRARY_ID`, `EXPLICIT_DEADLINE_IN_TEXT`
- Java enums use PascalCase type names with UPPER_SNAKE values: `ParseStatus.PENDING`, `ChunkingStrategy.FIXED_CHAR`

**Types:**
- Java DTOs and API payloads: `public record` with compact constructors (`SearchHit`, `RagChatRequest`, `CreateVectorLibraryRequest`)
- Java domain exceptions: `{Entity}NotFoundException`, `Invalid{Entity}Exception` extending `RuntimeException`
- Java config/settings: plain POJOs or records under `config/` packages (`RagProperties`, `VectorLibraryConfig`)
- Vue: `defineProps({ title: { type: String, default: '' } })` — no TypeScript; prop types declared inline

## Code Style

**Formatting (Java):**
- Java 21 (`pom.xml` `<java.version>21</java.version>`)
- UTF-8 source encoding
- 4-space indentation (standard Java)
- No project-wide formatter config detected (no Spotless, Checkstyle, or EditorConfig in repo)

**Formatting (JavaScript/Vue):**
- No Prettier or ESLint config in `frontend/knowbase-ui/`
- Single quotes for strings, no semicolons at statement ends (observed in `client.js`, `router/index.js`)
- 2-space indentation in Vue SFCs
- Template → script → style block order in `.vue` files

**Linting:**
- Not configured for frontend (no `eslint.config.*`, no `npm run lint` script)
- Backend relies on compiler + IDE defaults; no Checkstyle/SpotBugs in `pom.xml`

## Import Organization

**Java order:**
1. Same-project packages (`com.knowbase.*`)
2. Third-party (Spring, JUnit, Mockito, SLF4J)
3. JDK (`java.util`, `java.time`)
4. Static imports last: `import static org.junit.jupiter.api.Assertions.*`, `import static org.mockito.Mockito.*`

**JavaScript order:**
1. Vue / Vue Router
2. Third-party (`element-plus`, `@element-plus/icons-vue`, `axios`)
3. Relative imports to `../api/`, `../components/`, `../composables/`, `../utils/`

**Path Aliases:**
- Frontend: none — use relative paths (`../api/client`, `../utils/ingestErrors`)
- Backend: standard Maven package layout only

## Error Handling

**Backend patterns:**
- Domain failures throw typed `RuntimeException` subclasses co-located with the owning module:
  - `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentNotFoundException.java`
  - `knowbase-service/src/main/java/com/knowbase/library/service/LibraryNotFoundException.java`
  - `knowbase-service/src/main/java/com/knowbase/vector/client/ChatException.java`
- Global mapping in `knowbase-service/src/main/java/com/knowbase/ingest/controller/ApiExceptionHandler.java`:
  - Domain/not-found → `ProblemDetail` with HTTP 404
  - Validation/capacity → `ProblemDetail` with HTTP 400, optional `errorCode` property
  - External AI failures (`ChatException`, `EmbeddingException`) → `Map.of("error", message)` with HTTP 503
- Use `jakarta.validation` annotations on request records (`@NotBlank` on `CreateVectorLibraryRequest`)
- Controller methods do not catch exceptions — let `@RestControllerAdvice` handle them

**Frontend patterns:**
- Central axios client in `frontend/knowbase-ui/src/api/client.js` with response interceptor
- Global toast via `ElMessage.error` unless `config.skipGlobalErrorToast` is set
- Friendly error mapping in `frontend/knowbase-ui/src/utils/ingestErrors.js` — maps `ProblemDetail.errorCode` to user-facing Chinese messages with hints
- Views catch errors locally only when they need custom UX (e.g., `ElMessageBox.confirm` flows); otherwise rely on global interceptor

## Logging

**Framework:**
- SLF4J with `LoggerFactory.getLogger(ClassName.class)` in service/client classes
- Example: `knowbase-service/src/main/java/com/knowbase/vector/client/OllamaChatClient.java`

**Patterns:**
- `log.info` for startup/config confirmation
- `log.warn` for recoverable degradation (external service unreachable, storage cleanup failure)
- `log.debug` for verbose streaming/diagnostic output
- No logging in pure utility classes (`RagAnswerGuard`, `HybridSearchFusion`, `ParsedTextNormalizer`)
- Frontend: no structured logger — avoid `console.log` in committed view code; use Element Plus messages for user feedback

## Comments

**When to Comment:**
- Java: class-level Javadoc on non-obvious business rules (Chinese acceptable for domain logic), e.g. `RagAnswerGuard` header comment
- Java: inline comments for version constraints or non-obvious deps, e.g. PDFBox version note in `knowbase-service/pom.xml`
- JavaScript: file-level block comment for catalog/constants (`ingestErrors.js`)
- Avoid restating obvious code

**JSDoc/TSDoc:**
- Not used — frontend is plain JavaScript without TypeScript

**TODO Comments:**
- Not detected in `knowbase-service/src/main/`

## Function Design

**Size:**
- Keep RAG/ingest utilities focused — extract support classes rather than growing god methods (`RagQuestionAnalyzer`, `RagWeeklyReportWeekSupport`, `RagEmployeeNameExtractor`)
- Vue views can be large (500–900+ lines in `QaView.vue`, `DocumentsView.vue`) — acceptable pattern here; extract reusable pieces to `components/` or `utils/` when reused

**Parameters:**
- Java records for multi-field inputs (4+ fields): `RagChatRequest`, `DocumentParseOptions`, `SearchRequest`
- Java service methods accept domain IDs as `UUID`, tenant as `String`
- Frontend payload builders (e.g. `buildRetrievalPreviewPayload` in `search.js`) accept a single options object

**Return Values:**
- Java records for structured responses: `RagChatResponse`, `SearchResponse`, `PageResponse`
- Early returns in guard/validation utilities
- Vue composables return reactive refs/computed models

## Module Design

**Backend package layout (`com.knowbase.*`):**
- `ingest/` — document upload, parse, storage, pipeline
- `library/` — vector library CRUD, config, capacity
- `vector/` — chunking, embedding, retrieval, RAG
- `chat/` — conversation persistence and chat API
- Sub-packages: `controller`, `service`, `dto`, `domain`, `config`, `mapper`, `support`, `rag`, `chunk`, `retrieval`, `client`

**Frontend layout:**
- `src/views/` — route-level pages
- `src/components/` — shared UI pieces (`PageCard.vue`, wizards, drawers)
- `src/api/` — thin axios wrappers per backend domain
- `src/composables/` — shared reactive state (`useLibraryContext.js`)
- `src/utils/` — pure helpers (`ingestErrors.js`, `ragChat.js`, `libraryConfig.js`)
- `src/router/index.js` — flat route table with legacy redirects

**Exports:**
- Java: package-private test classes; public API classes are `public`
- JS: named exports for functions/constants; default export only for `client.js` and router
- Vue SFCs: no barrel `index.js` files — import components by full path

**Barrel Files:**
- Not used in frontend or backend

## REST API Conventions

- Base path: `/api/v1/`
- Resource-oriented controllers: `DocumentController` → `/api/v1/documents`, vector library routes under `/api/v1/vector-libraries`
- OpenAPI annotations via Knife4j: `@Tag`, `@Operation` on controllers
- Request/response bodies use JSON; records/DTOs serialize via Jackson
- Multipart upload endpoints use `@RequestParam MultipartFile`

## Spring Conventions

- Constructor injection for services/controllers (no field `@Autowired`)
- `@ConfigurationProperties` classes in `config/` packages bound from `application.yml`
- MyBatis-Plus mappers in `mapper/` packages
- Async ingest via `@Async` configured in `knowbase-service/src/main/java/com/knowbase/ingest/config/AsyncConfig.java`

---

*Convention analysis: 2026-06-10*
*Update when patterns change*
