package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateLibraryTypePresetCommand;
import com.knowbase.api.command.CreateSceneRulePresetCommand;
import com.knowbase.api.result.IngestionCatalogResult;
import com.knowbase.api.result.LibraryTypePresetGuideResult;
import com.knowbase.api.result.PageResult;
import com.knowbase.api.result.PresetResult;

public interface ManagePresetUseCase {

    PageResult<PresetResult> pageLibraryTypePresets(String tenantId, int page, int size);

    PresetResult getLibraryTypePreset(String tenantId, String code);

    IngestionCatalogResult getIngestionCatalog();

    LibraryTypePresetGuideResult getLibraryTypePresetGuide(String tenantId, String code);

    PresetResult createLibraryTypePreset(CreateLibraryTypePresetCommand command);

    void deleteLibraryTypePreset(String tenantId, String code);

    PageResult<PresetResult> pageSceneRulePresets(String tenantId, int page, int size);

    PresetResult getSceneRulePreset(String tenantId, String code);

    PresetResult createSceneRulePreset(CreateSceneRulePresetCommand command);

    void deleteSceneRulePreset(String tenantId, String code);
}
