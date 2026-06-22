package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "当前页数据")
        List<T> items,
        @Schema(description = "总记录数")
        long total,
        @Schema(description = "当前页码，从 1 开始")
        int page,
        @Schema(description = "每页条数")
        int size
) {
}
