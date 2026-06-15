package com.knowbase.autoconfigure;

import com.knowbase.api.facade.KnowbaseIngestFacade;
import com.knowbase.api.facade.KnowbaseLibraryFacade;
import com.knowbase.api.facade.KnowbaseRagFacade;
import com.knowbase.api.facade.KnowbaseSearchFacade;
import com.knowbase.api.spi.KnowbaseTenantResolver;
import com.knowbase.facade.KnowbaseIngestFacadeImpl;
import com.knowbase.facade.KnowbaseLibraryFacadeImpl;
import com.knowbase.facade.KnowbaseRagFacadeImpl;
import com.knowbase.facade.KnowbaseSearchFacadeImpl;
import com.knowbase.facade.KnowbaseTenantSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class KnowbaseFacadeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KnowbaseTenantSupport knowbaseTenantSupport(Optional<KnowbaseTenantResolver> tenantResolver) {
        return new KnowbaseTenantSupport(tenantResolver.orElse(null));
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseRagFacade.class)
    public KnowbaseRagFacade knowbaseRagFacade(
            com.knowbase.vector.service.RagService ragService, KnowbaseTenantSupport tenantSupport) {
        return new KnowbaseRagFacadeImpl(ragService, tenantSupport);
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseSearchFacade.class)
    public KnowbaseSearchFacade knowbaseSearchFacade(
            com.knowbase.vector.service.VectorSearchService searchService, KnowbaseTenantSupport tenantSupport) {
        return new KnowbaseSearchFacadeImpl(searchService, tenantSupport);
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseLibraryFacade.class)
    public KnowbaseLibraryFacade knowbaseLibraryFacade(
            com.knowbase.library.service.VectorLibraryService libraryService,
            com.knowbase.library.service.LibraryConfigResolver libraryConfigResolver,
            KnowbaseTenantSupport tenantSupport) {
        return new KnowbaseLibraryFacadeImpl(libraryService, libraryConfigResolver, tenantSupport);
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseIngestFacade.class)
    public KnowbaseIngestFacade knowbaseIngestFacade(
            com.knowbase.ingest.service.DocumentIngestor documentIngestor,
            com.knowbase.ingest.service.DocumentQueryService documentQueryService,
            com.knowbase.library.service.LibraryConfigResolver libraryConfigResolver,
            KnowbaseTenantSupport tenantSupport) {
        return new KnowbaseIngestFacadeImpl(
                documentIngestor, documentQueryService, libraryConfigResolver, tenantSupport);
    }
}
