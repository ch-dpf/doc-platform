package com.knowbase.preset;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;

import java.util.List;
import java.util.Optional;

public interface PresetCatalog {

    List<LibraryTypePreset> listLibraryTypePresets();

    List<SceneRulePreset> listSceneRulePresets();

    Optional<LibraryTypePreset> findLibraryTypePreset(String code);

    Optional<SceneRulePreset> findSceneRulePreset(String code);
}
