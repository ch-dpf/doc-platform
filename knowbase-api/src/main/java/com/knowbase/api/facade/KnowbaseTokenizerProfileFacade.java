package com.knowbase.api.facade;

import com.knowbase.api.command.CreateTokenizerProfileCommand;
import com.knowbase.api.result.TokenizerProfileResult;

import java.util.List;
import java.util.UUID;

public interface KnowbaseTokenizerProfileFacade {

    TokenizerProfileResult createTokenizerProfile(CreateTokenizerProfileCommand command);

    TokenizerProfileResult getTokenizerProfile(UUID tokenizerProfileId);

    List<TokenizerProfileResult> listTokenizerProfiles(String provider, boolean includeDisabled);
}
