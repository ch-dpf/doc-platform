package com.knowbase.api.facade;

import com.knowbase.api.result.PresetResult;

import java.util.List;

public interface KnowbasePresetFacade {

    List<PresetResult> listLibraryTypePresets();

    List<PresetResult> listSceneRulePresets();
}
