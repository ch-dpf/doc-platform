package com.knowbase.application.service;

import com.knowbase.api.result.DocumentProfileCatalogItemResult;
import com.knowbase.api.result.DocumentProfileGuideResult;
import com.knowbase.api.result.IngestionCatalogResult;
import com.knowbase.api.result.LibraryTypePresetGuideResult;
import com.knowbase.api.result.ParserCatalogItemResult;
import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.preset.CompositePresetCatalog;
import com.knowbase.preset.IngestionProductCatalog;

import java.util.List;
import java.util.Map;

public final class IngestionCatalogService {

    private final CompositePresetCatalog presetCatalog;
    private final ParserHealthProbe parserHealthProbe;

    public IngestionCatalogService(CompositePresetCatalog presetCatalog) {
        this(presetCatalog, ParserHealthProbe.noop());
    }

    public IngestionCatalogService(CompositePresetCatalog presetCatalog, ParserHealthProbe parserHealthProbe) {
        this.presetCatalog = presetCatalog;
        this.parserHealthProbe = parserHealthProbe == null ? ParserHealthProbe.noop() : parserHealthProbe;
    }

    public IngestionCatalogResult getCatalog() {
        List<ParserCatalogItemResult> parsers = IngestionProductCatalog.parsers().stream()
                .map(parser -> new ParserCatalogItemResult(
                        parser.code(),
                        parser.nameZh(),
                        parser.descriptionZh(),
                        parser.builtIn(),
                        parser.external(),
                        parser.endpointRequired(),
                        parser.supportedExtensions(),
                        parser.capabilities(),
                        parserHealthProbe.check(parser.code())
                ))
                .toList();
        List<DocumentProfileCatalogItemResult> profiles = IngestionProductCatalog.profileTemplates().stream()
                .map(profile -> new DocumentProfileCatalogItemResult(
                        profile.code(),
                        profile.nameZh(),
                        profile.descriptionZh(),
                        profile.contentFamily(),
                        profile.defaultParserCode(),
                        profile.defaultChunkingStrategy(),
                        profile.chunkingStrategyLabelZh(),
                        profile.fileExtensions(),
                        profile.configurableFields(),
                        profile.immutableFields()
                ))
                .toList();
        return new IngestionCatalogResult(
                parsers,
                profiles,
                IngestionProductCatalog.configurationModelNoteZh()
        );
    }

    public LibraryTypePresetGuideResult getLibraryTypePresetGuide(String tenantId, String code) {
        LibraryTypePreset preset = (tenantId == null || tenantId.isBlank()
                ? presetCatalog.findLibraryTypePreset(code)
                : presetCatalog.findLibraryTypePreset(tenantId, code))
                .filter(LibraryTypePreset::enabled)
                .orElseThrow(() -> new ResourceNotFoundException("库类型预设不存在: " + code));
        Map<String, Object> payload = IngestionProductCatalog.buildPresetGuidePayload(preset);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> profileMaps = (List<Map<String, Object>>) payload.get("documentProfiles");
        List<DocumentProfileGuideResult> profiles = profileMaps.stream()
                .map(this::toProfileGuide)
                .toList();
        @SuppressWarnings("unchecked")
        Map<String, Object> l1 = (Map<String, Object>) payload.get("l1Defaults");
        @SuppressWarnings("unchecked")
        List<String> suitable = (List<String>) payload.get("suitableFileTypesZh");
        @SuppressWarnings("unchecked")
        List<String> caution = (List<String>) payload.get("cautionFileTypesZh");
        @SuppressWarnings("unchecked")
        List<String> hints = (List<String>) payload.get("changeImpactHintsZh");
        return new LibraryTypePresetGuideResult(
                preset.code(),
                preset.name(),
                preset.description(),
                String.valueOf(payload.get("instanceBindingNoteZh")),
                suitable,
                caution,
                l1,
                profiles,
                hints
        );
    }

    private DocumentProfileGuideResult toProfileGuide(Map<String, Object> map) {
        return new DocumentProfileGuideResult(
                stringValue(map, "code"),
                stringValue(map, "nameZh"),
                stringValue(map, "descriptionZh"),
                stringValue(map, "contentFamily"),
                stringValue(map, "parserCode"),
                stringValue(map, "parserNameZh"),
                booleanValue(map, "parserBuiltIn"),
                booleanValue(map, "parserExternal"),
                stringValue(map, "chunkingStrategy"),
                stringValue(map, "chunkingStrategyLabelZh"),
                listValue(map, "fileExtensions")
        );
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean booleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static List<String> listValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
