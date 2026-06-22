package com.knowbase.web.filter;

import com.knowbase.api.spi.KnowbaseTenantResolver;
import com.knowbase.application.security.KnowbaseRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class KnowbaseRequestContextFilter extends OncePerRequestFilter {

    private final KnowbaseTenantResolver tenantResolver;

    public KnowbaseRequestContextFilter(KnowbaseTenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = firstNonBlank(request.getHeader("X-Knowbase-Tenant-Id"), tenantResolver.currentTenantId());
        String userId = firstNonBlank(request.getHeader("X-Knowbase-User-Id"), "anonymous");
        List<String> roles = parseRoles(request.getHeader("X-Knowbase-Roles"));
        KnowbaseRequestContext.set(new KnowbaseRequestContext.Snapshot(tenantId, userId, roles));
        try {
            filterChain.doFilter(request, response);
        } finally {
            KnowbaseRequestContext.clear();
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private static List<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
