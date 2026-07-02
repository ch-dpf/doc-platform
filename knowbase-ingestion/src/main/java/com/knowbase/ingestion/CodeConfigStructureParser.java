package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structure-aware parser for code and configuration documents (plan §3.4).
 */
public final class CodeConfigStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "code-config-structure";

    private static final Pattern CODE_BOUNDARY = Pattern.compile(
            "^(?:\\s*(?:package|import|public|private|protected|class|interface|enum|record|def|function|export|const|let|var)\\b|#\\s*\\w+).*$"
    );

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String extension = extension(sourceUri);
        return extension != null && isSupportedExtension(extension);
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String text = readText(source);
        String extension = extension(source.sourceUri());
        List<StructuralBlock> blocks = switch (extension) {
            case "yml", "yaml" -> parseYamlSections(text);
            case "json" -> parseJsonSections(text);
            case "properties" -> parsePropertiesSections(text);
            default -> parseCodeSections(text, extension);
        };
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("parserCode", PARSER_CODE);
        metadata.put("structureAware", !blocks.isEmpty());
        metadata.put("configFormat", extension);
        metadata.put("blockCount", blocks.size());
        String flatText = blocks.isEmpty() ? text : StructureParsingSupport.blocksToText(blocks);
        return new ParsedDocument(
                source.sourceUri(),
                source.filename(),
                flatText,
                ContentFamily.CODE_OR_CONFIG,
                Map.copyOf(metadata),
                blocks
        );
    }

    private static List<StructuralBlock> parseYamlSections(String text) {
        Object loaded = new Yaml().load(text);
        if (!(loaded instanceof Map<?, ?> map) || map.isEmpty()) {
            return StructureParsingSupport.parsePlainText(text);
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml dumper = new Yaml(options);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Map<String, Object> section = Map.of(key, entry.getValue());
            String content = dumper.dump(section).trim();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("boundaryType", "config_section");
            metadata.put("configFormat", "yaml");
            metadata.put("configKey", key);
            metadata.put("indexableHint", true);
            blocks.add(new StructuralBlock("config_section", 0, content, ordinal++, Map.copyOf(metadata)));
        }
        return blocks;
    }

    @SuppressWarnings("unchecked")
    private static List<StructuralBlock> parseJsonSections(String text) {
        Object loaded = new Yaml().load(text);
        if (!(loaded instanceof Map<?, ?> map) || map.isEmpty()) {
            return StructureParsingSupport.parsePlainText(text);
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Map<String, Object> section = new LinkedHashMap<>();
            section.put(key, entry.getValue());
            String content = compactJsonSection(section);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("boundaryType", "config_section");
            metadata.put("configFormat", "json");
            metadata.put("configKey", key);
            metadata.put("indexableHint", true);
            blocks.add(new StructuralBlock("config_section", 0, content, ordinal++, Map.copyOf(metadata)));
        }
        return blocks;
    }

    private static String compactJsonSection(Map<String, Object> section) {
        StringBuilder builder = new StringBuilder("{\n");
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            builder.append("  \"").append(entry.getKey()).append("\": ");
            builder.append(renderJsonValue(entry.getValue(), 2));
            builder.append('\n');
        }
        builder.append('}');
        return builder.toString();
    }

    private static String renderJsonValue(Object value, int indent) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + string.replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            String pad = " ".repeat(indent);
            StringBuilder builder = new StringBuilder("{\n");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                builder.append(pad).append("  \"").append(entry.getKey()).append("\": ");
                builder.append(renderJsonValue(entry.getValue(), indent + 2));
                builder.append(",\n");
            }
            if (!map.isEmpty()) {
                builder.setLength(builder.length() - 2);
                builder.append('\n');
            }
            builder.append(pad).append('}');
            return builder.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int index = 0; index < list.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(renderJsonValue(list.get(index), indent));
            }
            builder.append(']');
            return builder.toString();
        }
        return "\"" + String.valueOf(value).replace("\"", "\\\"") + "\"";
    }

    private static List<StructuralBlock> parsePropertiesSections(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new java.io.StringReader(text));
        } catch (IOException exception) {
            return StructureParsingSupport.parsePlainText(text);
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            String prefix = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
            groups.computeIfAbsent(prefix, ignored -> new ArrayList<>()).add(name + "=" + properties.getProperty(name));
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String content = String.join("\n", entry.getValue());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("boundaryType", "config_section");
            metadata.put("configFormat", "properties");
            metadata.put("configKey", entry.getKey());
            metadata.put("indexableHint", true);
            blocks.add(new StructuralBlock("config_section", 0, content, ordinal++, Map.copyOf(metadata)));
        }
        return blocks;
    }

    private static List<StructuralBlock> parseCodeSections(String text, String extension) {
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        List<StructuralBlock> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int ordinal = 0;
        for (String line : lines) {
            Matcher matcher = CODE_BOUNDARY.matcher(line);
            if (matcher.matches() && current.length() > 0) {
                blocks.add(codeBlock(current.toString().trim(), ordinal++, extension));
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(line);
        }
        if (current.length() > 0) {
            blocks.add(codeBlock(current.toString().trim(), ordinal, extension));
        }
        return blocks.isEmpty() ? StructureParsingSupport.parsePlainText(text) : blocks;
    }

    private static StructuralBlock codeBlock(String content, int ordinal, String extension) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("boundaryType", "code_block");
        metadata.put("configFormat", extension);
        metadata.put("indexableHint", true);
        return new StructuralBlock("code_block", 0, content, ordinal, Map.copyOf(metadata));
    }

    private static boolean isSupportedExtension(String extension) {
        return switch (extension) {
            case "java", "kt", "js", "ts", "vue", "py", "yml", "yaml", "json", "xml", "properties" -> true;
            default -> false;
        };
    }

    private static String extension(String sourceUri) {
        if (sourceUri == null) {
            return null;
        }
        int dot = sourceUri.lastIndexOf('.');
        if (dot < 0 || dot == sourceUri.length() - 1) {
            return null;
        }
        return sourceUri.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String readText(DocumentSource source) {
        try (InputStream inputStream = source.inputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取代码/配置文档失败: " + source.sourceUri(), exception);
        }
    }
}
