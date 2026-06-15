package com.knowbase.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Disables Spring Boot's default Flyway auto-migration; schema is managed either by
 * {@code infra/postgres/init.sql} (Docker) or {@link com.knowbase.autoconfigure.flyway.KnowbaseFlywayConfiguration}.
 */
public class KnowbaseEnvironmentDefaultsPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE = "knowbaseEnvironmentDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE)) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>();
        if (!environment.containsProperty("spring.flyway.enabled")) {
            defaults.put("spring.flyway.enabled", false);
        }
        if (!defaults.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
