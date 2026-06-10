---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
---

# External Integrations

**Analysis Date:** 2026-06-10

## APIs & External Services

**LLM / Embedding (Ollama):**
- Ollama — Local embedding and chat inference for vector indexing and RAG
  - SDK/Client: Spring `WebClient` bean in `com.knowbase.config.PlatformConfig` (`ollamaWebClient`)
  - Implementation: `com.knowbase.vector.client.OllamaEmbeddingClient`, `com.knowbase.vector.client.OllamaChatClient`
  - Base URL: `ollama.base-url` in `knowbase-service/src/main/resources/application.yml` (default `http://localhost:11434`)
  - Endpoints used:
    - `GET /api/tags` — Startup health probe (`StartupValidation`, embedding/chat clients)
    - `POST /api/embed` — Batch/single embeddings (`OllamaEmbeddingClient`)
    - `POST /api/chat` — RAG chat completion, streaming via SSE (`OllamaChatClient`, `ConversationController`)
  - Models: `nomic-embed-text` (768-dim embeddings), `llama3.2` (default chat); per-library overrides via `config_json` / request `chatModel`
  - Auth: None (local Ollama, no API key)
  - Docker: `ollama/ollama:latest` in `docker-compose.yml`; models pulled by `scripts/start-infra.ps1`

**Object Storage (MinIO):**
- MinIO — Document originals and parsed text objects
  - SDK/Client: `io.minio.MinioClient` configured in `com.knowbase.ingest.config.MinioConfig`
  - Implementation: `com.knowbase.ingest.storage.MinioDocumentObjectStorage`
  - Auth: `minio.access-key`, `minio.secret-key` in `application.yml` (default `minioadmin` / `minioadmin`)
  - Endpoint: `minio.endpoint` (default `http://localhost:9000`); console port 9001
  - Bucket: `documents` (auto-created on startup in `MinioConfig.ensureBucket`)
  - Presigned URLs: `minio.presign-expiry-minutes` (default 30)
  - Docker: `minio/minio:latest` in `docker-compose.yml`

**Alternative Storage (Local Filesystem):**
- Local FS — Drop-in replacement when `storage.type=local-fs`
  - Implementation: `com.knowbase.ingest.storage.LocalFsDocumentObjectStorage`
  - Path: `storage.local.base-path` (default `./data/documents`)
  - No external service; files stored on server disk

**Document Parsing (Apache Tika):**
- Apache Tika 2.9.2 — In-process text extraction from PDF, Office, plain text, markdown
  - Usage: `com.knowbase.ingest.service.DocumentParseService`, `ParsePreviewService`
  - Auth: None (local library)
  - Supported types: `ingest.allowed-mime-types` in `application.yml`

**OCR (Tesseract via Tess4J):**
- Tesseract — OCR fallback for scanned PDFs and images when library parsing rules enable OCR
  - SDK/Client: `net.sourceforge.tess4j.Tesseract` in `com.knowbase.ingest.service.DocumentOcrService`
  - Data path: `ingest.ocr.data-path` or `KNOWBASE_TESSDATA` env var → `infra/tesseract/tessdata/`
  - Language packs: `chi_sim+eng` (downloaded via `scripts/setup-tesseract.ps1`, not committed)
  - Auth: None (local native library + tessdata files)
  - Global switch: `ingest.ocr.enabled` in `application.yml`

**Frontend → Backend API:**
- knowbase-service REST API — All UI operations
  - Client: `axios` in `frontend/knowbase-ui/src/api/client.js`
  - Dev proxy: Vite proxies `/api`, `/doc.html`, `/v3/api-docs`, `/webjars` to backend (`frontend/knowbase-ui/vite.config.js`)
  - Base URL resolution: `frontend/knowbase-ui/src/config.js` (`VITE_API_BASE`, `VITE_BACKEND_URL`, or same-origin proxy in dev)
  - Auth: None (open API; `tenantId` passed as query/body parameter from `localStorage`)

**URL Collection (documented, endpoint not in current controller):**
- README documents `POST /api/v1/documents/collect` for URL crawl ingestion
- Current `DocumentController` exposes upload/batch/async only; `SourceType.CRAWL` exists in schema (`infra/postgres/init.sql`) but no active collect handler detected in `knowbase-service/src/main/java/com/knowbase/ingest/controller/`

## Data Storage

**Databases:**
- PostgreSQL 16 + pgvector — Primary relational and vector store
  - Image: `pgvector/pgvector:pg16` in `docker-compose.yml`
  - Connection: `spring.datasource.url` in `application.yml` (`jdbc:postgresql://localhost:5432/knowbase`)
  - Credentials: `spring.datasource.username` / `password` (default `knowbase` / `knowbase`)
  - Client: MyBatis-Plus mappers in `com.knowbase.ingest.mapper`, `com.knowbase.vector.mapper`, `com.knowbase.library.mapper`, `com.knowbase.chat.mapper`
  - Vector type: `com.pgvector.PGvector` registered at startup (`PlatformConfig.pgvectorTypeRegistration`)
  - Schema init: `infra/postgres/init.sql` mounted into Docker `docker-entrypoint-initdb.d`
  - Migrations: `infra/postgres/migrate-*.sql` (manual apply)
  - Reset: `scripts/reset-db.ps1`
  - Key tables: `vector_library`, `doc_metadata`, `document_chunk`, `upload_task`, `chat_conversation`, `chat_message`, `processed_event`, `document_index_job`

**File Storage:**
- MinIO bucket `documents` (default) or local `./data/documents`
  - Stores: uploaded originals, parsed `.txt` artifacts
  - Read path: `DocumentObjectStorage` interface; index pipeline reads via `ParsedTextFetcher` and `ParsedTextObjectStore`

**Caching:**
- In-memory RAG retrieval cache — `retrieval.cache-enabled` / `retrieval.cache-ttl-seconds` in `application.yml`
  - Implementation: `com.knowbase.vector.retrieval.RagRetrievalCache` (JVM-local, not Redis)
- No external cache service detected

## Authentication & Identity

**Auth Provider:**
- None — No `spring-boot-starter-security`, JWT, OAuth, or session middleware detected
  - APIs are open on port 8080
  - Multi-tenancy: `tenantId` string passed by frontend (`frontend/knowbase-ui/src/composables/useLibraryContext.js` → `localStorage`)
  - No server-side tenant authentication or authorization enforcement detected

**OAuth Integrations:**
- Not applicable

## Monitoring & Observability

**Error Tracking:**
- None — No Sentry, Datadog, or similar integration detected

**Analytics:**
- None detected

**Logs:**
- SLF4J / Logback (Spring Boot default) — stdout from `knowbase-service`
  - Startup validation warnings for Ollama/OCR availability (`StartupValidation`, `OllamaEmbeddingClient`, `DocumentOcrService`)
  - No centralized log aggregation configured

**Health Checks:**
- Spring Boot Actuator — `management.endpoints.web.exposure.include: health,info` in `application.yml`
- Docker healthcheck on Postgres in `docker-compose.yml` (`pg_isready`)
- Infra probe script: `scripts/infra-check.ps1`

## CI/CD & Deployment

**Hosting:**
- Local / manual deployment — JAR + Docker Compose infra
  - Backend: `scripts/start-services.ps1` launches JAR on 8080
  - Frontend: `npm run dev` (dev) or `npm run build` + static host (prod)
  - No Kubernetes, ECS, or PaaS manifests detected

**CI Pipeline:**
- Not detected — No `.github/workflows/` or other CI config in repository
- Manual E2E: `scripts/e2e-test.ps1`
- Git sync notes: `scripts/github-sync.md` (documentation only)

## Environment Configuration

**Development:**
- Required services: PostgreSQL (5432), MinIO (9000/9001), Ollama (11434)
- Backend config: `knowbase-service/src/main/resources/application.yml` (credentials inline for local dev)
- Frontend secrets: `.env` file present (gitignored); template `frontend/knowbase-ui/.env.example`
- Optional env vars:
  - `KNOWBASE_TESSDATA` — OCR language pack directory
  - `VITE_DEV_PROXY_TARGET` — Frontend dev proxy target
  - `MAVEN_HOME` — Maven location for `scripts/build.ps1`
- Mock/stub services: Use Docker Compose defaults; Ollama test models pulled on first `start-infra.ps1`

**Staging:**
- Not defined — No staging-specific profiles or env files detected

**Production:**
- Secrets management: Not formalized; override `application.yml` or use external config (Spring Boot env binding supported for all `@ConfigurationProperties` prefixes)
- Recommended overrides: datasource URL/credentials, `minio.*`, `ollama.base-url`, `storage.type`
- Database: Apply `infra/postgres/init.sql` on fresh DB; use migration scripts for upgrades

## Webhooks & Callbacks

**Incoming:**
- None — No webhook endpoints detected

**Outgoing:**
- Ollama HTTP — Embedding and chat requests from `OllamaEmbeddingClient` / `OllamaChatClient`
- MinIO S3 API — Put/get/presign from `MinioDocumentObjectStorage`
- HTTP GET fallback — `ParsedTextFetcher.fetchViaHttp` for legacy presigned parsed-text URLs (`java.net.http.HttpClient`)
- No external notification webhooks (email, Slack, etc.)

## Internal Coordination (replaces former Kafka)

**In-process event coordination:**
- `com.knowbase.platform.DocumentIndexCoordinator` — Async document indexing after ingest (replaces Kafka `doc.lifecycle.v1` topic)
- Spring `@EnableAsync` on `KnowbaseApplication` — Background parse/index tasks
- Idempotency table: `processed_event` in PostgreSQL (`infra/postgres/init.sql`)

## Port Reference

| Service | Port | Config / compose |
|---------|------|------------------|
| knowbase-service | 8080 | `application.yml` `server.port` |
| knowbase-ui (Vite) | 5173 | `frontend/knowbase-ui/vite.config.js` |
| PostgreSQL | 5432 | `docker-compose.yml` |
| MinIO API / Console | 9000 / 9001 | `docker-compose.yml`, `minio.endpoint` |
| Ollama | 11434 | `docker-compose.yml`, `ollama.base-url` |

---

*Integration audit: 2026-06-10*
*Update when adding/removing external services*
