package com.knowbase.api.facade;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryCreatedResult;

import java.util.UUID;

public interface KnowbaseLibraryFacade {

    LibraryCreatedResult createLibrary(CreateLibraryCommand command);

    UUID getLibraryIdOrThrow(UUID libraryId);
}
