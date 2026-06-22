package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryResult;

import java.util.List;
import java.util.UUID;

public interface CreateLibraryUseCase {

    LibraryResult create(CreateLibraryCommand command);

    LibraryResult get(UUID libraryId);

    List<LibraryResult> list(String tenantId);
}
