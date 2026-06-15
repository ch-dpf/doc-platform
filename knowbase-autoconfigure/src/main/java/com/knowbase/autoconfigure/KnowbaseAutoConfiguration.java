package com.knowbase.autoconfigure;

import com.knowbase.autoconfigure.datasource.KnowbaseDataSourceConfiguration;
import com.knowbase.autoconfigure.flyway.KnowbaseFlywayConfiguration;
import com.knowbase.autoconfigure.mybatis.KnowbaseMybatisConfiguration;
import com.knowbase.autoconfigure.platform.KnowbasePlatformConfiguration;
import com.knowbase.autoconfigure.scan.KnowbaseCoreComponentScanConfiguration;
import com.knowbase.autoconfigure.tx.KnowbaseTransactionConfiguration;
import com.knowbase.autoconfigure.web.KnowbaseAsyncConfiguration;
import com.knowbase.autoconfigure.web.KnowbaseWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "knowbase", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KnowbaseProperties.class)
@EnableAsync
@Import({
    KnowbaseDataSourceConfiguration.class,
    KnowbaseMybatisConfiguration.class,
    KnowbaseTransactionConfiguration.class,
    KnowbaseFlywayConfiguration.class,
    KnowbasePlatformConfiguration.class,
    KnowbaseAsyncConfiguration.class,
    KnowbaseCoreComponentScanConfiguration.class,
    KnowbaseFacadeAutoConfiguration.class,
    KnowbaseWebAutoConfiguration.class
})
public class KnowbaseAutoConfiguration {}
