package com.academic.risk.backend.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {
    private static final String DEFAULT_LOCAL_MYSQL_URL =
            "jdbc:mysql://localhost:3306/academic_risk_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_LOCAL_MYSQL_USERNAME = "root";
    private static final String DEFAULT_LOCAL_MYSQL_PASSWORD = "root";

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        String rawUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("JDBC_DATABASE_URL"),
                buildPostgresUrlFromParts(environment),
                environment.getProperty("spring.datasource.url")
        );

        String username = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("DATABASE_USERNAME"),
                environment.getProperty("PGUSER"),
                environment.getProperty("POSTGRES_USER"),
                environment.getProperty("spring.datasource.username"),
                DEFAULT_LOCAL_MYSQL_USERNAME
        );

        String password = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("DATABASE_PASSWORD"),
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("POSTGRES_PASSWORD"),
                environment.getProperty("spring.datasource.password"),
                DEFAULT_LOCAL_MYSQL_PASSWORD
        );

        String jdbcUrl = normalizeJdbcUrl(rawUrl);
        String driverClassName = jdbcUrl.startsWith("jdbc:postgresql:")
                ? "org.postgresql.Driver"
                : "com.mysql.cj.jdbc.Driver";

        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Environment environment) {
        return properties -> {
            String rawUrl = firstNonBlank(
                    environment.getProperty("SPRING_DATASOURCE_URL"),
                    environment.getProperty("DATABASE_URL"),
                    environment.getProperty("JDBC_DATABASE_URL"),
                    buildPostgresUrlFromParts(environment),
                    environment.getProperty("spring.datasource.url")
            );
            String jdbcUrl = normalizeJdbcUrl(rawUrl);
            if (jdbcUrl.startsWith("jdbc:postgresql:")) {
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            } else {
                properties.putIfAbsent("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            }
        };
    }

    private String normalizeJdbcUrl(String rawUrl) {
        if (isBlank(rawUrl) || rawUrl.startsWith("${")) {
            return DEFAULT_LOCAL_MYSQL_URL;
        }

        String trimmedUrl = rawUrl.trim();

        if (trimmedUrl.startsWith("http:postgresql://")) {
            trimmedUrl = "jdbc:" + trimmedUrl.substring("http:".length());
        } else if (trimmedUrl.startsWith("postgresql://")) {
            trimmedUrl = "jdbc:" + trimmedUrl;
        } else if (trimmedUrl.startsWith("postgres://")) {
            trimmedUrl = "jdbc:postgresql://" + trimmedUrl.substring("postgres://".length());
        }

        if (trimmedUrl.startsWith("jdbc:postgresql://") && !containsQueryParameter(trimmedUrl, "sslmode")) {
            trimmedUrl = trimmedUrl + (trimmedUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        return trimmedUrl;
    }

    private String buildPostgresUrlFromParts(Environment environment) {
        String host = firstNonBlank(
                environment.getProperty("PGHOST"),
                environment.getProperty("POSTGRES_HOST")
        );
        String port = firstNonBlank(
                environment.getProperty("PGPORT"),
                environment.getProperty("POSTGRES_PORT"),
                "5432"
        );
        String database = firstNonBlank(
                environment.getProperty("PGDATABASE"),
                environment.getProperty("POSTGRES_DB")
        );

        if (isBlank(host) || isBlank(database)) {
            return null;
        }

        return "jdbc:postgresql://" + host.trim() + ":" + port.trim() + "/" + database.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value) && !value.trim().startsWith("${")) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean containsQueryParameter(String url, String parameterName) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0 || queryStart == url.length() - 1) {
            return false;
        }

        String[] parameters = url.substring(queryStart + 1).split("&");
        for (String parameter : parameters) {
            if (parameter.equalsIgnoreCase(parameterName)
                    || parameter.toLowerCase().startsWith(parameterName.toLowerCase() + "=")) {
                return true;
            }
        }
        return false;
    }
}
