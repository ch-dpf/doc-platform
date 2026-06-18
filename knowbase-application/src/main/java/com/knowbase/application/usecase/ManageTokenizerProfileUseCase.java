package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateTokenizerProfileCommand;
import com.knowbase.api.result.TokenizerProfileResult;

import java.util.List;
import java.util.UUID;

public interface ManageTokenizerProfileUseCase {

    TokenizerProfileResult create(CreateTokenizerProfileCommand command);

    TokenizerProfileResult get(UUID tokenizerProfileId);

    List<TokenizerProfileResult> list(String provider, boolean includeDisabled);
}
