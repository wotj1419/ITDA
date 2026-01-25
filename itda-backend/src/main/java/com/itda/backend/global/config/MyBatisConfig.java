package com.itda.backend.global.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 설정
 */
@Configuration
@MapperScan(basePackages = "com.itda.backend.**.repository")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // Mapper XML 위치 설정
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath:mapper/**/*.xml"));

        // Type Aliases 패키지 설정
        sessionFactory.setTypeAliasesPackage(String.join(
                ",",
                "com.itda.backend.auth.domain",
                "com.itda.backend.asset.domain",
                "com.itda.backend.job.domain",
                "com.itda.backend.node.domain",
                "com.itda.backend.project.domain",
                "com.itda.backend.scene.domain",
                "com.itda.backend.node.repository.dto",
                "com.itda.backend.project.repository.dto",
                "com.itda.backend.scenario.repository.dto",
                "com.itda.backend.timeline.repository.dto"
        ));

        // MyBatis 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setUseGeneratedKeys(true);
        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }
}
