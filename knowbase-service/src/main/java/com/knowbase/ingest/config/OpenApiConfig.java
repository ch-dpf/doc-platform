package com.knowbase.ingest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI knowbaseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("知库 API")
                        .description("""
                                企业知识库：文档采集入库、向量索引与 RAG 智能问答。

                                **知识库配置（分节 API）**
                                - 详情/创建响应使用 `libraryConfig`（非内部 `config_json` 全量）
                                - 更新按 Tab 解耦：`/basic`、`/index-pipeline`、`/retrieval`

                                **系统级接入策略（application.yml → ingest）**
                                - `allowed-mime-types` / `supportedFileTypes`（upload-constraints）
                                - `version-policy` 版本与重复
                                - `ingest-review-mode` 入库审核
                                """)
                        .version("1.0.0"))
                .tags(List.of(
                        new Tag()
                                .name("知识库管理")
                                .description("""
                                        知识库 CRUD 与分节配置。

                                        | 分节 | 端点 | 说明 |
                                        |------|------|------|
                                        | 基本信息 | PUT `/{id}/basic` | 名称、描述、标签 |
                                        | 索引管道 | PUT `/{id}/index-pipeline` | 解析/清洗/分块/向量化；有分块时锁定 |
                                        | 检索 | PUT `/{id}/retrieval` | 混合检索、重排序、相似度阈值 |

                                        版本策略、入库审核、MIME 白名单见 `ingest.*` 系统配置与 `upload-constraints` API。
                                        """)));
    }
}
