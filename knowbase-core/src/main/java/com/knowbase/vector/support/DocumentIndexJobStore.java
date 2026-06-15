package com.knowbase.vector.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.vector.domain.DocumentIndexJob;
import com.knowbase.vector.mapper.DocumentIndexJobMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentIndexJobStore {

    private final DocumentIndexJobMapper mapper;

    public DocumentIndexJobStore(DocumentIndexJobMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<DocumentIndexJob> findByDocIdAndVersion(UUID docId, int version) {
        return Optional.ofNullable(mapper.findByDocIdAndVersion(docId, version));
    }

    public DocumentIndexJob save(DocumentIndexJob job) {
        long count = mapper.selectCount(
                new LambdaQueryWrapper<DocumentIndexJob>().eq(DocumentIndexJob::getJobId, job.getJobId()));
        if (count == 0) {
            mapper.insert(job);
        } else {
            mapper.updateById(job);
        }
        return job;
    }
}
