package com.knowbase.api.facade;

import com.knowbase.api.command.RagChatCommand;
import com.knowbase.api.result.RagChatResult;

public interface KnowbaseRagFacade {

    RagChatResult chat(RagChatCommand command);
}
