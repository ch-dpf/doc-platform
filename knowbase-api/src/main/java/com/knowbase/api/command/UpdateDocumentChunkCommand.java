package com.knowbase.api.command;

import jakarta.validation.constraints.Size;

public record UpdateDocumentChunkCommand(
        @Size(max = 65536) String content,
        Boolean retrievalEnabled
) {
}
