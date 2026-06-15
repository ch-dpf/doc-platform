package com.knowbase.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowbase")
public class KnowbaseProperties {

    /** Master switch for KnowBase auto-configuration. */
    private boolean enabled = true;

    private final Web web = new Web();

    private final Datasource datasource = new Datasource();

    private final Flyway flyway = new Flyway();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Web getWeb() {
        return web;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public Flyway getFlyway() {
        return flyway;
    }

    public static class Web {
        /** When true, expose /api/v1/* REST controllers from KnowBase. */
        private boolean exposeControllers = false;

        public boolean isExposeControllers() {
            return exposeControllers;
        }

        public void setExposeControllers(boolean exposeControllers) {
            this.exposeControllers = exposeControllers;
        }
    }

    public static class Datasource {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public boolean isConfigured() {
            return url != null && !url.isBlank();
        }
    }

    public static class Flyway {
        /** When true, run classpath db/knowbase/migration via Knowbase Flyway (not Spring Boot Flyway). */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
