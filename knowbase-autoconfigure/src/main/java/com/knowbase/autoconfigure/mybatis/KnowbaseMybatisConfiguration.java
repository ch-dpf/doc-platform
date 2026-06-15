package com.knowbase.autoconfigure.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.knowbase.autoconfigure.datasource.KnowbaseDataSourceConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {
            "com.knowbase.ingest.mapper",
            "com.knowbase.vector.mapper",
            "com.knowbase.library.mapper",
            "com.knowbase.chat.mapper"
        },
        sqlSessionFactoryRef = KnowbaseMybatisConfiguration.SESSION_FACTORY_BEAN)
public class KnowbaseMybatisConfiguration {

    public static final String SESSION_FACTORY_BEAN = "knowbaseSqlSessionFactory";

    @Bean(name = SESSION_FACTORY_BEAN)
    public SqlSessionFactory knowbaseSqlSessionFactory(
            @Qualifier(KnowbaseDataSourceConfiguration.BEAN_NAME) DataSource dataSource)
            throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        factory.setTypeHandlersPackage(
                "com.knowbase.ingest.mybatis,com.knowbase.vector.mybatis,com.knowbase.platform.mybatis");
        factory.setPlugins(knowbaseMybatisPlusInterceptor());
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultEnumTypeHandler(org.apache.ibatis.type.EnumTypeHandler.class);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }

    @Bean
    public MybatisPlusInterceptor knowbaseMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
