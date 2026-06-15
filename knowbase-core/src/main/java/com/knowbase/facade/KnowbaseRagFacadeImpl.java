package com.knowbase.facade;

import com.knowbase.api.command.RagChatCommand;
import com.knowbase.api.facade.KnowbaseRagFacade;
import com.knowbase.api.result.RagChatResult;
import com.knowbase.api.result.RagCitationResult;
import com.knowbase.vector.dto.RagChatRequest;
import com.knowbase.vector.dto.RagChatResponse;
import com.knowbase.vector.dto.RagCitation;
import com.knowbase.vector.service.RagService;

import java.util.List;

public class KnowbaseRagFacadeImpl implements KnowbaseRagFacade {

    private final RagService ragService;
    private final KnowbaseTenantSupport tenantSupport;

    public KnowbaseRagFacadeImpl(RagService ragService, KnowbaseTenantSupport tenantSupport) {
        this.ragService = ragService;
        this.tenantSupport = tenantSupport;
    }

    @Override
    public RagChatResult chat(RagChatCommand command) {
        String tenantId = tenantSupport.resolve(command.tenantId());
        RagChatRequest request = new RagChatRequest(
                command.libraryId(),
                tenantId,
                command.question(),
                command.topK(),
                null,
                null,
                null);
        RagChatResponse response = ragService.chat(request);
        return new RagChatResult(
                response.answer(),
                mapCitations(response.citations()),
                response.retrievedCount(),
                response.usedLlm(),
                response.found(),
                response.searchQuery());
    }

    private static List<RagCitationResult> mapCitations(List<RagCitation> citations) {
        if (citations == null) {
            return List.of();
        }
        return citations.stream()
                .map(c -> new RagCitationResult(
                        c.chunkId(),
                        c.docId(),
                        c.chunkIndex(),
                        c.score(),
                        c.excerpt(),
                        c.fileName()))
                .toList();
    }
}
