package com.knowbase.ingest.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        return findParsedWithTextKey(libraryId, tenantId, null);
    }

    public List<DocMetadata> findParsedWithTextKey(UUID libraryId, String tenantId, String chunkProfileId) {
        LambdaQueryWrapper<DocMetadata> wrapper = new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId)
                .eq(DocMetadata::isDeleted, false)
                .eq(DocMetadata::getParseStatus, ParseStatus.PARSED)
                .isNotNull(DocMetadata::getParsedTextKey)
                .ne(DocMetadata::getParsedTextKey, "");
        if (chunkProfileId != null && !chunkProfileId.isBlank()) {
            wrapper.eq(DocMetadata::getChunkProfileId, chunkProfileId.trim());
        }
        return mapper.selectList(wrapper.orderByDesc(DocMetadata::getUpdatedAt));
    }

    public List<DocMetadata> findParsedWithTextKeyNotOnPrimary(
            UUID libraryId, String tenantId, String primaryChunkProfileId) {
        return mapper.selectList(
                parsedNotOnPrimaryWrapper(libraryId, tenantId, primaryChunkProfileId)
                        .orderByDesc(DocMetadata::getUpdatedAt));
    }

    public int countParsedWithTextKeyNotOnPrimary(
            UUID libraryId, String tenantId, String primaryChunkProfileId) {
        Long count = mapper.selectCount(parsedNotOnPrimaryWrapper(libraryId, tenantId, primaryChunkProfileId));
        return count != null ? count.intValue() : 0;
    }

    private static LambdaQueryWrapper<DocMetadata> parsedNotOnPrimaryWrapper(
            UUID libraryId, String tenantId, String primaryChunkProfileId) {
        LambdaQueryWrapper<DocMetadata> wrapper = new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId.trim())
                .eq(DocMetadata::isDeleted, false)
                .eq(DocMetadata::getParseStatus, ParseStatus.PARSED)
                .isNotNull(DocMetadata::getParsedTextKey)
                .ne(DocMetadata::getParsedTextKey, "");
        if (primaryChunkProfileId != null && !primaryChunkProfileId.isBlank()) {
            String primary = primaryChunkProfileId.trim();
            wrapper.and(w -> w.ne(DocMetadata::getChunkProfileId, primary).or().isNull(DocMetadata::getChunkProfileId));
        }
        return wrapper;
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

    public List<DocMetadata> findActiveByChunkProfile(UUID libraryId, String tenantId, String chunkProfileId) {
        return mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .eq(DocMetadata::getLibraryId, libraryId)
                .eq(DocMetadata::getTenantId, tenantId.trim())
                .eq(DocMetadata::isDeleted, false)
                .eq(DocMetadata::getChunkProfileId, chunkProfileId.trim())
                .orderByDesc(DocMetadata::getUpdatedAt));
    }

    public List<DocMetadata> findActiveByIds(Collection<UUID> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<DocMetadata>()
                .in(DocMetadata::getDocId, docIds)
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

    public List<DocMetadata> findAnyByDocIds(Collection<UUID> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<DocMetadata>().in(DocMetadata::getDocId, docIds));
    }

    public long sumSizeBytesByLibraryId(UUID libraryId) {
        return mapper.sumSizeBytesByLibraryId(libraryId);
    }

    public int countDistinctChunkProfiles(UUID libraryId) {
        return mapper.countDistinctChunkProfiles(libraryId);
    }

    public boolean existsChunkProfileId(UUID libraryId, String chunkProfileId) {
        if (chunkProfileId == null || chunkProfileId.isBlank()) {
            return false;
        }
        return mapper.existsChunkProfileId(libraryId, chunkProfileId);
    }

    public List<ChunkProfileStatsRow> listChunkProfileStats(UUID libraryId) {
        return mapper.listChunkProfileStats(libraryId).stream()
                .map(row -> new ChunkProfileStatsRow(row.chunkProfileId(), row.docCount(), row.chunkCount()))
                .toList();
    }

    public List<DocMetadata> findMissingChunkProfile(UUID libraryId) {
        return mapper.findMissingChunkProfile(libraryId);
    }

    public record ChunkProfileStatsRow(String chunkProfileId, int docCount, int chunkCount) {}

    /** 显式写入 ingest_profile_json（含置 null），避免 updateById 默认跳过 null 字段。 */
    public void updateIngestProfileJson(UUID docId, String ingestProfileJson) {
        mapper.update(
                null,
                new LambdaUpdateWrapper<DocMetadata>()
                        .eq(DocMetadata::getDocId, docId)
                        .set(DocMetadata::getIngestProfileJson, ingestProfileJson)
                        .set(DocMetadata::getUpdatedAt, Instant.now()));
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
