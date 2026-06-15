package com.knowbase.facade;

import com.knowbase.api.spi.KnowbaseTenantResolver;

public final class KnowbaseTenantSupport {

    private final KnowbaseTenantResolver tenantResolver;

    public KnowbaseTenantSupport(KnowbaseTenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    public String resolve(String commandTenantId) {
        if (tenantResolver != null) {
            String tenant = tenantResolver.currentTenantId();
            if (tenant == null || tenant.isBlank()) {
                throw new IllegalStateException("KnowbaseTenantResolver returned blank tenantId");
            }
            return tenant.trim();
        }
        if (commandTenantId == null || commandTenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required when no KnowbaseTenantResolver is configured");
        }
        return commandTenantId.trim();
    }
}
