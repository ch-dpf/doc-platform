# AGENTS.md

## Cursor Cloud specific instructions

KnowBase is a RAG platform: a Maven multi-module Java 21 / Spring Boot 3.2 backend (`knowbase-app` is the runnable app) plus a Vue 3 + Vite admin console (`frontend/knowbase-ui`). See `README.md` for the product overview and module map.

### Environment already provisioned (persisted in the VM snapshot)
- **Maven 3.9.9** at `/opt/apache-maven-3.9.9` (symlinked to `mvn`). The apt package `maven` is 3.8.7 and CANNOT parse `.mvn/maven.config` (it contains a `#` comment, only supported by Maven 3.9+), so always use the 3.9.x install.
- **JDK 21**, **Node 22 / npm 10** are preinstalled.
- **PostgreSQL 16 + pgvector** installed natively. The cluster is configured to listen on **port 5433** (matching `application.yml`), with role/db `knowbase`/`knowbase` (password `knowbase`, superuser).
- Maven local repo is kept in-repo at `.m2/repository` (gitignored); always pass `-Dmaven.repo.local=.m2/repository`.

### Starting services (the update script does NOT start anything)
- **PostgreSQL** (required for the default config): `sudo pg_ctlcluster 16 main start`. It must be running before the backend starts; the backend runs Flyway migrations on boot.
- **Backend**: `java -jar knowbase-app/target/knowbase-app-1.0.0-SNAPSHOT.jar --server.port=8080 --knowbase.ollama.enabled=false` (run `mvn -q -Dmaven.repo.local=.m2/repository -DskipTests package` first if the jar is missing). Serves `/api/v1`, Swagger at `/swagger-ui.html`, Knife4j at `/doc.html`.
- **Frontend**: `npm --prefix frontend/knowbase-ui run dev` → `http://localhost:5173` (Vite proxies `/api` to `http://127.0.0.1:8080`).

### Key non-obvious gotchas
- **Ollama is optional for CI/smoke tests.** Default ingestion uses **ML layout** (`layout.default-provider=ollama-layout`) and **Ollama reading-order** (`reading-order.provider=ollama`); both fall back to PDFBox heuristics when Ollama is unreachable. Pass `--knowbase.ollama.enabled=false` for Deterministic embedding/chat. For fully heuristic parsing without ML attempts, also pass `--knowbase.ingestion.layout.ollama.enabled=false --knowbase.ingestion.reading-order.provider=heuristic`. Before first run with ML defaults: `.\scripts\pull-ollama-layout-models.ps1` (creates `knowbase-reading-order`, pulls `llama3.2-vision`).
- **MinIO is not needed.** Storage defaults to local filesystem (`knowbase.storage.type=local`); MinIO is only used when `type=minio`.
- The helper scripts in `scripts/` (`start-app.ps1`, `verify-postgres-rag.ps1`, etc.) are **PowerShell / Windows-only**. On Linux, run the equivalent `mvn` / `npm` / `curl` commands directly. `mvnw` is Windows-only (`mvnw.cmd`); use the installed `mvn`.
- There is **no backend lint config** (standard Maven compile only) and the frontend has **no lint/test scripts** (only `dev`, `build`, `preview`).

### Tests
- Backend unit tests: `mvn -Dmaven.repo.local=.m2/repository test` (no external services required; ~30 tests across `knowbase-ingestion` / `knowbase-retrieval`).

### Ingestion debug logs
- Ingestion/prepare pipelines emit **Chinese structured SLF4J logs** (`入库任务开始`, `文档加载开始`, `向量化完成`, etc.). Filter by `runId=` or `sourceUri=` when tailing backend stdout. Full message catalog: `docs/INGESTION_INTERFACES.md` §结构化日志.
