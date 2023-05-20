package com.example.conf;


import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;


@Configuration
@EnableCaching
@EnableAutoConfiguration
public class ApplicationConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(javax.sql.DataSource.class)
    public javax.sql.DataSource dataSource(DataSourceProperties dataSourceProperties,
                                           @Value("${spring.datasource.tomcat.max-active}") Integer maxActive) {
        DataSource dataSource = new DataSource();
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        dataSource.setUrl(dataSourceProperties.getUrl());
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        dataSource.setMaxActive(maxActive);

        return dataSource;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("data.sql")));
        return initializer;
    }

    /*@Bean
    public org.ehcache.CacheManager ehCacheManager() {
        URL ehcacheConfigUrl = getClass().getResource("/ehcache.xml");
        return CacheManagerBuilder.newCacheManager(new XmlConfiguration(ehcacheConfigUrl));
    }

    @Bean
    public CacheManager cacheManager() {
        return new JCacheCacheManager(JCacheCachingProvider.getCacheManager(null, null, Caching.getCachingProvider().getDefaultProperties()));
    }*/


}
