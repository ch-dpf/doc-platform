package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryResult;
import com.knowbase.api.result.PageResult;

import java.util.List;
import java.util.UUID;

public interface CreateLibraryUseCase {

    LibraryResult create(CreateLibraryCommand command);

    LibraryResult get(UUID libraryId);

    List<LibraryResult> list(String tenantId);

    PageResult<LibraryResult> page(String tenantId, int page, int size);

    void delete(UUID libraryId);
}
