package com.knowbase.application.service;

import com.knowbase.api.command.CreateTokenizerProfileCommand;
import com.knowbase.api.facade.KnowbaseTokenizerProfileFacade;
import com.knowbase.api.result.TokenizerProfileResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.ManageTokenizerProfileUseCase;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultTokenizerProfileService implements ManageTokenizerProfileUseCase, KnowbaseTokenizerProfileFacade {

    private final KnowbaseRepository repository;

    public DefaultTokenizerProfileService(KnowbaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public TokenizerProfileResult create(CreateTokenizerProfileCommand command) {
        Instant now = Instant.now();
        TokenizerProfile profile = findAny(command.provider(), command.modelName())
                .map(existing -> new TokenizerProfile(
                        existing.tokenizerProfileId(),
                        command.provider(),
                        command.modelName(),
                        command.tokenizerId(),
                        command.tokenizerVersion(),
                        command.approximate(),
                        command.config() == null ? Map.of() : command.config(),
                        command.enabled(),
                        existing.createdAt(),
                        now
                ))
                .orElseGet(() -> new TokenizerProfile(
                        UUID.randomUUID(),
                        command.provider(),
                        command.modelName(),
                        command.tokenizerId(),
                        command.tokenizerVersion(),
                        command.approximate(),
                        command.config() == null ? Map.of() : command.config(),
                        command.enabled(),
                        now,
                        now
                ));
        return ResultMapper.toTokenizerProfileResult(repository.saveTokenizerProfile(profile));
    }

    @Override
    public TokenizerProfileResult get(UUID tokenizerProfileId) {
        return repository.findTokenizerProfile(tokenizerProfileId)
                .map(ResultMapper::toTokenizerProfileResult)
                .orElseThrow(() -> new ResourceNotFoundException("Tokenizer Profile 不存在: " + tokenizerProfileId));
    }

    @Override
    public List<TokenizerProfileResult> list(String provider, boolean includeDisabled) {
        ensureDefaults();
        return repository.listTokenizerProfiles(provider, includeDisabled).stream()
                .map(ResultMapper::toTokenizerProfileResult)
                .toList();
    }

    @Override
    public TokenizerProfileResult createTokenizerProfile(CreateTokenizerProfileCommand command) {
        return create(command);
    }

    @Override
    public TokenizerProfileResult getTokenizerProfile(UUID tokenizerProfileId) {
        return get(tokenizerProfileId);
    }

    @Override
    public List<TokenizerProfileResult> listTokenizerProfiles(String provider, boolean includeDisabled) {
        return list(provider, includeDisabled);
    }

    private void ensureDefaults() {
        seed("ollama", "bge-m3", "ollama:bge-m3", "bge-m3", true);
        seed("ollama", "llama3.2", "ollama:llama3.2", "llama3.2", true);
        seed("ollama", "llama3", "ollama:llama3", "llama3", true);
        seed("default", "default", "approx-default", "1", true);
    }

    private void seed(String provider, String modelName, String tokenizerId, String version, boolean approximate) {
        if (findAny(provider, modelName).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        repository.saveTokenizerProfile(new TokenizerProfile(
                UUID.randomUUID(),
                provider,
                modelName,
                tokenizerId,
                version,
                approximate,
                Map.of("builtIn", true),
                true,
                now,
                now
        ));
    }

    private java.util.Optional<TokenizerProfile> findAny(String provider, String modelName) {
        return repository.listTokenizerProfiles(provider, true).stream()
                .filter(profile -> profile.modelName().equals(modelName))
                .findFirst();
    }
}
