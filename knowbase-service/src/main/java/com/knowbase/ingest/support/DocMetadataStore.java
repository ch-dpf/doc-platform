package com.knowbase.ingest.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.dto.DocumentListQuery;
import com.knowbase.ingest.mapper.DocMetadataMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocMetadataStore {

    private final DocMetadataMapper mapper;

    public DocMetadataStore(DocMetadataMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<DocMetadata> findById(UUID docId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getDocId, docId)));
    }

    public Optional<DocMetadata> findByLibraryTenantChecksum(
            UUID libraryId, String tenantId, String checksumSha256) {
        return Optional.ofNullable(
                mapper.findByLibraryTenantChecksum(libraryId, tenantId, checksumSha256));
    }

    public Optional<DocMetadata> findByLibraryTenantSourceUrl(
            UUID libraryId, String tenantId, String sourceUrl) {
        return Optional.ofNullable(mapper.findByLibraryTenantSourceUrl(libraryId, tenantId, sourceUrl));
    }

    public Optional<DocMetadata> findByDocIdAndDeletedFalse(UUID docId) {
        return Optional.ofNullable(mapper.findByDocIdAndDeletedFalse(docId));
    }

    public IPage<DocMetadata> list(DocumentListQuery query) {
        Page<DocMetadata> page = new Page<>(query.page(), query.size());
        LambdaQueryWrapper<DocMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocMetadata::getTenantId, query.tenantId());
        if (query.libraryId() != null) {
            wrapper.eq(DocMetadata::getLibraryId, query.libraryId());
        }
        wrapper.eq(DocMetadata::isDeleted, false);
        if (query.sourceType() != null) {
            wrapper.eq(DocMetadata::getSourceType, query.sourceType());
        }
        if (query.parseStatus() != null) {
            wrapper.eq(DocMetadata::getParseStatus, query.parseStatus());
        }
        if (query.indexStatus() != null) {
            wrapper.eq(DocMetadata::getIndexStatus, query.indexStatus());
        }
        if (StringUtils.hasText(query.keyword())) {
            String pattern = "%" + query.keyword().trim() + "%";
            wrapper.and(w -> w.like(DocMetadata::getFileName, pattern)
                    .or()
                    .like(DocMetadata::getSourceUrl, pattern));
        }
        wrapper.orderByDesc(DocMetadata::getUpdatedAt);
        return mapper.selectPage(page, wrapper);
    }

    public List<DocMetadata> findParsedWithTextKey(UUID libraryId, String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId)
                .eq(DocMetadata::isDeleted, false)
                .eq(DocMetadata::getParseStatus, ParseStatus.PARSED)
                .isNotNull(DocMetadata::getParsedTextKey)
                .ne(DocMetadata::getParsedTextKey, "")
                .orderByDesc(DocMetadata::getUpdatedAt));
    }

    public void deleteByDocId(UUID docId) {
        mapper.delete(new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getDocId, docId));
    }

    public int countActiveByLibraryId(UUID libraryId) {
        return mapper.countActiveByLibraryId(libraryId);
    }

    public List<DocMetadata> findActiveByLibrary(UUID libraryId, String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId)
                .eq(DocMetadata::isDeleted, false)
                .orderByDesc(DocMetadata::getUpdatedAt));
    }

    public List<String> findActiveFileNamesByLibrary(UUID libraryId, String tenantId) {
        List<DocMetadata> docs = mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId)
                .eq(DocMetadata::isDeleted, false)
                .select(DocMetadata::getFileName));
        return docs.stream()
                .map(DocMetadata::getFileName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    public Map<UUID, String> findFileNamesByDocIds(Collection<UUID> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return Map.of();
        }
        List<DocMetadata> docs = mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .in(DocMetadata::getDocId, docIds)
                .eq(DocMetadata::isDeleted, false));
        Map<UUID, String> result = new java.util.HashMap<>();
        for (DocMetadata doc : docs) {
            if (doc.getFileName() != null && !doc.getFileName().isBlank()) {
                result.put(doc.getDocId(), doc.getFileName());
            }
        }
        return result;
    }

    public long sumSizeBytesByLibraryId(UUID libraryId) {
        return mapper.sumSizeBytesByLibraryId(libraryId);
    }

    public void save(DocMetadata doc) {
        Instant now = Instant.now();
        doc.setUpdatedAt(now);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(now);
        }
        long count = mapper.selectCount(
                new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getDocId, doc.getDocId()));
        if (count == 0) {
            mapper.insert(doc);
        } else {
            mapper.updateById(doc);
        }
    }
}
