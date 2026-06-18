package com.knowbase.api.facade;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryResult;

import java.util.List;
import java.util.UUID;

public interface KnowbaseLibraryFacade {

    LibraryResult createLibrary(CreateLibraryCommand command);

    LibraryResult getLibrary(UUID libraryId);

    List<LibraryResult> listLibraries(String tenantId);
}
