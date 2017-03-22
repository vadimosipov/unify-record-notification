package ru.cti.iss.verint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.cti.iss.verint.dao.VerintDao;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

@Configuration
@PropertySource("classpath:verint.properties")
@Profile("test")
public class TestVerintConfiguration {

    @Inject
    private Environment environment;

    @Bean
    public EmbeddedDatabase dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();
    }

    @Bean
    public VerintDao verintDao() {
        int dataSourceId = environment.getProperty("verint.db.DatasourceId", Integer.class);
        return new VerintDao(dataSource(), dataSourceId);
    }

    @PreDestroy
    public void destroy() {
        dataSource().shutdown();
    }
}
