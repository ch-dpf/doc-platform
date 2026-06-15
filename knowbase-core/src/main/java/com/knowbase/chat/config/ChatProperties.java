package com.knowbase.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private int maxHistoryMessages = 10;
    private int summaryTriggerMessages = 20;
    private int maxTitleChars = 40;

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public int getSummaryTriggerMessages() {
        return summaryTriggerMessages;
    }

    public void setSummaryTriggerMessages(int summaryTriggerMessages) {
        this.summaryTriggerMessages = summaryTriggerMessages;
    }

    public int getMaxTitleChars() {
        return maxTitleChars;
    }

    public void setMaxTitleChars(int maxTitleChars) {
        this.maxTitleChars = maxTitleChars;
    }
}
