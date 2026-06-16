package com.knowbase.facade;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.facade.KnowbaseLibraryFacade;
import com.knowbase.api.result.LibraryCreatedResult;
import com.knowbase.library.dto.CreateVectorLibraryRequest;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.library.service.VectorLibraryService;

import java.util.UUID;

public class KnowbaseLibraryFacadeImpl implements KnowbaseLibraryFacade {

    private final VectorLibraryService libraryService;
    private final LibraryConfigResolver libraryConfigResolver;
    private final KnowbaseTenantSupport tenantSupport;

    public KnowbaseLibraryFacadeImpl(
            VectorLibraryService libraryService,
            LibraryConfigResolver libraryConfigResolver,
            KnowbaseTenantSupport tenantSupport) {
        this.libraryService = libraryService;
        this.libraryConfigResolver = libraryConfigResolver;
        this.tenantSupport = tenantSupport;
    }

    @Override
    public LibraryCreatedResult createLibrary(CreateLibraryCommand command) {
        String tenantId = tenantSupport.resolve(command.tenantId());
        VectorLibraryResponse created = libraryService.create(new CreateVectorLibraryRequest(
                tenantId, command.name(), command.description(), command.tags(), null, null, null));
        return new LibraryCreatedResult(created.libraryId(), created.tenantId(), created.name());
    }

    @Override
    public UUID getLibraryIdOrThrow(UUID libraryId) {
        libraryConfigResolver.requireLibrary(libraryId);
        return libraryId;
    }
}
