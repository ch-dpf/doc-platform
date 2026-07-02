package com.knowbase.preset;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.PresetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class CompositePresetCatalog implements PresetCatalog {

    private final BuiltinPresetCatalog builtinPresetCatalog;
    private final PresetRepository presetRepository;

    public CompositePresetCatalog(BuiltinPresetCatalog builtinPresetCatalog, PresetRepository presetRepository) {
        this.builtinPresetCatalog = builtinPresetCatalog;
        this.presetRepository = presetRepository;
    }

    @Override
    public List<LibraryTypePreset> listLibraryTypePresets() {
        return mergeLibraryTypePresets(presetRepository.listLibraryTypePresets(null));
    }

    public List<LibraryTypePreset> listLibraryTypePresets(String tenantId) {
        return mergeLibraryTypePresets(presetRepository.listLibraryTypePresets(tenantId));
    }

    @Override
    public List<SceneRulePreset> listSceneRulePresets() {
        return mergeSceneRulePresets(presetRepository.listSceneRulePresets(null));
    }

    public List<SceneRulePreset> listSceneRulePresets(String tenantId) {
        return mergeSceneRulePresets(presetRepository.listSceneRulePresets(tenantId));
    }

    @Override
    public Optional<LibraryTypePreset> findLibraryTypePreset(String code) {
        return findLibraryTypePreset(null, code);
    }

    public Optional<LibraryTypePreset> findLibraryTypePreset(String tenantId, String code) {
        Optional<LibraryTypePreset> custom = presetRepository.findLibraryTypePreset(tenantId, code);
        if (custom.isPresent()) {
            return custom;
        }
        return builtinPresetCatalog.findLibraryTypePreset(code);
    }

    @Override
    public Optional<SceneRulePreset> findSceneRulePreset(String code) {
        return findSceneRulePreset(null, code);
    }

    public Optional<SceneRulePreset> findSceneRulePreset(String tenantId, String code) {
        Optional<SceneRulePreset> custom = presetRepository.findSceneRulePreset(tenantId, code);
        if (custom.isPresent()) {
            return custom;
        }
        return builtinPresetCatalog.findSceneRulePreset(code);
    }

    private List<LibraryTypePreset> mergeLibraryTypePresets(List<LibraryTypePreset> customPresets) {
        List<LibraryTypePreset> merged = new ArrayList<>(builtinPresetCatalog.listLibraryTypePresets());
        for (LibraryTypePreset custom : customPresets) {
            boolean replaced = false;
            for (int index = 0; index < merged.size(); index++) {
                if (merged.get(index).code().equals(custom.code())) {
                    merged.set(index, custom);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                merged.add(custom);
            }
        }
        return List.copyOf(merged);
    }

    private List<SceneRulePreset> mergeSceneRulePresets(List<SceneRulePreset> customPresets) {
        List<SceneRulePreset> merged = new ArrayList<>(builtinPresetCatalog.listSceneRulePresets());
        for (SceneRulePreset custom : customPresets) {
            boolean replaced = false;
            for (int index = 0; index < merged.size(); index++) {
                if (merged.get(index).code().equals(custom.code())) {
                    merged.set(index, custom);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                merged.add(custom);
            }
        }
        return List.copyOf(merged);
    }
}
