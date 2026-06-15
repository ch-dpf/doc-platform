package com.knowbase.autoconfigure.tx;

import com.knowbase.autoconfigure.datasource.KnowbaseDataSourceConfiguration;
import com.knowbase.tx.KnowbaseTransactions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class KnowbaseTransactionConfiguration {

    @Bean(name = KnowbaseTransactions.MANAGER_BEAN_NAME)
    public PlatformTransactionManager knowbaseTransactionManager(
            @Qualifier(KnowbaseDataSourceConfiguration.BEAN_NAME) DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
