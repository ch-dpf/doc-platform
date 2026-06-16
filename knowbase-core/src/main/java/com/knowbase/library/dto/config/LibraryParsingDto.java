package com.knowbase.library.dto.config;

import com.knowbase.library.config.ParserEngineRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "库级解析配置（按文件类型选内置解析器 + 高级选项）")
public record LibraryParsingDto(
        @Schema(description = "按文件类型绑定的解析器规则") @Valid @NotNull List<ParserEngineRule> parserRules,
        @Schema(description = "默认语言（OCR/Tika）", example = "zh-CN") String defaultLanguage,
        @Schema(description = "是否自动检测文件编码", example = "true") Boolean autoDetectEncoding) {

    public LibraryParsingDto {
        if (parserRules == null) {
            parserRules = new ArrayList<>();
        }
    }
}
