# 运维脚本说明

所有脚本默认在项目根目录 `D:\workspace\doc-platform` 下执行（除特别说明外）。

| 脚本 | 用途 |
|------|------|
| `build.ps1` | Maven 编译打包（默认 `clean package -DskipTests`） |
| `start-infra.ps1` | **Docker Compose** 启动 Postgres / Kafka / MinIO / Ollama |
| `infra-check.ps1` | **本机安装** 时检查五类基础设施是否就绪 |
| `start-services.ps1` | 新窗口启动两个 Java 服务（需先构建且基础设施已就绪） |
| `e2e-test.ps1` | 端到端冒烟：上传 → 等待索引 → 检索 → 删除 |

## 常用命令

```powershell
# 编译（含测试）
.\scripts\build.ps1 -Test

# 仅 Docker 基础设施
.\scripts\start-infra.ps1

# 本机基础设施检查
.\scripts\infra-check.ps1

# 启动 Java 服务
.\scripts\start-services.ps1

# 联调验证
.\scripts\e2e-test.ps1
```

## 数据库维护 SQL

位于 `infra/postgres/`：

| 文件 | 说明 |
|------|------|
| `init.sql` | 首次初始化（Docker 挂载或手动执行） |
| `migrate-source-url.sql` | 已有库增加 `source_url` 列与索引（一次性） |
| `reset-vector-idempotency.sql` | 清空向量服务幂等表，用于索引失败后的重试 |
