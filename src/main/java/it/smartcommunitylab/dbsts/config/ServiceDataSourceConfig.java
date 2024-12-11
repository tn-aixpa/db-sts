package it.smartcommunitylab.dbsts.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.service", name = "platform", havingValue = "postgresql")
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.smartcommunitylab.dbsts.db.repository",
        entityManagerFactoryRef = "serviceEntityManagerFactory",
        transactionManagerRef = "serviceTransactionManager"
)
public class ServiceDataSourceConfig {


    @Bean
    @ConfigurationProperties("spring.datasource.service")
    public DataSourceProperties serviceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "serviceDataSource")
    public DataSource serviceDataSource() {
       return serviceDataSourceProperties()
               .initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "serviceEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean serviceEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("serviceDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("it.smartcommunitylab.dbsts.db.entity")
                .persistenceUnit("servicePU")
                .properties(
                        Map.of(
                                "hibernate.hbm2ddl.auto", "update",
                                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"
                        )
                )
                .build();
    }

    @Primary
    @Bean(name = "serviceTransactionManager")
    public PlatformTransactionManager serviceTransactionManager(
            @Qualifier("serviceEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }

}