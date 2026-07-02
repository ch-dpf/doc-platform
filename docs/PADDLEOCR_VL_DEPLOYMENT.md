# PaddleOCR-VL / vLLM 本地部署

KnowBase 通过 `knowbase.vision-document` 接入官方 PaddleOCR-VL 服务，用于扫描件 / 低置信度 PDF 的结构化解析。

## 配置（application.yml）

```yaml
knowbase:
  vision-document:
    enabled: true
    provider: paddleocr-vl   # paddleocr-vl | vllm | ollama
    timeout: 600s
    paddleocr-vl:
      base-url: http://localhost:8080
      layout-parsing-path: /layout-parsing
      pipeline-name: PaddleOCR-VL-1.6
    vllm:
      base-url: http://localhost:8118
      model: PaddleOCR-VL-1.6-0.9B
  ollama:
    vision-language-model: ""   # 官方服务启用时留空
  ingestion:
    pdf:
      vl-on-scanned: true
      vl-fallback-to-heuristic: true
```

| provider | 场景 | 端点 |
|----------|------|------|
| `paddleocr-vl` | 官方完整版面 pipeline（推荐） | `POST /layout-parsing` |
| `vllm` | 独立 vLLM VLM 服务 | `POST /v1/chat/completions` |
| `ollama` | Ollama 社区版 VLM 回退 | Ollama `/api/chat` |

`vision-document.enabled=true` 时优先于 `knowbase.ollama.vision-language-model`。

## Docker 部署

### 方式 A：独立 vLLM（最快验证）

```bash
docker compose -f infra/docker-compose.paddleocr-vl.yml up -d
```

- 监听：`http://localhost:8118`
- 配置：`provider: vllm`

健康检查：

```bash
curl http://localhost:8118/health
# 或
curl http://localhost:8118/v1/models
```

### 方式 B：官方完整 PaddleOCR-VL HPS（生产推荐）

需 NVIDIA GPU（Compute Capability 8.0–9.x，CUDA 12.6+）：

```bash
git clone https://github.com/PaddlePaddle/PaddleOCR.git
cd PaddleOCR/deploy/paddleocr_vl_docker/hps
cp .env.example .env
# 可选：修改 HPS_PIPELINE_NAME、HPS_DEVICE_ID
bash prepare.sh
docker compose up -d
```

- 网关：`http://localhost:8080`
- 就绪检查：`curl http://localhost:8080/health/ready`
- 配置：`provider: paddleocr-vl`，`base-url: http://localhost:8080`

KnowBase 对每页 PNG 调用 `/layout-parsing`，读取 `result.layoutParsingResults[0].markdown.text`，再映射为 `StructuralBlock`（含 GFM 表格）。

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

`PdfLayoutParser` 在以下情况走 VLM：

1. 扫描件 PDF（`vl-on-scanned`）
2. 文档 metadata `pdfParseMode=vl|vision|paddleocr-vl`
3. layout 置信度低于阈值（`vl-on-low-confidence`）

失败且 `vl-fallback-to-heuristic=true` 时回退 Tesseract / 启发式 layout。

解析 metadata：`pdfParseRoute=vision-vl`、`visionLanguageModel=<pipeline 或 model 名>`。

## 相关文档

- [BUILTIN_DEEP_PARSE.md](./BUILTIN_DEEP_PARSE.md)
- [PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md) §4.2
- [PaddleOCR-VL 官方文档](https://www.paddleocr.ai/latest/en/version3.x/pipeline_usage/PaddleOCR-VL.html)
