package com.knowbase.application.service;

import com.knowbase.api.facade.KnowbasePresetFacade;
import com.knowbase.api.result.PresetResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.ListPresetUseCase;
import com.knowbase.preset.PresetCatalog;

import java.util.List;

public class DefaultPresetService implements ListPresetUseCase, KnowbasePresetFacade {

    private final PresetCatalog presetCatalog;

    public DefaultPresetService(PresetCatalog presetCatalog) {
        this.presetCatalog = presetCatalog;
    }

    @Override
    public List<PresetResult> listLibraryTypePresets() {
        return presetCatalog.listLibraryTypePresets().stream()
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
}
