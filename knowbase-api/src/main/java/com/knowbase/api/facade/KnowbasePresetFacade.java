package com.knowbase.api.facade;

import com.knowbase.api.result.PageResult;
import com.knowbase.api.result.PresetResult;

import java.util.List;

public interface KnowbasePresetFacade {

    PageResult<PresetResult> pageLibraryTypePresets(String tenantId, int page, int size);

    PresetResult getLibraryTypePreset(String tenantId, String code);

    List<PresetResult> listLibraryTypePresets();

    PageResult<PresetResult> pageSceneRulePresets(String tenantId, int page, int size);

    PresetResult getSceneRulePreset(String tenantId, String code);

    List<PresetResult> listSceneRulePresets();
}
