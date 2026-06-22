package com.knowbase.application.service;

import com.knowbase.api.command.CreateLibraryTypePresetCommand;
import com.knowbase.api.command.CreateSceneRulePresetCommand;
import com.knowbase.api.facade.KnowbasePresetFacade;
import com.knowbase.api.result.PresetResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.ListPresetUseCase;
import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.PresetRepository;
import com.knowbase.preset.CompositePresetCatalog;

import java.util.List;
import java.util.UUID;

public class DefaultPresetService implements ListPresetUseCase, KnowbasePresetFacade {

    private final CompositePresetCatalog presetCatalog;
    private final PresetRepository presetRepository;

    public DefaultPresetService(CompositePresetCatalog presetCatalog, PresetRepository presetRepository) {
        this.presetCatalog = presetCatalog;
        this.presetRepository = presetRepository;
    }

    @Override
    public List<PresetResult> listLibraryTypePresets() {
        return presetCatalog.listLibraryTypePresets().stream()
                .filter(preset -> preset.enabled())
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    public List<PresetResult> listLibraryTypePresets(String tenantId) {
        return presetCatalog.listLibraryTypePresets(tenantId).stream()
                .filter(preset -> preset.enabled())
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    @Override
    public List<PresetResult> listSceneRulePresets() {
        return presetCatalog.listSceneRulePresets().stream()
                .filter(preset -> preset.enabled())
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    public List<PresetResult> listSceneRulePresets(String tenantId) {
        return presetCatalog.listSceneRulePresets(tenantId).stream()
                .filter(preset -> preset.enabled())
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    public PresetResult createLibraryTypePreset(CreateLibraryTypePresetCommand command) {
        LibraryTypePreset saved = presetRepository.saveLibraryTypePreset(new LibraryTypePreset(
                UUID.randomUUID(),
                command.tenantId(),
                command.code(),
                command.name(),
                command.description(),
                command.config(),
                false,
                true
        ));
        return ResultMapper.toPresetResult(saved);
    }

    public PresetResult createSceneRulePreset(CreateSceneRulePresetCommand command) {
        SceneRulePreset saved = presetRepository.saveSceneRulePreset(new SceneRulePreset(
                UUID.randomUUID(),
                command.tenantId(),
                command.code(),
                command.name(),
                command.description(),
                command.config(),
                false,
                true
        ));
        return ResultMapper.toPresetResult(saved);
    }
}
