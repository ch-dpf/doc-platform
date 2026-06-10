---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
---

# Technology Stack

**Analysis Date:** 2026-06-10

## Languages

**Primary:**
- Java 21 — All backend application code in `knowbase-service/src/main/java/com/knowbase/`
- JavaScript (ES modules) — Vue 3 frontend in `frontend/knowbase-ui/src/`

**Secondary:**
- SQL — PostgreSQL schema and migrations in `infra/postgres/` (`init.sql`, `migrate-*.sql`)
- PowerShell — Build, infra, and ops scripts in `scripts/*.ps1`
- Bash — OCR setup script `scripts/setup-tesseract.sh`
- YAML — Backend configuration in `knowbase-service/src/main/resources/application.yml`
- HTML — Frontend entry `frontend/knowbase-ui/index.html`

## Runtime

**Environment:**
- JVM 21 — Spring Boot monolith (`knowbase-service`), entry `com.knowbase.KnowbaseApplication`
- Node.js — Required for Vite dev server and frontend build (not pinned in `package.json`; Vite 6.x typically needs Node 18+)
- Docker — Local infrastructure via `docker-compose.yml` (PostgreSQL, MinIO, Ollama)
- Browser — Vue SPA served by Vite on port 5173

**Package Manager:**
- Maven 3.9.x — Root aggregator `pom.xml`, module `knowbase-service/pom.xml`; build via `scripts/build.ps1` (defaults to `D:\software\maven\apache-maven-3.9.11` or `$env:MAVEN_HOME`)
- npm — Frontend in `frontend/knowbase-ui/`; lockfile `frontend/knowbase-ui/package-lock.json` present

## Frameworks

**Core:**
- Spring Boot 3.2.4 — Monolithic REST API server on port 8080 (`knowbase-service/pom.xml`, `application.yml`)
- MyBatis-Plus 3.5.7 — PostgreSQL persistence; mappers under `com.knowbase.*.mapper`
- Vue 3.5.x — SPA framework (`frontend/knowbase-ui/package.json`)
- Element Plus 2.9.x — UI component library (`frontend/knowbase-ui/package.json`)
- Vue Router 4.5.x — Client routing (`frontend/knowbase-ui/src/router/index.js`)

**Testing:**
- JUnit 5 + Spring Boot Test — Via `spring-boot-starter-test` in `knowbase-service/pom.xml`
- Test location: `knowbase-service/src/test/java/com/knowbase/` (ingest, vector, library, chat packages)
- Run via Maven: `scripts/build.ps1 -Test` or `mvn test` from repo root
- E2E smoke: `scripts/e2e-test.ps1` (HTTP checks against localhost:8080)
- Frontend unit/E2E tests: Not detected

**Build/Dev:**
- Vite 6.0.x — Frontend bundler and dev server (`frontend/knowbase-ui/vite.config.js`)
- `@vitejs/plugin-vue` 5.2.x — Vue SFC support
- Spring Boot Maven Plugin — Repackages `knowbase-service-1.0.0-SNAPSHOT.jar`
- Knife4j 4.4.0 — OpenAPI/Swagger UI at `/doc.html` (`knowbase-service/pom.xml`)
- springdoc-openapi — API docs paths in `application.yml` (`/v3/api-docs`, `/swagger-ui.html`)

## Key Dependencies

**Critical:**
- `spring-boot-starter-web` — REST controllers (`DocumentController`, `RagController`, `VectorLibraryController`, etc.)
- `spring-boot-starter-webflux` — Reactive `WebClient` for Ollama HTTP calls (`com.knowbase.config.PlatformConfig`, `OllamaEmbeddingClient`, `OllamaChatClient`)
- `mybatis-plus-spring-boot3-starter` — ORM for PostgreSQL entities and mappers
- `postgresql` + `pgvector` 0.1.6 — Vector storage; type handler `com.knowbase.vector.mybatis.PGvectorTypeHandler`
- `minio` 8.5.12 — S3-compatible object storage for documents and parsed text (`MinioDocumentObjectStorage`)
- `tika-parsers-standard-package` 2.9.2 — Multi-format document parsing (`DocumentParseService`)
- `tess4j` 5.11.0 — In-process Tesseract OCR (`DocumentOcrService`)
- `pdfbox` 2.0.31 — PDF rendering for OCR fallback (pinned to Tika 2.x API)
- `axios` 1.7.x — Frontend HTTP client (`frontend/knowbase-ui/src/api/client.js`)

**Infrastructure:**
- `spring-boot-starter-actuator` — Health/info endpoints (`management.endpoints.web.exposure` in `application.yml`)
- `spring-boot-starter-validation` — Jakarta Bean Validation on DTOs
- `jackson-bom` 2.17.2 — JSON serialization (managed in root `pom.xml`)
- `knife4j-openapi3-jakarta-spring-boot-starter` — API documentation UI

## Configuration

**Environment:**
- Backend: `knowbase-service/src/main/resources/application.yml` (primary; no profile-specific files detected)
- Frontend dev: Vite env vars loaded from `frontend/knowbase-ui/.env` (gitignored); template in `frontend/knowbase-ui/.env.example`
- OCR tessdata path: `KNOWBASE_TESSDATA` env var overrides `ingest.ocr.data-path` default `./infra/tesseract/tessdata`
- Maven home: `$env:MAVEN_HOME` or hardcoded path in `scripts/build.ps1`

**Key backend config sections (`application.yml`):**
- `spring.datasource` — PostgreSQL JDBC URL, credentials, Hikari UTF-8 init
- `storage.type` — `minio` (default) or `local-fs`
- `minio.*` — Endpoint, credentials, bucket `documents`
- `ingest.*` — Upload limits, OCR, text normalization, allowed MIME types
- `embedding.*` / `ollama.*` — Embedding provider and LLM settings
- `chunking.*` / `rag.*` / `retrieval.*` / `chat.*` — Indexing and RAG behavior

**Frontend env vars (`.env.example`):**
- `VITE_DEV_PROXY_TARGET` — Dev proxy backend (default `http://127.0.0.1:8080`)
- `VITE_API_BASE` / `VITE_BACKEND_URL` — Production build API base (`frontend/knowbase-ui/src/config.js`)

**Build:**
- `pom.xml` — Parent POM: Java 21, Spring Boot 3.2.4, dependency versions
- `knowbase-service/pom.xml` — Service module dependencies and repackage plugin
- `frontend/knowbase-ui/vite.config.js` — Dev server port 5173, `/api` proxy to backend
- `docker-compose.yml` — Postgres pgvector, MinIO, Ollama service definitions

## Platform Requirements

**Development:**
- JDK 21 — Required for `knowbase-service` compilation and runtime
- Maven 3.9+ — Backend build (`scripts/build.ps1`)
- Node.js + npm — Frontend (`cd frontend/knowbase-ui && npm install && npm run dev`)
- Docker Desktop (or Docker Engine) — `scripts/start-infra.ps1` runs `docker compose up -d`
- Windows: Visual C++ Redistributable for Tess4J OCR (`infra/tesseract/README.md`)
- OCR language packs: `scripts/setup-tesseract.ps1` downloads to `infra/tesseract/tessdata/` (gitignored)

**Production:**
- Deployment target: Not formalized (no CI/CD or containerized app image detected)
- Typical layout: JAR on port 8080 (`knowbase-service/target/knowbase-service-1.0.0-SNAPSHOT.jar`), static frontend build from `npm run build`, Docker Compose or managed equivalents for Postgres/MinIO/Ollama
- JVM flags: `-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8` recommended on Windows (`scripts/start-services.ps1`)
- Ollama models required: `nomic-embed-text` (embedding), `llama3.2` (chat) — pulled by `scripts/start-infra.ps1`

## Module Layout

| Path | Stack role |
|------|------------|
| `pom.xml` | Maven parent aggregator |
| `knowbase-service/` | Spring Boot monolith (ingest + vector + library + chat) |
| `frontend/knowbase-ui/` | Vue 3 + Vite SPA |
| `infra/postgres/` | PostgreSQL init and migration SQL |
| `infra/tesseract/` | OCR tessdata directory (runtime, not committed) |
| `scripts/` | PowerShell build/infra/start/E2E automation |
| `docker-compose.yml` | Local Postgres + MinIO + Ollama |

**Removed / not present:** Kafka, separate `doc-ingest-service` / `vector-index-service` modules (merged into `knowbase-service` per `README.md`)

---

*Stack analysis: 2026-06-10*
*Update after major dependency changes*
