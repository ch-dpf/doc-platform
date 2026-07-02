# 本地开发一键指南

面向 **Windows + Docker Desktop** 与 **IDEA 本地调试**。Linux/macOS 将 `.\scripts\*.ps1` 换为等价命令即可。

## 前置

| 依赖 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 后端 |
| Maven | 3.9+ | 构建（需解析 `.mvn/maven.config`） |
| Node | 18+ | 前端 |
| Docker Desktop | 最新 | PostgreSQL（必选）、MinIO/Ollama（可选） |

## 最快路径（推荐）

```powershell
cd D:\workspace\doc-platform

# 1. 基础设施（PostgreSQL 5433；可选 minio / ollama）
.\scripts\start-infra.ps1

# 2. 构建并启动后端（dev profile：本地存储、启发式阅读顺序）
.\scripts\start-app.ps1 -Profile dev -SkipPackage   # 首次去掉 -SkipPackage

# 3. 另一终端：前端
.\scripts\start-ui.ps1
```

访问：

| 入口 | URL |
|------|-----|
| 控制台 | http://localhost:5173 |
| API | http://localhost:8080/api/v1 |
| Knife4j | http://localhost:8080/doc.html |

## 一键脚本

| 脚本 | 作用 |
|------|------|
| `start-infra.ps1` | `docker compose up -d postgres`（+ `-WithMinio` / `-WithOllama`）并等待健康 |
| `start-app.ps1` | `mvn package` + 启动 JAR（`-Profile dev`、`-Port 8080`） |
| `start-dev.ps1` | infra + app（不含 UI） |
| `verify-dev-health.ps1` | 检查 5433 / 8080 / 5173 等端口 |
| `verify-postgres-rag.ps1` | 端到端 RAG 冒烟 |

## 配置说明

| 文件 | 用途 |
|------|------|
| `application.yml` | 提交默认：8080、local 存储、启发式 reading-order、VLM 默认关 |
| `application-dev.yml` | `spring.profiles.active=dev` 时加载；与 compose 本地服务对齐 |
| `frontend/knowbase-ui/.env.development` | Vite 代理 → `http://127.0.0.1:8080` |

### PostgreSQL

- **Docker（推荐）**：`docker compose up -d postgres` → `localhost:5433`
- **本机 PG 5432**：启动参数覆盖  
  `--spring.datasource.url=jdbc:postgresql://localhost:5432/knowbase`

若容器名冲突或网络损坏：

```powershell
docker rm -f knowbase-postgres
docker compose up -d postgres
```

### Ollama（可选，Embedding/Chat）

```powershell
.\scripts\start-infra.ps1 -WithOllama
.\scripts\pull-ollama-layout-models.ps1   # 可选：版面/阅读顺序 ML
```

无 Ollama 时：解析回退 PDFBox 启发式；Embedding 需 Ollama 或改配置。

### MinIO（可选）

```powershell
.\scripts\start-infra.ps1 -WithMinio
```

在 `application-dev.yml` 中取消 MinIO 配置注释，并设置 `type: minio`。

### PaddleOCR-VL（可选，扫描 PDF）

见 [PADDLEOCR_VL_DEPLOYMENT.md](PADDLEOCR_VL_DEPLOYMENT.md)。启用后在 `application.yml` 设 `knowbase.vision-document.enabled: true`。

## IDEA 启动

1. 先运行 `.\scripts\start-infra.ps1`
2. Run Configuration → Active profiles: `dev`（或 VM options: `--spring.profiles.active=dev`）
3. Main class: `com.knowbase.app.KnowbaseAppApplication`

## 冒烟验证

```powershell
.\scripts\verify-dev-health.ps1
.\scripts\verify-postgres-rag.ps1 -BaseUrl http://localhost:8080
```

## 相关文档

- [模块划分](MODULES.md)
- [实现进度](PROJECT_STATUS.md)
- [README 产品概览](../README.md)
