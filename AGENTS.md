# AGENTS.md

## Cursor Cloud specific instructions

KnowBase is a RAG platform: a Maven multi-module Java 21 / Spring Boot 3.2 backend (`knowbase-app` is the runnable app) plus a Vue 3 + Vite admin console (`frontend/knowbase-ui`). See `README.md` for the product overview; see `docs/MODULES.md` for module map and `docs/PROJECT_STATUS.md` for doc/code sync.

### Environment already provisioned (persisted in the VM snapshot)
- **Maven 3.9.9** at `/opt/apache-maven-3.9.9` (symlinked to `mvn`). The apt package `maven` is 3.8.7 and CANNOT parse `.mvn/maven.config` (it contains a `#` comment, only supported by Maven 3.9+), so always use the 3.9.x install.
- **JDK 21**, **Node 22 / npm 10** are preinstalled.
- **PostgreSQL 16 + pgvector** installed natively. The cluster is configured to listen on **port 5433** (matching `application.yml`), with role/db `knowbase`/`knowbase` (password `knowbase`, superuser).
- Maven local repo is kept in-repo at `.m2/repository` (gitignored); always pass `-Dmaven.repo.local=.m2/repository`.

### Starting services (the update script does NOT start anything)
- **PostgreSQL** (required for the default config): `sudo pg_ctlcluster 16 main start` **or** `docker compose up -d postgres` (maps host **5433**). Must be running before the backend starts; the backend runs Flyway migrations on boot.
- **Backend**: `java -jar knowbase-app/target/knowbase-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev` (default port **8088** in `application.yml`; run `mvn -q -Dmaven.repo.local=.m2/repository -DskipTests package` first if the jar is missing). Serves `/api/v1`, Swagger at `/swagger-ui.html`, Knife4j at `/doc.html`.
- **Frontend**: `npm --prefix frontend/knowbase-ui run dev` → `http://localhost:5173` (Vite proxies `/api` to `http://127.0.0.1:8088` via `.env.development`).

On **Windows**, prefer `.\scripts\start-infra.ps1` then `.\scripts\start-app.ps1 -Profile dev` — see `docs/DEV_SETUP.md`.

### Key non-obvious gotchas
- **Ollama is optional for CI/smoke tests.** Default `application.yml` uses **heuristic reading-order** and **local filesystem storage** so the app boots without MinIO. Pass `--knowbase.ollama.enabled=false` only if you need fully deterministic embedding without any Ollama attempt. For ML layout/reading-order: `.\scripts\pull-ollama-layout-models.ps1` after starting Ollama.
- **MinIO is not required by default.** Storage defaults to `knowbase.storage.type=local` (`./data/knowbase-storage`); use MinIO only when `type=minio` and compose MinIO is up.
- **Backend default port is 8088** (`application.yml`). Host **8080** is often occupied by other services (e.g. PaddleOCR HPS); align IDEA Run Configuration and Vite proxy with 8088.
- **Docker postgres container name** is fixed (`knowbase-postgres`). If `docker compose up` fails with name conflict, `docker rm knowbase-postgres` then retry.
- The helper scripts in `scripts/` (`start-app.ps1`, `verify-postgres-rag.ps1`, etc.) are **PowerShell / Windows-only**. On Linux, run the equivalent `mvn` / `npm` / `curl` commands directly. `mvnw` is Windows-only (`mvnw.cmd`); use the installed `mvn`.
- There is **no backend lint config** (standard Maven compile only) and the frontend has **no lint/test scripts** (only `dev`, `build`, `preview`).

### Tests
- Backend unit tests: `mvn -Dmaven.repo.local=.m2/repository test` (no external services required; ~30 tests across `knowbase-ingestion` / `knowbase-retrieval`).

### Ingestion debug logs
- Ingestion/prepare pipelines emit **Chinese structured SLF4J logs** (`入库任务开始`, `文档加载开始`, `向量化完成`, etc.). Filter by `runId=` or `sourceUri=` when tailing backend stdout. Full message catalog: `docs/INGESTION_INTERFACES.md` §结构化日志.
