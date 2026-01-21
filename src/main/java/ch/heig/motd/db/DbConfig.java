package ch.heig.motd.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Optional;

public class DbConfig {
    public static DataSource createFromEnv() {
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            String host = Optional.ofNullable(System.getenv("DB_HOST")).orElse("localhost");
            String port = Optional.ofNullable(System.getenv("DB_PORT")).orElse("5432");
            String db = Optional.ofNullable(System.getenv("DB_NAME")).orElse("motd");
            String user = Optional.ofNullable(System.getenv("DB_USER")).orElse("motd");
            String pass = Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("motd");
            jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.addDataSourceProperty("socketTimeout", "30");
            cfg.setMaximumPoolSize(5);
            return new HikariDataSource(cfg);
        } else {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl);
            // optional user/pass from env
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");
            if (user != null) cfg.setUsername(user);
            if (pass != null) cfg.setPassword(pass);
            cfg.setMaximumPoolSize(5);
            return new HikariDataSource(cfg);
        }
    }
}
