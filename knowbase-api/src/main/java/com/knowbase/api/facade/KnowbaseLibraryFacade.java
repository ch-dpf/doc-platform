package com.knowbase.api.facade;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryResult;
import com.knowbase.api.result.PageResult;

import java.util.List;
import java.util.UUID;

public interface KnowbaseLibraryFacade {

    LibraryResult createLibrary(CreateLibraryCommand command);

    LibraryResult getLibrary(UUID libraryId);

    List<LibraryResult> listLibraries(String tenantId);

    PageResult<LibraryResult> pageLibraries(String tenantId, int page, int size);

    void deleteLibrary(UUID libraryId);
}
