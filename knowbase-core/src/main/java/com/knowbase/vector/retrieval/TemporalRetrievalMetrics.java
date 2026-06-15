package com.knowbase.vector.retrieval;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** 时间感知检索指标（预过滤/降级/后过滤剔除）。 */
@Component
public class TemporalRetrievalMetrics {

    private final AtomicLong prefilterApplied = new AtomicLong();
    private final AtomicLong prefilterFallback = new AtomicLong();
    private final AtomicLong postfilterDropped = new AtomicLong();
    private final AtomicLong personRelaxed = new AtomicLong();

    public void recordPrefilterApplied() {
        prefilterApplied.incrementAndGet();
    }

    public void recordPrefilterFallback() {
        prefilterFallback.incrementAndGet();
    }

    public void recordPostfilterDropped(int count) {
        if (count > 0) {
            postfilterDropped.addAndGet(count);
        }
    }

    public void recordPersonRelaxed() {
        personRelaxed.incrementAndGet();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                prefilterApplied.get(),
                prefilterFallback.get(),
                postfilterDropped.get(),
                personRelaxed.get());
    }

    public record Snapshot(long prefilterApplied, long prefilterFallback, long postfilterDropped, long personRelaxed) {}
}
