package ru.cti.iss.verint;

import com.squareup.okhttp.OkHttpClient;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import ru.cti.iss.verint.dao.VerintDao;
import ru.cti.iss.verint.service.VerintMonitoringService;
import ru.cti.iss.verint.service.VerintNotifierService;
import ru.cti.iss.verint.service.VerintService;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Configuration
@PropertySource("classpath:verint.properties")
public class VerintConfiguration {

    @Inject
    private Environment environment;

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource() {
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setConnectionTimeout(TimeUnit.SECONDS.toMillis(5));
        dataSource.setJdbcUrl(environment.getProperty("jdbc.verint.url"));
        dataSource.setUsername(environment.getProperty("jdbc.verint.username"));
        dataSource.setPassword(environment.getProperty("jdbc.verint.password"));
        dataSource.setReadOnly(true);
        return dataSource;
    }

    @Bean
    public VerintNotifierService verintNotifierService() {
        final OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(2, TimeUnit.SECONDS);
        return new VerintNotifierService(null, environment.getProperty("verint.server.StartRecord"), client);
    }

    @Bean
    public VerintDao verintDao() {
        int dataSourceId = environment.getProperty("verint.db.DatasourceId", Integer.class);
        return new VerintDao(dataSource(), dataSourceId);
    }
}
