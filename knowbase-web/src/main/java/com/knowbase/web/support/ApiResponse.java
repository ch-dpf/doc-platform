package com.knowbase.web.support;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "统一 API 响应")
public record ApiResponse<T>(
        @Schema(description = "是否成功", example = "true")
        boolean success,
        @Schema(description = "响应码", example = "OK")
        String code,
        @Schema(description = "响应消息", example = "success")
        String message,
        @Schema(description = "响应数据")
        T data,
        @Schema(description = "响应时间戳")
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data, Instant.now());
    }

    public static <T> ApiResponse<T> failed(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }
}
