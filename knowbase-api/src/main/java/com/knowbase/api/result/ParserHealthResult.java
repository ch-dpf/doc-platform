package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "解析器依赖健康状态")
public record ParserHealthResult(
        @Schema(description = "READY | DEGRADED | UNCONFIGURED | UNKNOWN")
        String status,
        @Schema(description = "面向用户的状态说明")
        String message,
        @Schema(description = "是否已配置必要依赖")
        boolean configured,
        @Schema(description = "依赖端点或本地引擎标识")
        String endpoint,
        @Schema(description = "实际探测的 provider / engine")
        String provider,
        @Schema(description = "探测时间")
        Instant checkedAt,
        @Schema(description = "附加详情")
        Map<String, Object> details
) {
}
