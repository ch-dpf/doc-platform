package com.knowbase.api.spi;

/**
 * Host-provided tenant resolution for embedded mode.
 * When absent, facades use {@code tenantId} from command objects (standalone / HTTP).
 */
@FunctionalInterface
public interface KnowbaseTenantResolver {

    String currentTenantId();
}
