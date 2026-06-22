package com.knowbase.domain.audit;

public final class NoopAuditSink implements AuditSink {

    @Override
    public void record(AuditEvent event) {
        // Default for lightweight host mode; persistence mode provides a database-backed sink.
    }
}
