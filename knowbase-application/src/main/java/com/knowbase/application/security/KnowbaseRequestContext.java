package com.knowbase.application.security;

import java.util.List;

public final class KnowbaseRequestContext {

    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private KnowbaseRequestContext() {
    }

    public record Snapshot(String tenantId, String userId, List<String> roles) {
    }

    public static void set(Snapshot snapshot) {
        CURRENT.set(snapshot);
    }

    public static Snapshot get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
