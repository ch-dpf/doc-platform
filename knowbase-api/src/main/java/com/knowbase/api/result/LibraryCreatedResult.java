package com.knowbase.api.result;

import java.util.UUID;

public record LibraryCreatedResult(UUID libraryId, String tenantId, String name) {}
