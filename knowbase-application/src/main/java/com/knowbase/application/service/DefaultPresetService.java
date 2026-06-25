package com.knowbase.application.service;

import com.knowbase.api.command.CreateLibraryTypePresetCommand;
import com.knowbase.api.command.CreateSceneRulePresetCommand;
import com.knowbase.api.facade.KnowbasePresetFacade;
import com.knowbase.api.result.IngestionCatalogResult;
import com.knowbase.api.result.LibraryTypePresetGuideResult;
import com.knowbase.api.result.PageResult;
import com.knowbase.api.result.PresetResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.ManagePresetUseCase;
import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.repository.PresetRepository;
import com.knowbase.preset.CompositePresetCatalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultPresetService implements ManagePresetUseCase, KnowbasePresetFacade {

    private final CompositePresetCatalog presetCatalog;
    private final PresetRepository presetRepository;
    private final IngestionCatalogService ingestionCatalogService;

    public DefaultPresetService(
            CompositePresetCatalog presetCatalog,
            PresetRepository presetRepository,
            IngestionCatalogService ingestionCatalogService
    ) {
        this.presetCatalog = presetCatalog;
        this.presetRepository = presetRepository;
        this.ingestionCatalogService = ingestionCatalogService;
    }

    @Override
    public PageResult<PresetResult> pageLibraryTypePresets(String tenantId, int page, int size) {
        List<PresetResult> all = listEnabledLibraryTypePresets(tenantId);
        return paginate(all, page, size);
    }

    @Override
    public PresetResult getLibraryTypePreset(String tenantId, String code) {
        return findEnabledLibraryTypePreset(tenantId, code)
                .map(ResultMapper::toPresetResult)
                .orElseThrow(() -> new ResourceNotFoundException("库类型预设不存在: " + code));
    }

    @Override
    public IngestionCatalogResult getIngestionCatalog() {
        return ingestionCatalogService.getCatalog();
    }

    @Override
    public LibraryTypePresetGuideResult getLibraryTypePresetGuide(String tenantId, String code) {
        return ingestionCatalogService.getLibraryTypePresetGuide(tenantId, code);
    }

    @Override
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

    @Override
    public void deleteLibraryTypePreset(String tenantId, String code) {
        LibraryTypePreset preset = presetRepository.findLibraryTypePreset(tenantId, code)
                .orElseThrow(() -> new ResourceNotFoundException("库类型预设不存在: " + code));
        if (preset.builtIn()) {
            throw new IllegalStateException("系统内置预设不可删除: " + code);
        }
        presetRepository.deleteLibraryTypePreset(tenantId, code);
    }

    @Override
    public PageResult<PresetResult> pageSceneRulePresets(String tenantId, int page, int size) {
        List<PresetResult> all = listEnabledSceneRulePresets(tenantId);
        return paginate(all, page, size);
    }

    @Override
    public PresetResult getSceneRulePreset(String tenantId, String code) {
        return findEnabledSceneRulePreset(tenantId, code)
                .map(ResultMapper::toPresetResult)
                .orElseThrow(() -> new ResourceNotFoundException("场景规则预设不存在: " + code));
    }

    @Override
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

    @Override
    public void deleteSceneRulePreset(String tenantId, String code) {
        SceneRulePreset preset = presetRepository.findSceneRulePreset(tenantId, code)
                .orElseThrow(() -> new ResourceNotFoundException("场景规则预设不存在: " + code));
        if (preset.builtIn()) {
            throw new IllegalStateException("系统内置预设不可删除: " + code);
        }
        presetRepository.deleteSceneRulePreset(tenantId, code);
    }

    @Override
    public List<PresetResult> listLibraryTypePresets() {
        return listEnabledLibraryTypePresets(null);
    }

    @Override
    public List<PresetResult> listSceneRulePresets() {
        return listEnabledSceneRulePresets(null);
    }

    private List<PresetResult> listEnabledLibraryTypePresets(String tenantId) {
        List<LibraryTypePreset> presets = tenantId == null || tenantId.isBlank()
                ? presetCatalog.listLibraryTypePresets()
                : presetCatalog.listLibraryTypePresets(tenantId);
        return presets.stream()
                .filter(LibraryTypePreset::enabled)
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    private List<PresetResult> listEnabledSceneRulePresets(String tenantId) {
        List<SceneRulePreset> presets = tenantId == null || tenantId.isBlank()
                ? presetCatalog.listSceneRulePresets()
                : presetCatalog.listSceneRulePresets(tenantId);
        return presets.stream()
                .filter(SceneRulePreset::enabled)
                .map(ResultMapper::toPresetResult)
                .toList();
    }

    private Optional<LibraryTypePreset> findEnabledLibraryTypePreset(String tenantId, String code) {
        Optional<LibraryTypePreset> preset = tenantId == null || tenantId.isBlank()
                ? presetCatalog.findLibraryTypePreset(code)
                : presetCatalog.findLibraryTypePreset(tenantId, code);
        return preset.filter(LibraryTypePreset::enabled);
    }

    private Optional<SceneRulePreset> findEnabledSceneRulePreset(String tenantId, String code) {
        Optional<SceneRulePreset> preset = tenantId == null || tenantId.isBlank()
                ? presetCatalog.findSceneRulePreset(code)
                : presetCatalog.findSceneRulePreset(tenantId, code);
        return preset.filter(SceneRulePreset::enabled);
    }

    private static PageResult<PresetResult> paginate(List<PresetResult> all, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int fromIndex = (safePage - 1) * safeSize;
        if (fromIndex >= all.size()) {
            return new PageResult<>(List.of(), all.size(), safePage, safeSize);
        }
        int toIndex = Math.min(fromIndex + safeSize, all.size());
        return new PageResult<>(all.subList(fromIndex, toIndex), all.size(), safePage, safeSize);
    }
}
