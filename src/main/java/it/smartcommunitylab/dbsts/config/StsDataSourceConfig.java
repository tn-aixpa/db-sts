package it.smartcommunitylab.dbsts.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.sts", name = "platform", havingValue = "postgresql")
public class StsDataSourceConfig {


    @Bean
    @ConfigurationProperties("spring.datasource.sts")
    public DataSourceProperties stsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "stsDataSource")
    public DataSource stsDataSource() {
        return stsDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "stsJdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("stsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
