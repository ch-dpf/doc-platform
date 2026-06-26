package com.knowbase.autoconfigure;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Host embedding mode: run KnowBase DDL with a dedicated Flyway history table,
 * separate from the host application's {@code spring.flyway} migrations.
 */
@AutoConfiguration(
        after = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class},
        before = KnowbasePersistenceAutoConfiguration.class
)
@ConditionalOnClass(Flyway.class)
@ConditionalOnExpression("${knowbase.persistence.enabled:false} && ${knowbase.flyway.enabled:true} && ${knowbase.flyway.autoconfigure:true}")
@EnableConfigurationProperties(KnowbaseProperties.class)
public class KnowbaseFlywayAutoConfiguration {

    @Bean
    public Flyway knowbaseFlyway(DataSource dataSource, KnowbaseProperties properties) {
        KnowbaseFlywaySupport.migrateIfEnabled(dataSource, properties);
        return KnowbaseFlywaySupport.configure(dataSource, properties).load();
    }
}
