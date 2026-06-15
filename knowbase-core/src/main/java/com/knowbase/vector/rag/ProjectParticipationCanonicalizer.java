package com.knowbase.vector.rag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 周报「类别/项目」列名归一化，合并同一项目的不同写法。 */
public final class ProjectParticipationCanonicalizer {

    private static final String SHANGHAI_FB_CANONICAL = "上海fb浮标";
    private static final String SHANGHAI_FB_DISPLAY = "上海fb项目";

    private ProjectParticipationCanonicalizer() {}

    public static LinkedHashSet<String> dedupe(Collection<String> raw, List<Set<String>> forcedAliasGroups) {
        Map<String, String> forcedAlias = buildForcedAliasMap(forcedAliasGroups);
        LinkedHashMap<String, String> canonicalToDisplay = new LinkedHashMap<>();
        for (String project : raw) {
            if (project == null || project.isBlank()) {
                continue;
            }
            String resolved = forcedAlias.getOrDefault(matchKey(project), project.strip());
            String canonical = canonicalKey(resolved);
            canonicalToDisplay.putIfAbsent(canonical, displayName(canonical, resolved));
        }
        return new LinkedHashSet<>(canonicalToDisplay.values());
    }

    /** 工作汇总分组键：合并上海fb/浮标/FB 等别名。 */
    public static String groupingKey(String project) {
        if (project == null || project.isBlank()) {
            return "";
        }
        return canonicalKey(project.strip());
    }

    /** 工作汇总展示名：同一规范键使用统一中文项目名。 */
    public static String groupedDisplayLabel(String project) {
        if (project == null || project.isBlank()) {
            return "";
        }
        String stripped = project.strip();
        return fixedGroupedDisplayName(canonicalKey(stripped), stripped);
    }

    static String canonicalKey(String project) {
        String raw = project.strip();
        String compact = raw.toLowerCase(Locale.ROOT).replace("项目", "").replaceAll("\\s+", "");
        if (compact.contains("海图")) {
            return "海图";
        }
        if (isShanghaiFbFamily(raw, compact)) {
            return SHANGHAI_FB_CANONICAL;
        }
        return matchKey(raw);
    }

    private static boolean isShanghaiFbFamily(String raw, String compact) {
        if ("fb".equals(compact) || compact.contains("fb")) {
            return true;
        }
        if (raw.contains("浮标")) {
            return true;
        }
        if (raw.contains("上海")) {
            return true;
        }
        return false;
    }

    private static String fixedGroupedDisplayName(String canonical, String original) {
        return switch (canonical) {
            case "海图" -> "海图项目";
            case SHANGHAI_FB_CANONICAL -> SHANGHAI_FB_DISPLAY;
            default -> original;
        };
    }

    private static String displayName(String canonical, String resolved) {
        return switch (canonical) {
            case "海图" -> "海图项目";
            case SHANGHAI_FB_CANONICAL -> preferShanghaiFbLabel(resolved);
            default -> resolved;
        };
    }

    private static String preferShanghaiFbLabel(String resolved) {
        if (resolved.contains("浮标")) {
            return "上海浮标项目";
        }
        if (resolved.toLowerCase(Locale.ROOT).contains("fb")) {
            return "上海fb项目";
        }
        return resolved.contains("上海") ? resolved : "上海fb项目";
    }

    private static Map<String, String> buildForcedAliasMap(List<Set<String>> forcedAliasGroups) {
        Map<String, String> alias = new LinkedHashMap<>();
        if (forcedAliasGroups == null) {
            return alias;
        }
        for (Set<String> group : forcedAliasGroups) {
            if (group == null || group.size() < 2) {
                continue;
            }
            String representative = pickRepresentative(group);
            for (String name : group) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                alias.put(matchKey(name), representative);
            }
        }
        return alias;
    }

    private static String pickRepresentative(Set<String> group) {
        for (String name : group) {
            if (name != null && name.contains("fb")) {
                return name.strip();
            }
        }
        return group.iterator().next().strip();
    }

    static String matchKey(String project) {
        return project.strip()
                .toLowerCase(Locale.ROOT)
                .replace("项目", "")
                .replaceAll("\\s+", "");
    }
}
