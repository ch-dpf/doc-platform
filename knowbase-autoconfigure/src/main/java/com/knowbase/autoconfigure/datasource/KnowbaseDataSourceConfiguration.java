package com.knowbase.autoconfigure.datasource;

import com.knowbase.autoconfigure.KnowbaseProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class KnowbaseDataSourceConfiguration {

    public static final String BEAN_NAME = "knowbaseDataSource";

    @Bean(name = BEAN_NAME)
    @ConditionalOnProperty(prefix = "knowbase.datasource", name = "url")
    public DataSource knowbaseDedicatedDataSource(KnowbaseProperties properties) {
        KnowbaseProperties.Datasource datasource = properties.getDatasource();
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(datasource.getUrl())
                .username(datasource.getUsername())
                .password(datasource.getPassword())
                .driverClassName(datasource.getDriverClassName())
                .build();
    }

    /** Standalone mode: reuse the application primary {@link DataSource}. */
    @Bean(name = BEAN_NAME)
    @ConditionalOnMissingBean(name = BEAN_NAME)
    public DataSource knowbaseSharedDataSource(DataSource dataSource) {
        return dataSource;
    }
}
