package com.knowbase.domain.audit;

public interface AuditSink {

    void record(AuditEvent event);
}
