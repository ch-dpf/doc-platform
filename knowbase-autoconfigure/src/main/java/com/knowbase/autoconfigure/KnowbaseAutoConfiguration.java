package com.knowbase.autoconfigure;

import com.knowbase.agent.LibraryRouter;
import com.knowbase.agent.SelectedLibrariesRouter;
import com.knowbase.application.agent.AclAwareLibraryRouter;
import com.knowbase.api.spi.KnowbaseTenantResolver;
import com.knowbase.application.pipeline.DefaultQueryPipeline;
import com.knowbase.application.observability.DefaultPipelineObserver;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.application.service.DefaultAclService;
import com.knowbase.application.service.DefaultChatService;
import com.knowbase.application.service.DefaultDocumentChunkService;
import com.knowbase.application.service.DefaultDocumentService;
import com.knowbase.application.service.DefaultDocumentProfileService;
import com.knowbase.application.service.DefaultIndexGenerationRebuildService;
import com.knowbase.application.service.DefaultIndexVersionService;
import com.knowbase.application.service.IndexGenerationService;
import com.knowbase.application.service.DefaultAgentVersionService;
import com.knowbase.application.service.DefaultEvalService;
import com.knowbase.application.service.DefaultLibraryCatalogService;
import com.knowbase.application.service.DefaultLibraryIndexHealthService;
import com.knowbase.application.service.DefaultLibraryProfileService;
import com.knowbase.application.service.DefaultLibraryRetrievalTestService;
import com.knowbase.application.service.DefaultPromoteEvalGateService;
import com.knowbase.application.service.DefaultRetrievalEvalService;
import com.knowbase.application.service.RetrievalHitEvaluator;
import com.knowbase.application.service.DefaultObjectUploadService;
import com.knowbase.application.service.DefaultObservabilityService;
import com.knowbase.application.service.InMemoryAccessControlRepository;
import com.knowbase.application.service.InMemoryObservabilityRepository;
import com.knowbase.application.service.AsyncIngestionRunExecutor;
import com.knowbase.application.service.DefaultIngestionPrepareService;
import com.knowbase.application.service.DefaultIngestionPreviewService;
import com.knowbase.application.service.DefaultKnowledgeAgentService;
import com.knowbase.application.service.DefaultLibraryService;
import com.knowbase.application.usecase.PrepareIngestionUseCase;
import com.knowbase.application.usecase.PreviewIngestionUseCase;
import com.knowbase.application.usecase.ManageTokenizerProfileUseCase;
import com.knowbase.application.service.DefaultPresetService;
import com.knowbase.application.service.IngestionCatalogService;
import com.knowbase.application.service.DefaultQuestionService;
import com.knowbase.application.service.DefaultRetrievalTestService;
import com.knowbase.application.service.DefaultTokenizerProfileService;
import com.knowbase.application.service.InMemoryPresetRepository;
import com.knowbase.application.service.InMemoryKnowbaseRepository;
import com.knowbase.application.service.IngestionEvalDraftService;
import com.knowbase.application.service.IngestionRunExecutor;
import com.knowbase.application.service.SynchronousIngestionRunExecutor;
import com.knowbase.domain.audit.AuditSink;
import com.knowbase.domain.audit.NoopAuditSink;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.ingestion.ChunkPostProcessor;
import com.knowbase.ingestion.CompositeChunkPostProcessor;
import com.knowbase.ingestion.DocumentLlmSummaryGenerator;
import com.knowbase.ingestion.summary.DocumentSummaryPromptCatalog;
import com.knowbase.ingestion.summary.DocumentSummarySettings;
import com.knowbase.ingestion.LlmDocumentSummaryPostProcessor;
import com.knowbase.ingestion.StructuredTableChunkPostProcessor;
import com.knowbase.ingestion.DocxStructureParser;
import com.knowbase.ingestion.DefaultDocumentMetadataEnricher;
import com.knowbase.ingestion.DocumentPreparationPipeline;
import com.knowbase.ingestion.DocumentMetadataEnricher;
import com.knowbase.ingestion.DocumentNormalizer;
import com.knowbase.ingestion.DocumentTextNormalizer;
import com.knowbase.ingestion.ExternalDocumentParser;
import com.knowbase.ingestion.HtmlStructureParser;
import com.knowbase.ingestion.MarkdownStructureParser;
import com.knowbase.ingestion.OcrLayoutDocumentParser;
import com.knowbase.ingestion.OcrDocumentParser;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.PdfStructureParser;
import com.knowbase.ingestion.DocumentLlmSummaryRefresher;
import com.knowbase.ingestion.DefaultIngestionPipeline;
import com.knowbase.ingestion.DocumentSourceLoader;
import com.knowbase.ingestion.IngestionPipeline;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.AccessControlRepository;
import com.knowbase.domain.repository.ObservabilityRepository;
import com.knowbase.application.service.DefaultIngestionService;
import com.knowbase.application.usecase.ManagePresetUseCase;
import com.knowbase.ingestion.QaDocumentParser;
import com.knowbase.ingestion.ZipDocumentParser;
import com.knowbase.ingestion.StructuredTableDocumentParser;
import com.knowbase.storage.MinioObjectStorage;
import com.knowbase.storage.ObjectStorage;
import io.minio.MinioClient;
import com.knowbase.ingestion.TextDocumentParser;
import com.knowbase.ingestion.TextStructureParser;
import com.knowbase.ingestion.TokenBasedDocumentChunker;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.DeterministicChatModelClient;
import com.knowbase.model.DeterministicEmbeddingModelClient;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.domain.repository.PresetRepository;
import com.knowbase.preset.BuiltinPresetCatalog;
import com.knowbase.preset.CompositePresetCatalog;
import com.knowbase.preset.PresetCatalog;
import com.knowbase.retrieval.ContextPacker;
import com.knowbase.retrieval.DefaultContextPacker;
import com.knowbase.retrieval.DefaultEvidenceBuilder;
import com.knowbase.retrieval.DefaultRetrievalPlanner;
import com.knowbase.retrieval.DefaultRetrievalPostProcessor;
import com.knowbase.retrieval.ParentChildRetrievalExpander;
import com.knowbase.retrieval.EvidenceBuilder;
import com.knowbase.retrieval.InMemoryVectorRetriever;
import com.knowbase.agent.QuestionAnalyzer;
import com.knowbase.retrieval.RetrievalPlanner;
import com.knowbase.retrieval.RetrievalPostProcessor;
import com.knowbase.retrieval.Retriever;
import com.knowbase.storage.LocalFilesystemObjectStorage;
import com.knowbase.ingestion.TikaDocumentParser;
import com.knowbase.tokenizer.DefaultTokenWindowChunker;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;
import com.knowbase.tokenizer.TokenWindowChunker;
import com.knowbase.tokenizer.TokenizerGuard;
import com.knowbase.tokenizer.TokenizerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@AutoConfiguration
@EnableConfigurationProperties(KnowbaseProperties.class)
@ConditionalOnProperty(prefix = "knowbase", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowbaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    KnowbaseRepository inMemoryKnowbaseRepository() {
        return new InMemoryKnowbaseRepository();
    }

    @Bean
    @ConditionalOnMissingBean(KnowbaseTenantResolver.class)
    KnowbaseTenantResolver knowbaseTenantResolver(KnowbaseProperties properties) {
        return () -> properties.getTenant().getDefaultTenantId();
    }

    @Bean
    @ConditionalOnMissingBean(PresetRepository.class)
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    PresetRepository inMemoryPresetRepository() {
        return new InMemoryPresetRepository();
    }

    @Bean
    @ConditionalOnMissingBean(CompositePresetCatalog.class)
    CompositePresetCatalog presetCatalog(PresetRepository presetRepository) {
        return new CompositePresetCatalog(new BuiltinPresetCatalog(), presetRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    AuditSink auditSink() {
        return new NoopAuditSink();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.ollama", name = "enabled", havingValue = "false", matchIfMissing = true)
    TokenizerRegistry tokenizerRegistry() {
        return new DefaultTokenizerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    TokenWindowChunker tokenWindowChunker() {
        return new DefaultTokenWindowChunker();
    }

    @Bean
    @ConditionalOnMissingBean
    TokenizerGuard tokenizerGuard(KnowbaseProperties properties) {
        return new TokenizerGuard(properties.getTokenizer().isAllowApproximateForIndexing());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    AccessControlRepository inMemoryAccessControlRepository() {
        return new InMemoryAccessControlRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    ObservabilityRepository inMemoryObservabilityRepository() {
        return new InMemoryObservabilityRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    AccessControlService accessControlService(
            AccessControlRepository accessControlRepository,
            KnowbaseRepository repository,
            KnowbaseProperties properties
    ) {
        return new AccessControlService(
                accessControlRepository,
                repository,
                properties.getSecurity().isAclEnabled()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    PipelineObserver pipelineObserver(
            ObservabilityRepository observabilityRepository,
            KnowbaseRepository repository
    ) {
        return new DefaultPipelineObserver(observabilityRepository, repository);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorage.class)
    @ConditionalOnProperty(prefix = "knowbase.storage", name = "type", havingValue = "local", matchIfMissing = true)
    ObjectStorage localObjectStorage(KnowbaseProperties properties) {
        String localRoot = properties.getStorage().getLocalRoot();
        if (localRoot == null || localRoot.isBlank()) {
            return new LocalFilesystemObjectStorage();
        }
        return new LocalFilesystemObjectStorage(Paths.get(localRoot));
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorage.class)
    @ConditionalOnProperty(prefix = "knowbase.storage", name = "type", havingValue = "minio")
    ObjectStorage minioObjectStorage(KnowbaseProperties properties) {
        KnowbaseProperties.Minio minio = properties.getStorage().getMinio();
        MinioClient client = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
        return new MinioObjectStorage(client, minio.isAutoCreateBucket());
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultObjectUploadService defaultObjectUploadService(ObjectStorage objectStorage, KnowbaseProperties properties) {
        return new DefaultObjectUploadService(
                objectStorage,
                properties.getStorage().getDefaultBucket(),
                properties.getUpload().getMaxFilesPerBatch(),
                properties.getUpload().getMaxFileSizeBytes()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultAclService defaultAclService(AccessControlRepository accessControlRepository, AccessControlService accessControlService) {
        return new DefaultAclService(accessControlRepository, accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultLibraryCatalogService defaultLibraryCatalogService(KnowbaseRepository repository, AccessControlService accessControlService) {
        return new DefaultLibraryCatalogService(repository, accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultAgentVersionService defaultAgentVersionService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService
    ) {
        return new DefaultAgentVersionService(repository, presetCatalog, accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultObservabilityService defaultObservabilityService(ObservabilityRepository observabilityRepository) {
        return new DefaultObservabilityService(observabilityRepository);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultEvalService.class)
    DefaultEvalService defaultEvalService(
            ObservabilityRepository observabilityRepository,
            KnowbaseRepository repository,
            DefaultQueryPipeline queryPipeline
    ) {
        return new DefaultEvalService(observabilityRepository, repository, queryPipeline);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.ollama", name = "enabled", havingValue = "false", matchIfMissing = true)
    EmbeddingModelClient embeddingModelClient() {
        return new DeterministicEmbeddingModelClient("ollama", "bge-m3", 1024);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.ollama", name = "enabled", havingValue = "false", matchIfMissing = true)
    ChatModelClient chatModelClient() {
        return new DeterministicChatModelClient("ollama", "llama3.2");
    }

    @Bean
    @ConditionalOnMissingBean
    MarkdownStructureParser markdownStructureParser() {
        return new MarkdownStructureParser();
    }

    @Bean
    @ConditionalOnMissingBean
    HtmlStructureParser htmlStructureParser() {
        return new HtmlStructureParser();
    }

    @Bean
    @ConditionalOnMissingBean
    DocxStructureParser docxStructureParser() {
        return new DocxStructureParser();
    }

    @Bean
    @ConditionalOnMissingBean
    PdfLayoutParser pdfLayoutParser() {
        return new PdfLayoutParser();
    }

    @Bean
    @ConditionalOnMissingBean
    PdfStructureParser pdfStructureParser() {
        return new PdfStructureParser();
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentTextNormalizer documentTextNormalizer() {
        return new DocumentTextNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentMetadataEnricher documentMetadataEnricher() {
        return new DefaultDocumentMetadataEnricher();
    }

    @Bean
    @ConditionalOnMissingBean
    StructuredTableChunkPostProcessor structuredTableChunkPostProcessor() {
        return new StructuredTableChunkPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentSummarySettings documentSummarySettings(KnowbaseProperties properties) {
        KnowbaseProperties.Summary summary = properties.getIngestion().getSummary();
        return new DocumentSummarySettings(
                summary.getPromptId(),
                summary.getLanguage(),
                summary.getMaxInputChars(),
                summary.getMaxOutputChars(),
                summary.getMinInputChars(),
                summary.getTemperature(),
                summary.getMaxCompletionTokens()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentLlmSummaryGenerator documentLlmSummaryGenerator(
            ChatModelClient chatModelClient,
            DocumentSummarySettings documentSummarySettings
    ) {
        return new DocumentLlmSummaryGenerator(
                chatModelClient,
                new DocumentSummaryPromptCatalog(),
                documentSummarySettings
        );
    }

    @Bean
    @ConditionalOnMissingBean
    LlmDocumentSummaryPostProcessor llmDocumentSummaryPostProcessor(DocumentLlmSummaryGenerator documentLlmSummaryGenerator) {
        return new LlmDocumentSummaryPostProcessor(documentLlmSummaryGenerator);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "chunkPostProcessor")
    ChunkPostProcessor chunkPostProcessor(StructuredTableChunkPostProcessor structuredTableChunkPostProcessor) {
        return CompositeChunkPostProcessor.of(structuredTableChunkPostProcessor);
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentLlmSummaryRefresher documentLlmSummaryRefresher(
            KnowbaseRepository repository,
            DocumentLlmSummaryGenerator documentLlmSummaryGenerator,
            LlmDocumentSummaryPostProcessor llmDocumentSummaryPostProcessor,
            EmbeddingModelClient embeddingModelClient,
            PipelineObserver pipelineObserver
    ) {
        return new DocumentLlmSummaryRefresher(
                repository,
                documentLlmSummaryGenerator,
                llmDocumentSummaryPostProcessor,
                embeddingModelClient,
                pipelineObserver
        );
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentPreparationPipeline documentPreparationPipeline(
            DocumentSourceLoader documentSourceLoader,
            DocumentNormalizer documentNormalizer,
            TokenBasedDocumentChunker tokenBasedDocumentChunker,
            DocumentMetadataEnricher documentMetadataEnricher,
            ChunkPostProcessor chunkPostProcessor,
            DocumentLlmSummaryGenerator documentLlmSummaryGenerator,
            PipelineObserver pipelineObserver
    ) {
        return new DocumentPreparationPipeline(
                documentSourceLoader,
                documentNormalizer,
                tokenBasedDocumentChunker,
                documentMetadataEnricher,
                chunkPostProcessor,
                documentLlmSummaryGenerator,
                pipelineObserver
        );
    }

    @Bean
    @ConditionalOnMissingBean
    StructuredTableDocumentParser structuredTableDocumentParser() {
        return new StructuredTableDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    OcrLayoutDocumentParser ocrLayoutDocumentParser() {
        return new OcrLayoutDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    OcrDocumentParser ocrDocumentParser() {
        return new OcrDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    TextStructureParser textStructureParser() {
        return new TextStructureParser();
    }

    @Bean
    @ConditionalOnMissingBean
    TextDocumentParser textDocumentParser() {
        return new TextDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    TikaDocumentParser tikaDocumentParser() {
        return new TikaDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    ExternalDocumentParser externalDocumentParser() {
        return new ExternalDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    QaDocumentParser qaDocumentParser() {
        return new QaDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    ZipDocumentParser zipDocumentParser() {
        return new ZipDocumentParser();
    }

    @Bean
    @ConditionalOnMissingBean
    DocumentSourceLoader documentSourceLoader(
            ObjectStorage objectStorage,
            MarkdownStructureParser markdownStructureParser,
            HtmlStructureParser htmlStructureParser,
            DocxStructureParser docxStructureParser,
            PdfLayoutParser pdfLayoutParser,
            PdfStructureParser pdfStructureParser,
            TextStructureParser textStructureParser,
            TextDocumentParser textDocumentParser,
            QaDocumentParser qaDocumentParser,
            ZipDocumentParser zipDocumentParser,
            StructuredTableDocumentParser structuredTableDocumentParser,
            OcrLayoutDocumentParser ocrLayoutDocumentParser,
            OcrDocumentParser ocrDocumentParser,
            ExternalDocumentParser externalDocumentParser,
            TikaDocumentParser tikaDocumentParser
    ) {
        return new DocumentSourceLoader(
                objectStorage,
                java.util.List.of(
                        markdownStructureParser,
                        htmlStructureParser,
                        docxStructureParser,
                        pdfLayoutParser,
                        pdfStructureParser,
                        textStructureParser,
                        qaDocumentParser,
                        zipDocumentParser,
                        textDocumentParser,
                        structuredTableDocumentParser,
                        ocrLayoutDocumentParser,
                        ocrDocumentParser,
                        externalDocumentParser,
                        tikaDocumentParser
                )
        );
    }

    @Bean
    @ConditionalOnMissingBean
    TokenBasedDocumentChunker tokenBasedDocumentChunker(
            TokenizerRegistry tokenizerRegistry,
            TokenWindowChunker tokenWindowChunker,
            TokenizerGuard tokenizerGuard
    ) {
        return new TokenBasedDocumentChunker(tokenizerRegistry, tokenWindowChunker, tokenizerGuard);
    }

    @Bean
    @ConditionalOnMissingBean
    IndexGenerationService indexGenerationService(KnowbaseRepository repository) {
        return new IndexGenerationService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    IngestionPipeline ingestionPipeline(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver,
            IndexGenerationService indexGenerationService,
            KnowbaseProperties properties
    ) {
        return new DefaultIngestionPipeline(
                repository,
                documentPreparationPipeline,
                embeddingModelClient,
                tokenizerRegistry,
                pipelineObserver,
                indexGenerationService::ensureActiveGeneration,
                indexGenerationService::refreshGenerationStats,
                properties.getIngestion().isDocumentUpsertEnabled(),
                null,
                null
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "knowbaseSummaryExecutor")
    Executor knowbaseSummaryExecutor() {
        return Executors.newFixedThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("knowbase-summary-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    @ConditionalOnMissingBean(IngestionRunExecutor.class)
    @ConditionalOnProperty(prefix = "knowbase.ingestion", name = "async-enabled", havingValue = "false", matchIfMissing = true)
    IngestionRunExecutor synchronousIngestionRunExecutor(
            IngestionPipeline ingestionPipeline,
            IngestionEvalDraftService evalDraftService
    ) {
        return new SynchronousIngestionRunExecutor(ingestionPipeline, evalDraftService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "knowbaseIngestionExecutor")
    @ConditionalOnProperty(prefix = "knowbase.ingestion", name = "async-enabled", havingValue = "true")
    Executor knowbaseIngestionExecutor(KnowbaseProperties properties) {
        int poolSize = Math.max(1, properties.getIngestion().getAsyncPoolSize());
        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("knowbase-ingestion-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    @ConditionalOnMissingBean(IngestionRunExecutor.class)
    @ConditionalOnProperty(prefix = "knowbase.ingestion", name = "async-enabled", havingValue = "true")
    IngestionRunExecutor asyncIngestionRunExecutor(
            KnowbaseRepository repository,
            IngestionPipeline ingestionPipeline,
            AuditSink auditSink,
            @Qualifier("knowbaseIngestionExecutor") Executor knowbaseIngestionExecutor,
            IngestionEvalDraftService evalDraftService
    ) {
        return new AsyncIngestionRunExecutor(repository, ingestionPipeline, auditSink, knowbaseIngestionExecutor, evalDraftService);
    }

    @Bean
    @ConditionalOnMissingBean
    LibraryRouter libraryRouter(AccessControlService accessControlService) {
        return new AclAwareLibraryRouter(new SelectedLibrariesRouter(), accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean
    ParentChildRetrievalExpander parentChildRetrievalExpander(KnowbaseRepository repository) {
        return new ParentChildRetrievalExpander(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    RetrievalPostProcessor retrievalPostProcessor(ParentChildRetrievalExpander parentChildRetrievalExpander) {
        return new DefaultRetrievalPostProcessor(parentChildRetrievalExpander);
    }

    @Bean
    @ConditionalOnMissingBean
    QuestionAnalyzer questionAnalyzer() {
        return new com.knowbase.agent.DefaultQuestionAnalyzer();
    }

    @Bean
    @ConditionalOnMissingBean
    RetrievalPlanner retrievalPlanner() {
        return new DefaultRetrievalPlanner();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "knowbase.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    Retriever inMemoryVectorRetriever(
            KnowbaseRepository repository,
            EmbeddingModelClient embeddingModelClient
    ) {
        return new InMemoryVectorRetriever(repository, embeddingModelClient);
    }

    @Bean
    @ConditionalOnMissingBean
    EvidenceBuilder evidenceBuilder() {
        return new DefaultEvidenceBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    ContextPacker contextPacker() {
        return new DefaultContextPacker();
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultQueryPipeline defaultQueryPipeline(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            QuestionAnalyzer questionAnalyzer,
            RetrievalPlanner retrievalPlanner,
            Retriever retriever,
            RetrievalPostProcessor retrievalPostProcessor,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver,
            AccessControlService accessControlService
    ) {
        return new DefaultQueryPipeline(
                repository,
                libraryRouter,
                questionAnalyzer,
                retrievalPlanner,
                retriever,
                retrievalPostProcessor,
                evidenceBuilder,
                contextPacker,
                chatModelClient,
                tokenizerRegistry,
                pipelineObserver,
                accessControlService
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultLibraryService.class)
    DefaultLibraryService defaultLibraryService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService
    ) {
        return new DefaultLibraryService(repository, presetCatalog, accessControlService, indexGenerationService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDocumentService.class)
    DefaultDocumentService defaultDocumentService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            DefaultIngestionService ingestionService,
            IndexGenerationService indexGenerationService,
            DefaultObjectUploadService uploadService,
            DocumentSourceLoader documentSourceLoader
    ) {
        return new DefaultDocumentService(
                repository,
                accessControlService,
                ingestionService,
                indexGenerationService,
                uploadService,
                documentSourceLoader
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDocumentChunkService.class)
    DefaultDocumentChunkService defaultDocumentChunkService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry
    ) {
        return new DefaultDocumentChunkService(
                repository,
                accessControlService,
                embeddingModelClient,
                tokenizerRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultIndexGenerationRebuildService.class)
    DefaultIndexGenerationRebuildService defaultIndexGenerationRebuildService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService,
            DefaultIngestionService ingestionService,
            DefaultIndexVersionService indexVersionService
    ) {
        return new DefaultIndexGenerationRebuildService(
                repository,
                accessControlService,
                indexGenerationService,
                ingestionService,
                indexVersionService
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultIngestionPreviewService.class)
    DefaultIngestionPreviewService defaultIngestionPreviewService(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            TokenizerRegistry tokenizerRegistry
    ) {
        return new DefaultIngestionPreviewService(
                repository,
                documentPreparationPipeline,
                tokenizerRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultIngestionPrepareService.class)
    DefaultIngestionPrepareService defaultIngestionPrepareService(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            TokenizerRegistry tokenizerRegistry
    ) {
        return new DefaultIngestionPrepareService(
                repository,
                documentPreparationPipeline,
                tokenizerRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean(PrepareIngestionUseCase.class)
    PrepareIngestionUseCase prepareIngestionUseCase(DefaultIngestionPrepareService defaultIngestionPrepareService) {
        return defaultIngestionPrepareService;
    }

    @Bean
    @ConditionalOnMissingBean(PreviewIngestionUseCase.class)
    PreviewIngestionUseCase previewIngestionUseCase(DefaultIngestionPreviewService defaultIngestionPreviewService) {
        return defaultIngestionPreviewService;
    }

    @Bean
    @ConditionalOnMissingBean(DefaultIngestionService.class)
    DefaultIngestionService defaultIngestionService(
            KnowbaseRepository repository,
            IngestionRunExecutor ingestionRunExecutor,
            AuditSink auditSink
    ) {
        return new DefaultIngestionService(repository, ingestionRunExecutor, auditSink);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultKnowledgeAgentService.class)
    DefaultKnowledgeAgentService defaultKnowledgeAgentService(
            KnowbaseRepository repository,
            PresetCatalog presetCatalog,
            AccessControlService accessControlService
    ) {
        return new DefaultKnowledgeAgentService(repository, presetCatalog, accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultChatService.class)
    DefaultChatService defaultChatService(KnowbaseRepository repository, DefaultQueryPipeline queryPipeline) {
        return new DefaultChatService(repository, queryPipeline);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultLibraryIndexHealthService.class)
    DefaultLibraryIndexHealthService defaultLibraryIndexHealthService(
            KnowbaseRepository repository,
            IndexGenerationService indexGenerationService,
            DefaultPromoteEvalGateService promoteEvalGateService
    ) {
        return new DefaultLibraryIndexHealthService(repository, indexGenerationService, promoteEvalGateService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultLibraryRetrievalTestService.class)
    DefaultLibraryRetrievalTestService defaultLibraryRetrievalTestService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            QuestionAnalyzer questionAnalyzer,
            RetrievalPlanner retrievalPlanner,
            Retriever retriever,
            RetrievalPostProcessor retrievalPostProcessor,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry
    ) {
        return new DefaultLibraryRetrievalTestService(
                repository,
                accessControlService,
                questionAnalyzer,
                retrievalPlanner,
                retriever,
                retrievalPostProcessor,
                evidenceBuilder,
                contextPacker,
                chatModelClient,
                tokenizerRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean(RetrievalHitEvaluator.class)
    RetrievalHitEvaluator retrievalHitEvaluator(KnowbaseRepository repository) {
        return new RetrievalHitEvaluator(repository);
    }

    @Bean
    @ConditionalOnMissingBean(IngestionEvalDraftService.class)
    IngestionEvalDraftService ingestionEvalDraftService(KnowbaseRepository repository) {
        return new IngestionEvalDraftService(repository);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultRetrievalEvalService.class)
    DefaultRetrievalEvalService defaultRetrievalEvalService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            DefaultLibraryRetrievalTestService libraryRetrievalTestService,
            RetrievalHitEvaluator hitEvaluator,
            IngestionEvalDraftService evalDraftService
    ) {
        return new DefaultRetrievalEvalService(
                repository,
                accessControlService,
                libraryRetrievalTestService,
                hitEvaluator,
                evalDraftService
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultPromoteEvalGateService.class)
    DefaultPromoteEvalGateService defaultPromoteEvalGateService(
            KnowbaseRepository repository,
            DefaultRetrievalEvalService retrievalEvalService,
            KnowbaseProperties properties
    ) {
        return new DefaultPromoteEvalGateService(
                repository,
                retrievalEvalService,
                properties.getIngestion().isPromoteEvalGateEnabled()
        );
    }

    @Bean
    @ConditionalOnMissingBean(DefaultDocumentProfileService.class)
    DefaultDocumentProfileService defaultDocumentProfileService(
            KnowbaseRepository repository,
            AccessControlService accessControlService
    ) {
        return new DefaultDocumentProfileService(repository, accessControlService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultLibraryProfileService.class)
    DefaultLibraryProfileService defaultLibraryProfileService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            DefaultLibraryIndexHealthService indexHealthService
    ) {
        return new DefaultLibraryProfileService(repository, accessControlService, indexHealthService);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultIndexVersionService.class)
    DefaultIndexVersionService defaultIndexVersionService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            IndexGenerationService indexGenerationService,
            DefaultLibraryIndexHealthService indexHealthService
    ) {
        return new DefaultIndexVersionService(repository, accessControlService, indexGenerationService, indexHealthService);
    }

    @Bean
    @ConditionalOnMissingBean(IngestionCatalogService.class)
    IngestionCatalogService ingestionCatalogService(CompositePresetCatalog presetCatalog) {
        return new IngestionCatalogService(presetCatalog);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultPresetService.class)
    DefaultPresetService defaultPresetService(
            CompositePresetCatalog presetCatalog,
            PresetRepository presetRepository,
            IngestionCatalogService ingestionCatalogService
    ) {
        return new DefaultPresetService(presetCatalog, presetRepository, ingestionCatalogService);
    }

    @Bean
    @ConditionalOnMissingBean(ManagePresetUseCase.class)
    ManagePresetUseCase managePresetUseCase(DefaultPresetService defaultPresetService) {
        return defaultPresetService;
    }

    @Bean
    @ConditionalOnMissingBean(ManageTokenizerProfileUseCase.class)
    ManageTokenizerProfileUseCase manageTokenizerProfileUseCase(KnowbaseRepository repository) {
        return new DefaultTokenizerProfileService(repository);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultQuestionService.class)
    DefaultQuestionService defaultQuestionService(
            KnowbaseRepository repository,
            DefaultQueryPipeline queryPipeline,
            AuditSink auditSink
    ) {
        return new DefaultQuestionService(repository, queryPipeline, auditSink);
    }

    @Bean
    @ConditionalOnMissingBean(DefaultRetrievalTestService.class)
    DefaultRetrievalTestService defaultRetrievalTestService(
            KnowbaseRepository repository,
            LibraryRouter libraryRouter,
            QuestionAnalyzer questionAnalyzer,
            RetrievalPlanner retrievalPlanner,
            Retriever retriever,
            RetrievalPostProcessor retrievalPostProcessor,
            EvidenceBuilder evidenceBuilder,
            ContextPacker contextPacker,
            ChatModelClient chatModelClient,
            TokenizerRegistry tokenizerRegistry,
            AccessControlService accessControlService
    ) {
        return new DefaultRetrievalTestService(
                repository,
                libraryRouter,
                questionAnalyzer,
                retrievalPlanner,
                retriever,
                retrievalPostProcessor,
                evidenceBuilder,
                contextPacker,
                chatModelClient,
                tokenizerRegistry,
                accessControlService
        );
    }
}
