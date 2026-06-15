package com.knowbase.pipeline.content;

import com.knowbase.platform.JsonSupport;

public final class ContentSignalsSupport {

    private ContentSignalsSupport() {
    }

    public static String toJson(ContentSignals signals) {
        if (signals == null || signals.isEmpty()) {
            return null;
        }
        return JsonSupport.toJson(signals);
    }

    public static ContentSignals parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JsonSupport.fromJson(json, ContentSignals.class);
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
