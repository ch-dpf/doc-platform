package com.knowbase.library.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "正文清洗规则")
public class CleaningRulesSettings {

    @Schema(description = "去除页眉页脚", example = "true")
    private boolean removeHeaderFooter = true;
    @Schema(description = "去除水印", example = "true")
    private boolean removeWatermark = true;
    @Schema(description = "去除重复段落", example = "true")
    private boolean removeDuplicateParagraphs = true;
    @Schema(description = "手机号脱敏", example = "false")
    private boolean maskPhone = false;
    @Schema(description = "身份证号脱敏", example = "false")
    private boolean maskIdCard = false;
    @Schema(description = "停用词过滤", example = "false")
    private boolean stopwordFilter = false;

    public boolean isRemoveHeaderFooter() {
        return removeHeaderFooter;
    }

    public void setRemoveHeaderFooter(boolean removeHeaderFooter) {
        this.removeHeaderFooter = removeHeaderFooter;
    }

    public boolean isRemoveWatermark() {
        return removeWatermark;
    }

    public void setRemoveWatermark(boolean removeWatermark) {
        this.removeWatermark = removeWatermark;
    }

    public boolean isRemoveDuplicateParagraphs() {
        return removeDuplicateParagraphs;
    }

    public void setRemoveDuplicateParagraphs(boolean removeDuplicateParagraphs) {
        this.removeDuplicateParagraphs = removeDuplicateParagraphs;
    }

    public boolean isMaskPhone() {
        return maskPhone;
    }

    public void setMaskPhone(boolean maskPhone) {
        this.maskPhone = maskPhone;
    }

    public boolean isMaskIdCard() {
        return maskIdCard;
    }

    public void setMaskIdCard(boolean maskIdCard) {
        this.maskIdCard = maskIdCard;
    }

    public boolean isStopwordFilter() {
        return stopwordFilter;
    }

    public void setStopwordFilter(boolean stopwordFilter) {
        this.stopwordFilter = stopwordFilter;
    }
}
