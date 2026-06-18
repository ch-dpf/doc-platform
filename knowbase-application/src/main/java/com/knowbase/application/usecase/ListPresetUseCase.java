package com.knowbase.application.usecase;

import com.knowbase.api.result.PresetResult;

import java.util.List;

public interface ListPresetUseCase {

    List<PresetResult> listLibraryTypePresets();

    List<PresetResult> listSceneRulePresets();
}
