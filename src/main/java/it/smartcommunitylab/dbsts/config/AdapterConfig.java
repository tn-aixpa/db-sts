package it.smartcommunitylab.dbsts.config;

import it.smartcommunitylab.dbsts.db.DbAdapter;
import it.smartcommunitylab.dbsts.postgresql.PostgresqlAdapter;
import it.smartcommunitylab.dbsts.postgresql.PostgresqlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfig {

    @Autowired
    PostgresqlProperties postgresqlProperties;

    @Value("${adapter.connection.platform}")
    String platform;

    @Bean(name = "connectionDataSourceProperties")
    @ConfigurationProperties("adapter.connection")
    public DataSourceProperties connectionDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DbAdapter adapter(
        @Qualifier("connectionDataSourceProperties") DataSourceProperties connectionDataSourceProperties
    ) {
        //supports only postgresql for now
        if ("postgresql".equals(platform)) {
            return new PostgresqlAdapter(connectionDataSourceProperties, postgresqlProperties);
        }

        return null;
    }
}
