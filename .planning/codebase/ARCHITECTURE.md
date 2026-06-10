---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
analysis_date: 2026-06-10
focus: arch
---

<!-- refreshed: 2026-06-10 -->
# Architecture

**Analysis Date:** 2026-06-10

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                     Frontend (Vue 3 + Element Plus)                       │
│  `frontend/knowbase-ui/src/views/*`  `frontend/knowbase-ui/src/api/*`     │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ HTTP /api/v1/*
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              knowbase-service (Spring Boot modular monolith)              │
├─────────────┬─────────────┬──────────────┬──────────────┬────────────────┤
│   library   │   ingest    │    vector    │     chat     │   platform     │
│  controllers│  controllers│  controllers │  controllers │  coordinators  │
│  + services │  + pipeline │  + RAG/search│  + memory    │  + shared util │
└──────┬──────┴──────┬──────┴──────┬───────┴──────┬───────┴────────┬───────┘
       │             │             │              │                │
       ▼             ▼             ▼              ▼                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  PostgreSQL (pgvector)          Object storage (MinIO or local FS)        │
│  `infra/postgres/init.sql`      `knowbase-service/.../storage/*`           │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  External: Ollama (embeddings + chat LLM)                                 │
│  `knowbase-service/.../vector/client/OllamaChatClient.java`               │
└──────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Vector library CRUD | Create/list/update/delete knowledge bases and per-library JSON config | `knowbase-service/src/main/java/com/knowbase/library/controller/VectorLibraryController.java` |
| Document upload & ingest | Accept files, persist metadata, store raw bytes, schedule parse | `knowbase-service/src/main/java/com/knowbase/ingest/controller/DocumentController.java` |
| Parse pipeline | Tika/OCR extract, normalize, clean, write parsed text to storage | `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentPipelineService.java` |
| Index coordinator | Bridge ingest → vector indexing with idempotency (replaces Kafka) | `knowbase-service/src/main/java/com/knowbase/platform/DocumentIndexCoordinator.java` |
| Chunking & embedding | Split parsed text, embed via Ollama, persist pgvector rows | `knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java` |
| Hybrid search | Vector + BM25 RRF fusion, rerank, metadata filters | `knowbase-service/src/main/java/com/knowbase/vector/service/VectorSearchService.java` |
| RAG Q&A | Retrieve chunks, build prompt, call LLM, return citations | `knowbase-service/src/main/java/com/knowbase/vector/service/RagService.java` |
| Conversation chat | Persist multi-turn history, delegate to RAG | `knowbase-service/src/main/java/com/knowbase/chat/service/ConversationChatService.java` |
| Library config | Resolve per-library parsing/chunking/retrieval rules from JSON | `knowbase-service/src/main/java/com/knowbase/library/service/LibraryConfigResolver.java` |
| Frontend shell | Sidebar nav, library/tenant context, route to feature views | `frontend/knowbase-ui/src/App.vue` |

## Pattern Overview

**Overall:** Modular monolith — single Spring Boot deployable with domain packages (`ingest`, `vector`, `library`, `chat`) and a thin `platform` coordination layer.

**Key Characteristics:**
- Package-by-feature under `com.knowbase.*` with classic controller → service → mapper layering
- Per-library configuration stored as JSONB (`vector_library.config_json`) resolved at runtime via `LibraryConfigResolver`
- Async document parse and index jobs via `@EnableAsync` + post-commit scheduling (no message broker)
- PostgreSQL + pgvector as the single source of truth for metadata, chunks, embeddings, and chat history
- Frontend SPA calls REST under `/api/v1/*`; dev proxy in `frontend/knowbase-ui/vite.config.js`

## Layers

**API / Controller Layer:**
- Purpose: HTTP boundary, OpenAPI tags, request validation, response DTO mapping
- Location: `knowbase-service/src/main/java/com/knowbase/*/controller/`
- Contains: `@RestController` classes, `@Valid` request bodies, Swagger `@Tag` annotations
- Depends on: Service layer only (no direct mapper access from controllers)
- Used by: Vue API modules in `frontend/knowbase-ui/src/api/`

**Service Layer:**
- Purpose: Business logic, orchestration, transaction boundaries
- Location: `knowbase-service/src/main/java/com/knowbase/*/service/`
- Contains: `@Service` classes (`UploadService`, `IndexingService`, `RagService`, `VectorLibraryService`, etc.)
- Depends on: Mappers/stores, config resolvers, external clients, other services
- Used by: Controllers and platform coordinators

**Domain & Persistence Layer:**
- Purpose: Entity records, MyBatis-Plus mappers, SQL access to PostgreSQL/pgvector
- Location: `knowbase-service/src/main/java/com/knowbase/*/domain/`, `*/mapper/`, `*/support/*Store.java`
- Contains: `DocMetadata`, `VectorLibrary`, `DocumentChunkMapper`, `ChatMessageMapper`
- Depends on: Database schema in `infra/postgres/init.sql`
- Used by: Service layer

**Platform Coordination Layer:**
- Purpose: Cross-domain glue that would have been event-driven in a distributed setup
- Location: `knowbase-service/src/main/java/com/knowbase/platform/`, `knowbase-service/src/main/java/com/knowbase/event/`
- Contains: `DocumentIndexCoordinator`, `IndexStatusUpdater`, lifecycle event records
- Depends on: Ingest and vector services
- Used by: `DocumentPipelineService` after parse completes or on delete

**Support / Strategy Layer (vector & ingest):**
- Purpose: Pure algorithms and pluggable strategies without Spring web concerns
- Location: `knowbase-service/src/main/java/com/knowbase/vector/chunk/`, `vector/retrieval/`, `vector/rag/`, `ingest/parse/`, `ingest/support/`
- Contains: Chunkers, hybrid fusion, RAG prompt builders, Tika processors, text normalizers
- Depends on: Config properties and library rules
- Used by: Service layer

**Frontend Presentation Layer:**
- Purpose: User-facing workflows for library admin, ingest, documents, chunks, Q&A
- Location: `frontend/knowbase-ui/src/views/`, `components/`, `composables/`
- Contains: Vue SFCs, Element Plus UI, axios API wrappers
- Depends on: Backend REST API via `frontend/knowbase-ui/src/api/client.js`
- Used by: Browser clients

## Data Flow

### Document Ingest Pipeline

1. Client uploads file → `DocumentController.upload` (`knowbase-service/src/main/java/com/knowbase/ingest/controller/DocumentController.java`)
2. `UploadService` validates constraints via `LibraryConfigResolver`, delegates to `DocumentIngestor.ingestOne`
3. `DocumentIngestor` writes raw file to object storage, upserts `doc_metadata`, increments library document count
4. `DocumentPipelineService.scheduleProcessAfterCommit` queues async parse after DB commit
5. `DocumentPipelineService.processAsync`: Tika/OCR parse → normalize → clean → store `parsed.txt` in object storage
6. If `index_requested`, emits `DocumentReadyForIndexEvent` → `DocumentIndexCoordinator.processReadyForIndex`
7. Coordinator checks `IdempotencyService`, calls `IndexingService.index`

### Chunking & Indexing

1. `IndexingService.index` loads parsed text via `ParsedTextFetcher` from storage key/URL
2. Applies library cleaning rules, runs `ChunkingService.chunk` (strategy from library config: FIXED_CHAR, HEADING_LEVEL, SEMANTIC, PARAGRAPH_FIRST)
3. `IndexingChunkFilter.removeHeaderOnlyChunks` drops noise chunks
4. `LibraryEmbeddingService.embedBatch` calls Ollama embedding endpoint
5. Deletes prior chunks for doc+version, inserts new rows via `DocumentChunkMapper.insertChunk` (pgvector column)
6. Updates `DocumentIndexJob` status and doc `index_status` via `IndexStatusUpdater`

### Hybrid Vector Search

1. Client POST `/api/v1/vector-libraries/{libraryId}/search` → `SearchController` → `VectorSearchService.search`
2. Pipeline embeds query, runs vector similarity + BM25 full-text (`HybridSearchFusion` with RRF)
3. Optional rerank via `ChunkRerankService`, metadata filter via `MetadataFilterResolver`, min-score filter
4. Returns `SearchResponse` with ranked `SearchHit` list

### RAG Q&A (Stateless)

1. Client POST `/api/v1/rag/chat` or `/chat/stream` → `RagController` → `RagService`
2. `RagService` classifies question (conversational, library stats, domain-specific shortcuts in `vector/rag/*`)
3. For retrieval path: `RagRetrievalService` rewrites query, calls `VectorSearchService.searchForRag`, merges hits (`RagHitMerger`)
4. `RagPromptBuilder` assembles context + history; `OllamaChatClient` generates answer
5. Returns `RagChatResponse` with citations, or SSE stream of `RagStreamEvent`

### Conversation Chat (Persistent Memory)

1. Client creates conversation → `ConversationController.create` → `ChatConversationService`
2. User sends message → `ConversationChatService.chat` loads history from `ChatMemoryService`
3. Builds `RagChatRequest` with history, calls `RagService.chat`, persists assistant message with citations
4. Stream variant uses `RagService.chatStream` and saves on `done` event

### Document Delete / Purge

1. Soft delete: `DocumentPipelineService.deleteDocument` marks `deleted=true`, `DocumentIndexCoordinator.processDeleted` → `IndexingService.delete`
2. Purge: removes storage artifacts and metadata row, also triggers vector cleanup

**State Management:**
- Server-side: PostgreSQL for all durable state (libraries, documents, chunks, jobs, conversations)
- Client-side: `useLibraryContext` persists `libraryId` and `tenantId` in `localStorage` (`frontend/knowbase-ui/src/composables/useLibraryContext.js`)
- No global in-memory application state beyond Spring singleton beans and optional RAG retrieval cache

## Key Abstractions

**Vector Library:**
- Purpose: Logical knowledge base with embedded pipeline config (parsing, chunking, embedding, retrieval, governance)
- Examples: `knowbase-service/src/main/java/com/knowbase/library/domain/VectorLibrary.java`, `library/config/VectorLibraryConfig.java`
- Pattern: JSONB config merged with global defaults via `LibraryConfigResolver`

**Document Lifecycle Event:**
- Purpose: Decouple ingest completion from indexing without a message broker
- Examples: `knowbase-service/src/main/java/com/knowbase/event/DocumentReadyForIndexEvent.java`, `DocumentDeletedEvent.java`
- Pattern: Immutable records passed synchronously to `DocumentIndexCoordinator`

**Object Storage Abstraction:**
- Purpose: Store raw uploads and parsed text (MinIO or local filesystem)
- Examples: `knowbase-service/src/main/java/com/knowbase/ingest/storage/ObjectStorageService.java`, `MinioDocumentObjectStorage.java`, `LocalFsDocumentObjectStorage.java`
- Pattern: Interface + Spring `@ConditionalOnProperty` implementation selection

**Chunking Strategy:**
- Purpose: Split normalized text into retrieval units
- Examples: `knowbase-service/src/main/java/com/knowbase/vector/chunk/FixedLengthChunker.java`, `SemanticChunker.java`, `HeadingLevelChunker.java`
- Pattern: Selected by `ChunkingProperties.strategy` enum in `ChunkingService`

**Search Pipeline:**
- Purpose: Unified retrieval for preview search and RAG
- Examples: `VectorSearchService.runSearchPipeline`, `HybridSearchFusion`, `ChunkRerankService`
- Pattern: Single pipeline with flags for RAG mode and trace output

**RAG Support Modules:**
- Purpose: Domain-specific query handling before generic retrieval
- Examples: `knowbase-service/src/main/java/com/knowbase/vector/rag/RagQuestionAnalyzer.java`, `RagQueryClassifier.java`, `RagPromptBuilder.java`
- Pattern: Static or injectable helpers called from `RagService` in priority order

## Entry Points

**Spring Boot Application:**
- Location: `knowbase-service/src/main/java/com/knowbase/KnowbaseApplication.java`
- Triggers: `java -jar` or IDE run
- Responsibilities: Component scan, `@EnableAsync`, MyBatis `@MapperScan` for four mapper packages

**REST API Controllers:**

| Controller | Base Path | Purpose |
|------------|-----------|---------|
| `VectorLibraryController` | `/api/v1/vector-libraries` | Library CRUD and settings |
| `DocumentController` | `/api/v1/documents` | Upload, list, parse preview, approve index, chunks |
| `UploadTaskController` | `/api/v1/upload-tasks` | Async large-file upload status |
| `IndexAdminController` | `/api/v1/index` | Chunk preview, library rebuild |
| `SearchController` | `/api/v1/vector-libraries/{id}/search` | Hybrid search preview |
| `RagController` | `/api/v1/rag` | Stateless RAG chat (sync + SSE) |
| `RagRetrievalController` | `/api/v1/rag/retrieval-preview` | RAG-path retrieval debug |
| `ConversationController` | `/api/v1/conversations`, `/api/v1/vector-libraries/{id}/conversations` | Persistent multi-turn chat |

**Vue SPA:**
- Location: `frontend/knowbase-ui/src/main.js` → `App.vue` + `router/index.js`
- Triggers: Browser navigation
- Routes: `/vector-libraries`, `/ingest`, `/documents`, `/documents/:docId/chunks`, `/qa`

## Architectural Constraints

- **Threading:** HTTP threads for sync APIs; `@Async` for parse pipeline and index jobs; Reactor `Flux` for SSE streaming endpoints
- **Transactions:** Upload/ingest uses `REQUIRES_NEW` per file; parse/index update metadata in `@Transactional` service methods; index scheduling deferred until after commit
- **Idempotency:** `IdempotencyService` prevents duplicate index/delete processing per doc+version+event type
- **Versioning:** Documents carry integer `version`; stale async jobs skip if DB version mismatch
- **Library immutability:** Pipeline config fields become read-only once a library has documents (`VectorLibraryController.updateSettings` description)
- **Tenant scoping:** APIs require `tenantId` query/body param; no auth middleware — tenant is caller-supplied
- **Circular imports:** None observed across packages; `platform` depends on both `ingest` and `vector` but not vice versa for web layer

## Anti-Patterns

### Calling mappers from controllers

**What happens:** Controllers inject MyBatis mappers directly.
**Why it's wrong:** Bypasses validation, config resolution, and transaction boundaries established in services.
**Do this instead:** Add or extend a `@Service` method (pattern used throughout `DocumentController` → `UploadService` → `DocumentIngestor`).

### Triggering index before DB commit

**What happens:** Async parse reads stale version if index fires inside an uncommitted transaction.
**Why it's wrong:** Race on document version upgrades causes skipped or duplicate indexing.
**Do this instead:** Use `scheduleProcessAfterCommit` / `scheduleIndexAfterCommit` pattern in `DocumentPipelineService` and `DocumentIndexCoordinator`.

### Bypassing LibraryConfigResolver for library rules

**What happens:** Hard-coded chunk sizes or MIME allowlists in feature code.
**Why it's wrong:** Per-library JSON config in `vector_library.config_json` becomes ineffective.
**Do this instead:** Resolve via `LibraryConfigResolver.chunkingFor`, `parsingFor`, `retrievalFor`, etc.

## Error Handling

**Strategy:** Domain-specific unchecked exceptions bubble to `@RestControllerAdvice`; responses use RFC 7807 `ProblemDetail`.

**Patterns:**
- Not found: `DocumentNotFoundException`, `LibraryNotFoundException`, `ConversationNotFoundException` → HTTP 404
- Validation: `InvalidDocumentException`, `MethodArgumentNotValidException` → HTTP 400 with detail
- Capacity: `LibraryCapacityExceededException` → HTTP 409
- External failures: `EmbeddingException`, `ChatException` → mapped in `ApiExceptionHandler`
- Frontend: axios interceptor in `frontend/knowbase-ui/src/api/client.js` shows Element Plus toast on error

## Cross-Cutting Concerns

**Logging:**
- SLF4J via `LoggerFactory` in services (`DocumentPipelineService`, `IndexingService`, `DocumentIndexCoordinator`)

**Validation:**
- Jakarta Bean Validation on request DTOs (`@Valid` in controllers)
- Business validation in services (`LibraryConfigResolver.requireLibrary`, `MimeTypeAllowlist`, `LibraryCapacityValidator`)

**Authentication:**
- Not implemented — APIs trust caller-provided `tenantId`; suitable only for trusted/internal deployments

**OpenAPI:**
- Knife4j/Swagger via `knowbase-service/src/main/java/com/knowbase/ingest/config/OpenApiConfig.java`; linked from frontend sidebar

**Configuration:**
- Global defaults: Spring `@ConfigurationProperties` in `*/config/*Properties.java`
- Per-library overrides: JSON in DB resolved by `LibraryConfigResolver`

---

*Architecture analysis: 2026-06-10*
*Update when major patterns change*
