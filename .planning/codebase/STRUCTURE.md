---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
analysis_date: 2026-06-10
focus: arch
---

# Codebase Structure

**Analysis Date:** 2026-06-10

## Directory Layout

```
doc-platform/
├── knowbase-service/              # Spring Boot backend (sole Maven module)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/knowbase/
│       │   ├── KnowbaseApplication.java
│       │   ├── chat/              # Persistent conversation + memory
│       │   ├── config/            # Shared Spring config (if any)
│       │   ├── event/             # Document lifecycle event records
│       │   ├── ingest/            # Upload, parse, document metadata
│       │   ├── library/           # Vector library CRUD + config
│       │   ├── orchestration/     # Empty (legacy placeholder)
│       │   ├── platform/          # Cross-domain coordinators
│       │   └── vector/            # Chunking, embedding, search, RAG
│       └── test/java/com/knowbase/  # JUnit tests mirroring main packages
├── frontend/
│   └── knowbase-ui/               # Vue 3 SPA
│       ├── package.json
│       ├── vite.config.js
│       ├── .env.example           # VITE_API_BASE, VITE_DEV_PROXY_TARGET
│       └── src/
│           ├── main.js
│           ├── App.vue
│           ├── api/               # Axios wrappers per domain
│           ├── components/        # Shared Vue components
│           ├── composables/       # Vue composition utilities
│           ├── router/
│           ├── styles/
│           ├── utils/
│           └── views/             # Route-level pages
├── infra/
│   ├── postgres/                  # Schema init + migrations
│   └── tesseract/                 # OCR tessdata placeholder
├── scripts/                       # PowerShell/bash ops scripts
├── pom.xml                        # Parent POM (modules: knowbase-service)
└── build.cmd                      # Windows build entry
```

## Directory Purposes

**`knowbase-service/src/main/java/com/knowbase/ingest/`:**
- Purpose: Document acquisition, parsing, cleaning, metadata persistence
- Contains: `controller/`, `service/`, `domain/`, `mapper/`, `dto/`, `config/`, `parse/`, `storage/`, `support/`
- Key files: `DocumentController.java`, `DocumentPipelineService.java`, `DocumentIngestor.java`, `UploadService.java`
- Subdirectories: `parse/` holds Tika/OCR processors; `storage/` holds MinIO/local FS adapters

**`knowbase-service/src/main/java/com/knowbase/vector/`:**
- Purpose: Chunking, embedding, pgvector storage, hybrid search, RAG
- Contains: `controller/`, `service/`, `mapper/`, `dto/`, `domain/`, `config/`, `chunk/`, `retrieval/`, `rag/`, `embedding/`, `client/`
- Key files: `IndexingService.java`, `VectorSearchService.java`, `RagService.java`, `ChunkingService.java`, `DocumentChunkMapper.java`
- Subdirectories: `rag/` for prompt/query logic; `retrieval/` for fusion, rerank, filters; `client/` for Ollama HTTP

**`knowbase-service/src/main/java/com/knowbase/library/`:**
- Purpose: Knowledge base (vector library) management and per-library configuration
- Contains: `controller/`, `service/`, `domain/`, `mapper/`, `dto/`, `config/`
- Key files: `VectorLibraryController.java`, `VectorLibraryService.java`, `LibraryConfigResolver.java`, `VectorLibraryConfig.java`

**`knowbase-service/src/main/java/com/knowbase/chat/`:**
- Purpose: Conversation CRUD and message persistence wrapping RAG
- Contains: `controller/`, `service/`, `domain/`, `mapper/`, `dto/`, `config/`
- Key files: `ConversationController.java`, `ConversationChatService.java`, `ChatMemoryService.java`

**`knowbase-service/src/main/java/com/knowbase/platform/`:**
- Purpose: Shared utilities and ingest↔vector coordination
- Contains: `DocumentIndexCoordinator.java`, `IndexStatusUpdater.java`, `JsonSupport.java`

**`knowbase-service/src/main/java/com/knowbase/event/`:**
- Purpose: Typed lifecycle event records (not Spring ApplicationEvents)
- Contains: `DocumentReadyForIndexEvent.java`, `DocumentDeletedEvent.java`, `DocumentEventType.java`, `IdempotencyKeys.java`

**`frontend/knowbase-ui/src/views/`:**
- Purpose: Top-level routed pages
- Contains: `VectorLibrariesView.vue`, `VectorLibraryDetailView.vue`, `IngestView.vue`, `DocumentsView.vue`, `DocumentChunksView.vue`, `QaView.vue`
- Key files: Each view pairs with API modules and composables

**`frontend/knowbase-ui/src/api/`:**
- Purpose: Thin HTTP client functions grouped by backend domain
- Contains: `client.js`, `library.js`, `ingest.js`, `vector.js`, `search.js`, `conversation.js`, `chunk.js`

**`infra/postgres/`:**
- Purpose: Database bootstrap and incremental migrations
- Contains: `init.sql` (full schema), `migrate-*.sql`, `bootstrap-knowbase-local.sql`
- Key tables: `vector_library`, `doc_metadata`, `document_chunk`, `document_index_job`, `chat_conversation`, `chat_message`

**`scripts/`:**
- Purpose: Local dev and ops automation
- Contains: `start-services.ps1`, `start-infra.ps1`, `build.ps1`, `e2e-test.ps1`, `reset-db.ps1`

## Key File Locations

**Entry Points:**
- `knowbase-service/src/main/java/com/knowbase/KnowbaseApplication.java`: Spring Boot main
- `frontend/knowbase-ui/src/main.js`: Vue app bootstrap
- `frontend/knowbase-ui/index.html`: SPA HTML shell

**Configuration:**
- `pom.xml`: Parent Maven project, Java 21, Spring Boot 3.2.4
- `knowbase-service/pom.xml`: Backend dependencies
- `knowbase-service/src/main/resources/application*.yml`: Spring properties (not quoted here)
- `frontend/knowbase-ui/vite.config.js`: Dev server proxy to backend `:8080`
- `frontend/knowbase-ui/.env.example`: Frontend env var template (`.env` present locally — do not commit secrets)

**Core Logic — Ingest:**
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentIngestor.java`: Upload transaction + schedule parse
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentPipelineService.java`: Async parse/normalize/clean/index trigger
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentParseService.java`: Tika text extraction
- `knowbase-service/src/main/java/com/knowbase/ingest/support/DocMetadataStore.java`: Metadata persistence facade

**Core Logic — Vector / RAG:**
- `knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java`: Chunk + embed + store
- `knowbase-service/src/main/java/com/knowbase/vector/service/VectorSearchService.java`: Hybrid search pipeline
- `knowbase-service/src/main/java/com/knowbase/vector/service/RagService.java`: RAG orchestration
- `knowbase-service/src/main/java/com/knowbase/vector/service/RagRetrievalService.java`: RAG-specific retrieval path
- `knowbase-service/src/main/java/com/knowbase/platform/DocumentIndexCoordinator.java`: Ingest→index bridge

**Core Logic — Library:**
- `knowbase-service/src/main/java/com/knowbase/library/service/VectorLibraryService.java`: Library lifecycle
- `knowbase-service/src/main/java/com/knowbase/library/service/LibraryConfigResolver.java`: Config merge/lookup

**API Controllers:**
- `knowbase-service/src/main/java/com/knowbase/ingest/controller/DocumentController.java`: `/api/v1/documents`
- `knowbase-service/src/main/java/com/knowbase/library/controller/VectorLibraryController.java`: `/api/v1/vector-libraries`
- `knowbase-service/src/main/java/com/knowbase/vector/controller/SearchController.java`: hybrid search
- `knowbase-service/src/main/java/com/knowbase/vector/controller/RagController.java`: stateless RAG
- `knowbase-service/src/main/java/com/knowbase/chat/controller/ConversationController.java`: persistent chat
- `knowbase-service/src/main/java/com/knowbase/ingest/controller/ApiExceptionHandler.java`: global errors

**Frontend Routes:**
- `frontend/knowbase-ui/src/router/index.js`: Route table and legacy redirects

**Testing:**
- `knowbase-service/src/test/java/com/knowbase/`: JUnit 5 tests co-located by package (e.g. `vector/rag/`, `ingest/support/`)
- `scripts/e2e-test.ps1`: End-to-end smoke script

**Documentation:**
- `scripts/README.md`: Script usage
- `infra/tesseract/README.md`: OCR setup

## Naming Conventions

**Java Files:**
- PascalCase classes: `DocumentPipelineService.java`, `RagChatRequest.java`
- Package names lowercase by domain: `com.knowbase.ingest.service`, `com.knowbase.vector.rag`
- Suffix patterns: `*Controller`, `*Service`, `*Mapper`, `*Properties`, `*Exception`, `*Request`/`*Response` (DTO records)
- Support facades: `*Store` for persistence wrappers (`DocMetadataStore`, `DocumentIndexJobStore`)

**Vue / JavaScript Files:**
- PascalCase for Vue SFCs: `QaView.vue`, `CreateLibraryWizard.vue`
- camelCase for JS modules: `useLibraryContext.js`, `ragChat.js`, `library.js`
- API modules named by backend domain: `ingest.js`, `conversation.js`, `search.js`

**Directories:**
- Backend: singular domain nouns (`ingest`, `vector`, `library`, `chat`)
- Frontend: plural for collections (`views`, `components`, `utils`, `api`)
- SQL migrations: `migrate-{topic}.sql` in `infra/postgres/`

**REST Paths:**
- Prefix: `/api/v1/`
- Resource-oriented: `/documents`, `/vector-libraries`, `/conversations`, `/rag`, `/index`

**Database:**
- snake_case table/column names: `doc_metadata`, `library_id`, `parse_status`

## Where to Add New Code

**New ingest API endpoint:**
- Controller method: `knowbase-service/src/main/java/com/knowbase/ingest/controller/DocumentController.java` (or new controller in same package)
- Business logic: `knowbase-service/src/main/java/com/knowbase/ingest/service/`
- DTO: `knowbase-service/src/main/java/com/knowbase/ingest/dto/`
- Frontend client: `frontend/knowbase-ui/src/api/ingest.js`
- Tests: `knowbase-service/src/test/java/com/knowbase/ingest/`

**New vector/search/RAG capability:**
- Service logic: `knowbase-service/src/main/java/com/knowbase/vector/service/`
- Pure algorithms: `knowbase-service/src/main/java/com/knowbase/vector/chunk/` or `retrieval/` or `rag/`
- Controller: `knowbase-service/src/main/java/com/knowbase/vector/controller/`
- Mapper/SQL: `knowbase-service/src/main/java/com/knowbase/vector/mapper/`
- Frontend: `frontend/knowbase-ui/src/api/search.js` or `vector.js`; UI in `views/QaView.vue` or new view + route

**New library configuration field:**
- Config model: `knowbase-service/src/main/java/com/knowbase/library/config/` (e.g. extend `ParsingRulesSettings`)
- Merge logic: `knowbase-service/src/main/java/com/knowbase/library/config/VectorLibraryConfigMerger.java`
- Resolver accessor: `knowbase-service/src/main/java/com/knowbase/library/service/LibraryConfigResolver.java`
- Frontend editor: `frontend/knowbase-ui/src/components/EditLibrarySettingsDrawer.vue`, `utils/libraryConfig.js`

**New chat/conversation feature:**
- Service: `knowbase-service/src/main/java/com/knowbase/chat/service/`
- Controller: `knowbase-service/src/main/java/com/knowbase/chat/controller/ConversationController.java`
- Domain/mapper: `knowbase-service/src/main/java/com/knowbase/chat/domain/`, `mapper/`
- Migration if schema change: `infra/postgres/migrate-{name}.sql`
- Frontend: `frontend/knowbase-ui/src/api/conversation.js`, `views/QaView.vue`

**New frontend page:**
- View: `frontend/knowbase-ui/src/views/{Name}View.vue`
- Route: register in `frontend/knowbase-ui/src/router/index.js`
- Nav link: add `el-menu-item` in `frontend/knowbase-ui/src/App.vue` if top-level
- Shared UI: `frontend/knowbase-ui/src/components/`
- Reusable state: `frontend/knowbase-ui/src/composables/`

**Cross-domain coordination (ingest affects vector):**
- Event record: `knowbase-service/src/main/java/com/knowbase/event/`
- Coordinator: `knowbase-service/src/main/java/com/knowbase/platform/DocumentIndexCoordinator.java`

**Database schema change:**
- Update `infra/postgres/init.sql` for fresh installs
- Add `infra/postgres/migrate-{feature}.sql` for existing deployments

**Utilities:**
- Backend shared JSON/helpers: `knowbase-service/src/main/java/com/knowbase/platform/`
- Backend ingest text helpers: `knowbase-service/src/main/java/com/knowbase/ingest/support/`
- Frontend helpers: `frontend/knowbase-ui/src/utils/`

## Special Directories

**`knowbase-service/src/main/java/com/knowbase/orchestration/`:**
- Purpose: Legacy placeholder; no source files
- Committed: Empty directory may exist; safe to ignore or remove in cleanup phases

**`infra/tesseract/tessdata/`:**
- Purpose: Local Tesseract language data for OCR (`.gitkeep` only in repo)
- Source: Downloaded via `scripts/setup-tesseract.ps1` / `setup-tesseract.sh`
- Committed: `.gitkeep` yes; tessdata binaries no

**`frontend/knowbase-ui/node_modules/`:**
- Purpose: npm dependencies
- Generated: Yes (`npm install`)
- Committed: No (gitignored)

**`knowbase-service/target/`:**
- Purpose: Maven build output
- Generated: Yes
- Committed: No

**`.planning/codebase/`:**
- Purpose: GSD codebase intelligence documents (this file, ARCHITECTURE.md, etc.)
- Committed: Yes

---

*Structure analysis: 2026-06-10*
*Update when directory structure changes*
