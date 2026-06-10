package com.knowbase.library.config;

public class CleaningRulesSettings {

    private boolean removeHeaderFooter = true;
    private boolean removeWatermark = true;
    private boolean removeDuplicateParagraphs = true;
    private boolean maskPhone = false;
    private boolean maskIdCard = false;
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
