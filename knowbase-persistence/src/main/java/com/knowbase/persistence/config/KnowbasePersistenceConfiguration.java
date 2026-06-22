package com.knowbase.persistence.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.knowbase.persistence.handler.JsonbTypeHandler;
import com.knowbase.persistence.handler.UuidTypeHandler;
import org.apache.ibatis.type.InstantTypeHandler;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.UUID;

@Configuration
@MapperScan("com.knowbase.persistence.mapper")
public class KnowbasePersistenceConfiguration {

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
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
