package com.example.seckill.config;

import com.example.seckill.datasource.DataSourceType;
import com.example.seckill.datasource.RoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.write")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.datasource.read")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource writeDataSource(@Qualifier("writeDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public DataSource readDataSource(@Qualifier("readDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(@Qualifier("writeDataSource") DataSource writeDataSource,
                                        @Qualifier("readDataSource") DataSource readDataSource) {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.WRITE, writeDataSource);
        targetDataSources.put(DataSourceType.READ, readDataSource);

        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        return routingDataSource;
    }
}
