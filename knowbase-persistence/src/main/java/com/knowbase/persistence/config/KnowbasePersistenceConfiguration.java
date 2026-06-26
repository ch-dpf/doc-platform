package com.knowbase.persistence.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.knowbase.persistence.handler.JsonbTypeHandler;
import com.knowbase.persistence.support.KnowbaseSchemaSupport;
import com.knowbase.persistence.handler.UuidTypeHandler;
import org.apache.ibatis.type.InstantTypeHandler;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.UUID;

@Configuration
public class KnowbasePersistenceConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    MybatisPlusInterceptor mybatisPlusInterceptor(KnowbaseSchemaSupport schemaSupport) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (schemaSupport.hasSchema()) {
            DynamicTableNameInnerInterceptor dynamicTableName = new DynamicTableNameInnerInterceptor();
            dynamicTableName.setTableNameHandler((sql, tableName) ->
                    tableName.startsWith("kb_") ? schemaSupport.table(tableName) : tableName
            );
            interceptor.addInnerInterceptor(dynamicTableName);
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    ConfigurationCustomizer knowbaseMybatisConfigurationCustomizer() {
        return configuration -> {
            configuration.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(Instant.class, InstantTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(JsonbTypeHandler.class);
        };
    }
}
