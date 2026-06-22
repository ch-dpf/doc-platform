package com.knowbase.application.service;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.PresetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPresetRepository implements PresetRepository {

    private final ConcurrentHashMap<String, LibraryTypePreset> libraryTypePresets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SceneRulePreset> sceneRulePresets = new ConcurrentHashMap<>();

    @Override
    public LibraryTypePreset saveLibraryTypePreset(LibraryTypePreset preset) {
        libraryTypePresets.put(key(preset.tenantId(), preset.code()), preset);
        return preset;
    }

    @Override
    public SceneRulePreset saveSceneRulePreset(SceneRulePreset preset) {
        sceneRulePresets.put(key(preset.tenantId(), preset.code()), preset);
        return preset;
    }

    @Override
    public List<LibraryTypePreset> listLibraryTypePresets(String tenantId) {
        return libraryTypePresets.values().stream()
                .filter(preset -> tenantId == null || preset.tenantId() == null || tenantId.equals(preset.tenantId()))
                .toList();
    }

    @Override
    public List<SceneRulePreset> listSceneRulePresets(String tenantId) {
        return sceneRulePresets.values().stream()
                .filter(preset -> tenantId == null || preset.tenantId() == null || tenantId.equals(preset.tenantId()))
                .toList();
    }

    @Override
    public Optional<LibraryTypePreset> findLibraryTypePreset(String tenantId, String code) {
        return Optional.ofNullable(libraryTypePresets.get(key(tenantId, code)));
    }

    @Override
    public Optional<SceneRulePreset> findSceneRulePreset(String tenantId, String code) {
        return Optional.ofNullable(sceneRulePresets.get(key(tenantId, code)));
    }

    private static String key(String tenantId, String code) {
        return (tenantId == null ? "" : tenantId) + ":" + code;
    }
}
