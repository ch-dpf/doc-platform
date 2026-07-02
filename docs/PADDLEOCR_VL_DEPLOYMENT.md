# PaddleOCR-VL / vLLM 本地部署

KnowBase 通过 `knowbase.vision-document` 接入官方 PaddleOCR-VL 服务，用于扫描件 / 低置信度 PDF 的结构化解析。

## 端口规划

| 服务 | 默认端口 | 说明 |
|------|----------|------|
| KnowBase 后端 | **8088** | `server.port`，REST API |
| PaddleOCR-VL HPS 网关 | **8888** | 官方 compose 默认 8080；本仓库推荐映射到 8888 避免与 KnowBase/其他服务冲突 |
| 独立 vLLM（可选） | 8118 | `infra/docker-compose.paddleocr-vl.yml`，仅 POC |
| Ollama | 11434 | embedding / reading-order |

## KnowBase 配置（application.yml）

生产推荐：**官方 HPS + `provider: paddleocr-vl`**

```yaml
knowbase:
  vision-document:
    enabled: true
    provider: paddleocr-vl   # paddleocr-vl | vllm | ollama
    timeout: 600s
    paddleocr-vl:
      base-url: http://localhost:8888
      layout-parsing-path: /layout-parsing
      pipeline-name: PaddleOCR-VL-1.6
      prettify-markdown: true
      return-markdown-images: false
      visualize: false
    vllm:
      base-url: http://localhost:8118
      model: PaddleOCR-VL-1.6-0.9B
  ollama:
    vision-language-model: ""   # 官方 HPS 启用时留空
  ingestion:
    pdf:
      vl-on-scanned: true
      vl-on-low-confidence: true
      vl-low-confidence-threshold: 0.55
      vl-fallback-to-heuristic: true
    layout:
      default-provider: local-pdf-layout
      ollama:
        enabled: false
```

| provider | 场景 | 端点 | KnowBase 组件 |
|----------|------|------|---------------|
| `paddleocr-vl` | 官方完整版面 pipeline（**推荐**） | `POST /layout-parsing` | `PaddleOcrVlLayoutProvider` → `prunedResult` + bbox |
| `vllm` | 独立 vLLM VLM（快速验证） | `POST /v1/chat/completions` | `VisionMarkdownLayoutProvider` |
| `ollama` | Ollama 社区版 VLM 回退 | Ollama `/api/chat` | `VisionMarkdownLayoutProvider` |

`vision-document.enabled=true` 时优先于 `knowbase.ollama.vision-language-model`。

## 端到端集成流程（官方 HPS → KnowBase）

```text
1. 部署官方 HPS（宿主机 8888）
2. verify-paddleocr-hps.ps1 确认 /health/ready
3. application.yml: provider=paddleocr-vl, base-url=http://localhost:8888
4. 重启 KnowBase 后端
5. 重新入库扫描 PDF，检查 metadata: layoutProvider=paddleocr-vl
```

KnowBase 对每页 PNG 调用 `/layout-parsing`，优先读取 `prunedResult.parsing_res_list`（含 bbox / 阅读顺序 / 表格区域），再映射为 `StructuralBlock`。

## Docker 部署

### 方式 B：官方完整 PaddleOCR-VL HPS（生产推荐，RTX 30/40 系列）

需 NVIDIA GPU（Compute Capability 8.0–9.x，CUDA 12.6+）：

```bash
git clone https://github.com/PaddlePaddle/PaddleOCR.git
cd PaddleOCR/deploy/paddleocr_vl_docker/hps
cp .env.example .env
# 可选：修改 HPS_PIPELINE_NAME、HPS_DEVICE_ID
bash prepare.sh
```

Windows 若 WSL/bash 不可用，用仓库 PowerShell 脚本替代（无需 bash）：

```powershell
.\scripts\prepare-paddleocr-hps.ps1 -HpsDir D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps
.\scripts\fix-paddleocr-hps-entrypoint.ps1 -HpsDir D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps
```

> 注意 clone 路径：常见为 `D:\workspace\PaddleOCR\...`，不是 `D:\PaddleOCR\...`。  
> Windows 下 `genai_server_entrypoint.sh` 必须是 **LF 换行**，否则 `paddleocr-vlm-server` 会 `unhealthy`（`set: Illegal option -`）。

### 方式 B2：RTX 50 系列 / Blackwell（如 RTX 5060 Ti）

官方 HPS 默认镜像 `paddlex3.6-gpu` **不支持 sm120**，pipeline 会报：

`layout-parsing UNAVAILABLE: Unsupported GPU architecture`

请改用 Blackwell 专用 compose（2 容器，同样暴露 `/layout-parsing`）：

```powershell
cd D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\accelerators\nvidia-gpu-sm120
# 将 compose.yaml 中 paddleocr-vl-api.ports 改为 8888:8080（8080 常被 KnowBase 占用）
docker compose up -d
```

健康检查：`GET http://localhost:8888/health`（sm120 **无** `/health/ready`，与三容器 HPS 不同）。

详见 [PaddleOCR-VL NVIDIA Blackwell 教程](https://www.paddleocr.ai/latest/en/version3.x/pipeline_usage/PaddleOCR-VL-NVIDIA-Blackwell.html)。

**将对外端口改为 8888**（官方默认 8080，常与 KnowBase 冲突）：

编辑 `compose.yaml` 中 `paddleocr-vl-api.ports`：

```yaml
  paddleocr-vl-api:
    ports:
      - "8888:8080"    # 宿主机:容器；容器内仍为 8080
```

Windows 可用仓库脚本自动改端口（在 clone 完成后）：

```powershell
.\scripts\patch-paddleocr-hps-port.ps1 -HpsDir D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps -HostPort 8888
```

启动：

```bash
docker compose up -d
```

验证：

```powershell
.\scripts\verify-paddleocr-hps.ps1 -HpsBaseUrl http://localhost:8888
# 带页面图片冒烟测试
.\scripts\verify-paddleocr-hps.ps1 -HpsBaseUrl http://localhost:8888 -ImagePath D:\document\page1.png
```

- 网关：`http://localhost:8888`
- 就绪检查：`curl http://localhost:8888/health/ready`
- KnowBase：`provider: paddleocr-vl`，`base-url: http://localhost:8888`

排障：

```bash
docker compose logs paddleocr-vl-api
docker compose logs paddleocr-vl-pipeline
docker compose logs paddleocr-vlm-server
```

### 方式 A：独立 vLLM（最快验证，质量低于 HPS）

```bash
docker compose -f infra/docker-compose.paddleocr-vl.yml up -d
```

- 监听：`http://localhost:8118`
- 配置：`provider: vllm`

健康检查：

```bash
curl http://localhost:8118/health
curl http://localhost:8118/v1/models
```

> 与 HPS 同时运行会争抢 GPU；切到 HPS 后建议停止 8118 容器。

### 方式 C：Ollama 社区版（无 GPU / 开发）

```yaml
knowbase:
  vision-document:
    enabled: false
  ollama:
    enabled: true
    vision-language-model: MedAIBase/PaddleOCR-VL:0.9b
```

```bash
ollama pull MedAIBase/PaddleOCR-VL:0.9b
```

## 路由行为

`PdfLayoutParser` 在以下情况走 VLM / HPS：

1. 扫描件 PDF（`vl-on-scanned`）
2. 文档 metadata `pdfParseMode=vl|vision|paddleocr-vl`
3. layout 置信度低于阈值（`vl-on-low-confidence`）

失败且 `vl-fallback-to-heuristic=true` 时回退 Tesseract / 启发式 layout。

成功入库后 metadata 示例：

- `layoutProvider`: `paddleocr-vl`
- `layoutAnalysisRoute`: `paddleocr-vl-pruned`
- `pdfParseRoute`: `vision-vl`
- `bboxSource`: `paddle-layout`

## 相关文档

- [BUILTIN_DEEP_PARSE.md](./BUILTIN_DEEP_PARSE.md)
- [PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md) §4.2
- [PaddleOCR-VL 官方 HPS README](https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/paddleocr_vl_docker/hps/README.md)
