package com.docplatform.ingest.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docplatform.ingest.domain.DocMetadata;
import com.docplatform.ingest.dto.DocumentListQuery;
import com.docplatform.ingest.mapper.DocMetadataMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
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

    public Optional<DocMetadata> findByTenantIdAndChecksumSha256AndDeletedFalse(
            String tenantId, String checksumSha256) {
        return Optional.ofNullable(
                mapper.findByTenantIdAndChecksumSha256AndDeletedFalse(tenantId, checksumSha256));
    }

    public Optional<DocMetadata> findByTenantIdAndSourceUrlAndDeletedFalse(
            String tenantId, String sourceUrl) {
        return Optional.ofNullable(mapper.findByTenantIdAndSourceUrlAndDeletedFalse(tenantId, sourceUrl));
    }

    public Optional<DocMetadata> findByDocIdAndDeletedFalse(UUID docId) {
        return Optional.ofNullable(mapper.findByDocIdAndDeletedFalse(docId));
    }

    public IPage<DocMetadata> list(DocumentListQuery query) {
        Page<DocMetadata> page = new Page<>(query.page(), query.size());
        LambdaQueryWrapper<DocMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocMetadata::getTenantId, query.tenantId());
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

    public void deleteByDocId(UUID docId) {
        mapper.delete(new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getDocId, docId));
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
