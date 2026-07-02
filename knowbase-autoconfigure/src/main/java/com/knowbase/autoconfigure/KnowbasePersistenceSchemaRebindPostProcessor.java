package com.knowbase.autoconfigure;

import com.knowbase.persistence.jdbc.KnowbaseSchemaJdbcTemplate;
import com.knowbase.persistence.jdbc.SchemaAwareJdbcTemplate;
import com.knowbase.persistence.mapper.AgentMapper;
import com.knowbase.persistence.mapper.AgentVersionMapper;
import com.knowbase.persistence.mapper.ChunkMapper;
import com.knowbase.persistence.mapper.DocumentIndexJobMapper;
import com.knowbase.persistence.mapper.DocumentMapper;
import com.knowbase.persistence.mapper.DocumentProfileMapper;
import com.knowbase.persistence.mapper.IndexVersionMapper;
import com.knowbase.persistence.mapper.IngestionRunMapper;
import com.knowbase.persistence.mapper.LibraryMapper;
import com.knowbase.persistence.mapper.LibraryProfileMapper;
import com.knowbase.persistence.mapper.QueryRunMapper;
import com.knowbase.persistence.mapper.TokenizerProfileMapper;
import com.knowbase.persistence.repository.PostgresAccessControlRepository;
import com.knowbase.persistence.repository.PostgresKnowbaseRepository;
import com.knowbase.persistence.repository.PostgresObservabilityRepository;
import com.knowbase.persistence.repository.PostgresPresetRepository;
import com.knowbase.persistence.store.AuditEventStore;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.support.KnowbaseSchemaSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rebinds KnowBase persistence beans that capture {@code knowbaseJdbcTemplate} at construction time.
 * Host apps may replace the template bean after auto-configuration; without rebinding, raw SQL
 * would target unqualified {@code kb_*} tables outside the configured schema.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
final class KnowbasePersistenceSchemaRebindPostProcessor implements BeanPostProcessor {

    private final KnowbaseSchemaSupport schemaSupport;
    private final ObjectProvider<JdbcTemplate> knowbaseJdbcTemplateProvider;
    private final LibraryMapper libraryMapper;
    private final LibraryProfileMapper libraryProfileMapper;
    private final TokenizerProfileMapper tokenizerProfileMapper;
    private final DocumentProfileMapper documentProfileMapper;
    private final IndexVersionMapper indexVersionMapper;
    private final IngestionRunMapper ingestionRunMapper;
    private final DocumentIndexJobMapper documentIndexJobMapper;
    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final QueryRunMapper queryRunMapper;

    KnowbasePersistenceSchemaRebindPostProcessor(
            KnowbaseSchemaSupport schemaSupport,
            @Qualifier("knowbaseJdbcTemplate") ObjectProvider<JdbcTemplate> knowbaseJdbcTemplateProvider,
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
            QueryRunMapper queryRunMapper
    ) {
        this.schemaSupport = schemaSupport;
        this.knowbaseJdbcTemplateProvider = knowbaseJdbcTemplateProvider;
        this.libraryMapper = libraryMapper;
        this.libraryProfileMapper = libraryProfileMapper;
        this.tokenizerProfileMapper = tokenizerProfileMapper;
        this.documentProfileMapper = documentProfileMapper;
        this.indexVersionMapper = indexVersionMapper;
        this.ingestionRunMapper = ingestionRunMapper;
        this.documentIndexJobMapper = documentIndexJobMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.agentMapper = agentMapper;
        this.agentVersionMapper = agentVersionMapper;
        this.queryRunMapper = queryRunMapper;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!schemaSupport.hasSchema() || !isRebindTarget(bean)) {
            return bean;
        }
        JdbcTemplate jdbcTemplate = resolveSchemaAwareTemplate();
        if (jdbcTemplate == null) {
            return bean;
        }
        if (bean instanceof PostgresPresetRepository) {
            return new PostgresPresetRepository(jdbcTemplate);
        }
        if (bean instanceof PostgresAccessControlRepository) {
            return new PostgresAccessControlRepository(jdbcTemplate);
        }
        if (bean instanceof PostgresObservabilityRepository) {
            return new PostgresObservabilityRepository(jdbcTemplate);
        }
        if (bean instanceof EmbeddingStore) {
            return new EmbeddingStore(jdbcTemplate);
        }
        if (bean instanceof AuditEventStore) {
            return new AuditEventStore(jdbcTemplate);
        }
        if (bean instanceof PostgresKnowbaseRepository) {
            EmbeddingStore embeddingStore = new EmbeddingStore(jdbcTemplate);
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
        return bean;
    }

    private static boolean isRebindTarget(Object bean) {
        return bean instanceof PostgresPresetRepository
                || bean instanceof PostgresAccessControlRepository
                || bean instanceof PostgresObservabilityRepository
                || bean instanceof EmbeddingStore
                || bean instanceof AuditEventStore
                || bean instanceof PostgresKnowbaseRepository;
    }

    private JdbcTemplate resolveSchemaAwareTemplate() {
        JdbcTemplate template = knowbaseJdbcTemplateProvider.getIfAvailable();
        if (template == null) {
            return null;
        }
        if (template instanceof KnowbaseSchemaJdbcTemplate) {
            return template;
        }
        return new SchemaAwareJdbcTemplate(template.getDataSource(), schemaSupport);
    }
}
