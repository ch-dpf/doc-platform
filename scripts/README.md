# 运维脚本说明

| 脚本 | 用途 |
|------|------|
| `build.ps1` | Maven 打包 `knowbase-service` |
| `start-infra.ps1` | Docker：Postgres / MinIO / Ollama（无 Kafka） |
| `reset-db.ps1` | **重置数据库**：单 schema `public` 删表并重建（或重建 Postgres 容器） |
| `infra-check.ps1` | 本机检查 Postgres、MinIO、Ollama、OCR tessdata |
| `setup-tesseract.ps1` | 下载 OCR 语言包到 `infra/tesseract/tessdata` |
| `start-services.ps1` | 启动 knowbase-app（8010） |
| `e2e-test.ps1` | 端到端冒烟 |

```powershell
.\scripts\build.ps1
.\scripts\start-infra.ps1
.\scripts\start-services.ps1
.\scripts\e2e-test.ps1
```

### 重置数据库（单 schema）

```powershell
# 就地清空 public 表并执行 init.sql（Docker Postgres 已运行时）
.\scripts\reset-db.ps1

# 删除 Postgres 容器后重建（空库自动跑 init.sql）
.\scripts\reset-db.ps1 -RecreateContainer

# 本机安装的 PostgreSQL（非 Docker）
.\scripts\reset-db.ps1 -UseLocalPsql -SkipConfirm
```

GitHub 同步：[github-sync.md](./github-sync.md)

### OCR 语言包（可选）

```powershell
.\scripts\setup-tesseract.ps1
.\scripts\setup-tesseract.ps1 -CheckOnly

# application.yml: ingest.ocr.enabled: true
# 详见 infra/tesseract/README.md
```
