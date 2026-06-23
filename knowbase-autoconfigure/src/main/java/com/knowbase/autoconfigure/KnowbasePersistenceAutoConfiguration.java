package com.knowbase.autoconfigure;

import com.knowbase.domain.audit.AuditSink;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.persistence.config.KnowbasePersistenceConfiguration;
import com.knowbase.persistence.mapper.AgentMapper;
import com.knowbase.persistence.mapper.AgentVersionMapper;
import com.knowbase.persistence.mapper.ChunkMapper;
import com.knowbase.persistence.mapper.DocumentMapper;
import com.knowbase.persistence.mapper.DocumentIndexJobMapper;
import com.knowbase.persistence.mapper.DocumentProfileMapper;
import com.knowbase.persistence.mapper.IndexVersionMapper;
import com.knowbase.persistence.mapper.IngestionRunMapper;
import com.knowbase.persistence.mapper.LibraryMapper;
import com.knowbase.persistence.mapper.LibraryProfileMapper;
import com.knowbase.persistence.mapper.QueryRunMapper;
import com.knowbase.persistence.mapper.TokenizerProfileMapper;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.repository.ObservabilityRepository;
import com.knowbase.persistence.repository.PostgresAccessControlRepository;
import com.knowbase.persistence.repository.PostgresObservabilityRepository;
import com.knowbase.persistence.repository.PostgresKnowbaseRepository;
import com.knowbase.persistence.repository.PostgresPresetRepository;
import com.knowbase.domain.repository.PresetRepository;
import com.knowbase.persistence.retrieval.PgVectorRetriever;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.store.AuditEventStore;
import com.knowbase.retrieval.RetrievalPostProcessor;
import com.knowbase.retrieval.Retriever;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({JdbcTemplate.class, LibraryMapper.class})
@ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "true")
@Import(KnowbasePersistenceConfiguration.class)
public class KnowbasePersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EmbeddingStore embeddingStore(JdbcTemplate jdbcTemplate) {
        return new EmbeddingStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(AuditSink.class)
    AuditSink auditSink(JdbcTemplate jdbcTemplate) {
        return new AuditEventStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(AccessControlRepository.class)
    AccessControlRepository postgresAccessControlRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresAccessControlRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ObservabilityRepository.class)
    ObservabilityRepository postgresObservabilityRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresObservabilityRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(PresetRepository.class)
    PresetRepository postgresPresetRepository(JdbcTemplate jdbcTemplate) {
        return new PostgresPresetRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseRepository.class)
    KnowbaseRepository postgresKnowbaseRepository(
            LibraryMapper libraryMapper,
            LibraryProfileMapper libraryProfileMapper,
            TokenizerProfileMapper tokenizerProfileMapper,
            DocumentProfileMapper documentProfileMapper,
            IndexVersionMapper indexVersionMapper,
            IngestionRunMapper ingestionRunMapper,
            DocumentIndexJobMapper documentIndexJobMapper,
            DocumentMapper documentMapper,
            ChunkMapper chunkMapper,
            AgentMapper agentMapper,
            AgentVersionMapper agentVersionMapper,
            QueryRunMapper queryRunMapper,
            EmbeddingStore embeddingStore,
            JdbcTemplate jdbcTemplate
    ) {
        return new PostgresKnowbaseRepository(
                libraryMapper,
                libraryProfileMapper,
                tokenizerProfileMapper,
                documentProfileMapper,
                indexVersionMapper,
                ingestionRunMapper,
                documentIndexJobMapper,
                documentMapper,
                chunkMapper,
                agentMapper,
                agentVersionMapper,
                queryRunMapper,
                embeddingStore,
                jdbcTemplate
        );
    }

    @Bean
    @ConditionalOnMissingBean(Retriever.class)
    Retriever pgVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient,
            EmbeddingStore embeddingStore
    ) {
        return new PgVectorRetriever(repository, embeddingModelClient, embeddingStore);
    }
}
