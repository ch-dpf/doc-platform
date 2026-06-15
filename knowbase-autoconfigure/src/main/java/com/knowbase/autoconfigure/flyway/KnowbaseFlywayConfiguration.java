package com.knowbase.autoconfigure.flyway;

import com.knowbase.autoconfigure.KnowbaseProperties;
import com.knowbase.autoconfigure.datasource.KnowbaseDataSourceConfiguration;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "knowbase.flyway", name = "enabled", havingValue = "true")
public class KnowbaseFlywayConfiguration {

    @Bean(initMethod = "migrate")
    public Flyway knowbaseFlyway(
            @Qualifier(KnowbaseDataSourceConfiguration.BEAN_NAME) DataSource dataSource,
            KnowbaseProperties properties) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/knowbase/migration")
                .table("knowbase_flyway_schema_history")
                .baselineOnMigrate(true)
                .load();
    }
}
