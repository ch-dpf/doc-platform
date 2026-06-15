# Kanhai × KnowBase 集成示例

宿主通过 `knowbase-starter` 嵌入知库；**knowbase-ui 控制台推荐方案 B**（与 kanhai 分离）。

## 模式 B：knowbase-ui 直连 knowbase-app（推荐）

| 进程 | 端口 | 职责 |
|------|------|------|
| kanhai 宿主 | 8080 | 业务 API；`knowbase.web.expose-controllers: false` |
| knowbase-app | 8010 | 知库 REST `/api/v1/*` |
| knowbase-ui | 5173 | 控制台；Vite 代理 `/api` → **8010** |

```powershell
# 知库 API
.\scripts\start-services.ps1

# 前端（已配置 .env → 8010）
cd frontend\knowbase-ui
npm run dev
```

`frontend/knowbase-ui/.env`：

```env
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8010
VITE_BACKEND_URL=http://127.0.0.1:8010
```

kanhai 侧保持：

```yaml
knowbase:
  web:
    expose-controllers: false
```

## 模式 A：knowbase-ui 挂到 kanhai 同一端口

前端请求 `/api/v1/vector-libraries` 等，宿主必须注册知库 REST Controller：

```yaml
knowbase:
  web:
    expose-controllers: true
```

并把 `VITE_DEV_PROXY_TARGET` 改为 `http://127.0.0.1:8080`。

未开启时典型报错：

```
No endpoint GET /api/v1/vector-libraries
PUT /api/v1/vector-libraries/undefined/retrieval
```

## 仅 Facade（无 UI）

由宿主暴露 `/api/knowledge/*`（见 `KnowledgeController`），调用 `KnowbaseRagFacade` 等。

## 租户

实现 `KnowbaseTenantResolver`（示例：`KnowbaseHostConfiguration` + JWT `orgId`）。

## 包结构

复制 `org.shkj.kanhai.knowbase` 到宿主工程，按需调整包名与 `OrgKnowledgeLibrary` 表。
