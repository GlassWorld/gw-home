package com.gw.infra.db.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = "com.gw.infra.db", annotationClass = Mapper.class)
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml")
        );
        factory.setTypeAliasesPackage(
                String.join(
                        ",",
                        "com.gw.share.vo",
                        "com.gw.share.jvo",
                        "com.gw.infra.db.vo",
                        "com.gw.infra.db.jvo"
                )
        );

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        // snake_case DB 컬럼을 camelCase Java 필드로 자동 매핑한다.
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultFetchSize(100);
        configuration.setDefaultStatementTimeout(30);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }
}
