package com.knowbase.library.config;

import io.swagger.v3.oas.annotations.media.Schema;

/** 库级按文件类型绑定的内置解析器规则。 */
@Schema(description = "按文件类型选择内置解析器")
public class ParserEngineRule {

    @Schema(description = "文件类型标识", example = "pdf", allowableValues = {"pdf", "word", "excel", "txt", "markdown"})
    private String fileType;

    @Schema(
            description = "内置解析器 ID",
            example = "tika-ocr-auto",
            allowableValues = {
                "auto",
                "tika-plain",
                "tika-structured",
                "tika-ocr-auto",
                "excel-structured",
                "tika-table-text"
            })
    private String parserId = "auto";

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getParserId() {
        return parserId;
    }

    public void setParserId(String parserId) {
        this.parserId = parserId != null ? parserId.trim().toLowerCase() : "auto";
    }
}
