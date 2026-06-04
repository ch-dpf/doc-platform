package com.docplatform.vector.config;

import com.docplatform.vector.mybatis.PostgresUuidTypeHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class MybatisConfig implements InitializingBean {

    private final SqlSessionFactory sqlSessionFactory;

    public MybatisConfig(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public void afterPropertiesSet() {
        TypeHandlerRegistry registry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
        registry.register(UUID.class, PostgresUuidTypeHandler.class);
    }
}
