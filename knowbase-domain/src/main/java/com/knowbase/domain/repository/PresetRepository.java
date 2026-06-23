package com.knowbase.domain.repository;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;

import java.util.List;
import java.util.Optional;

public interface PresetRepository {

    LibraryTypePreset saveLibraryTypePreset(LibraryTypePreset preset);

    SceneRulePreset saveSceneRulePreset(SceneRulePreset preset);

    List<LibraryTypePreset> listLibraryTypePresets(String tenantId);

    List<SceneRulePreset> listSceneRulePresets(String tenantId);

    Optional<LibraryTypePreset> findLibraryTypePreset(String tenantId, String code);

    Optional<SceneRulePreset> findSceneRulePreset(String tenantId, String code);

    void deleteLibraryTypePreset(String tenantId, String code);

    void deleteSceneRulePreset(String tenantId, String code);
}
