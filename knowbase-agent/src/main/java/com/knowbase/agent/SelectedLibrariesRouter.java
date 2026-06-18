package com.knowbase.agent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SelectedLibrariesRouter implements LibraryRouter {

    @Override
    public List<UUID> route(RouteRequest request) {
        if (request.candidateLibraryIds() == null || request.candidateLibraryIds().isEmpty()) {
            return List.of();
        }
        String mode = readMode(request.routingPolicy());
        List<UUID> routed = List.copyOf(request.candidateLibraryIds());
        if ("keyword".equals(mode)) {
            routed = routeByKeyword(request);
        }
        int maxLibraries = readInt(request.routingPolicy(), "maxLibraries", routed.size());
        return routed.stream().limit(Math.max(1, maxLibraries)).toList();
    }

    private static String readMode(Map<String, Object> routingPolicy) {
        if (routingPolicy == null || routingPolicy.get("mode") == null) {
            return null;
        }
        return String.valueOf(routingPolicy.get("mode"));
    }

    private static List<UUID> routeByKeyword(RouteRequest request) {
        Object rawRules = request.routingPolicy() == null ? null : request.routingPolicy().get("libraryKeywords");
        if (!(rawRules instanceof Map<?, ?> rules)) {
            return List.copyOf(request.candidateLibraryIds());
        }
        String question = request.question() == null ? "" : request.question().toLowerCase();
        List<UUID> selected = new java.util.ArrayList<>();
        for (UUID libraryId : request.candidateLibraryIds()) {
            Object rawKeywords = rules.get(libraryId.toString());
            if (!(rawKeywords instanceof List<?> keywords)) {
                continue;
            }
            boolean matched = keywords.stream()
                    .map(String::valueOf)
                    .map(String::toLowerCase)
                    .anyMatch(question::contains);
            if (matched) {
                selected.add(libraryId);
            }
        }
        return selected.isEmpty() ? List.copyOf(request.candidateLibraryIds()) : selected;
    }

    private static int readInt(Map<String, Object> routingPolicy, String key, int defaultValue) {
        if (routingPolicy == null || routingPolicy.get(key) == null) {
            return defaultValue;
        }
        Object value = routingPolicy.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
