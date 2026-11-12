package com.deangio.releasetracker;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.deangio.releasetracker.api.DeploymentController;
import com.deangio.releasetracker.api.Routes;
import com.deangio.releasetracker.config.AppConfig;
import com.deangio.releasetracker.repository.DeploymentRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

@Testcontainers 
abstract class AbstractIntegrationTest {

    
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static HikariDataSource dataSource;
    static DeploymentRepository repository;
    static DeploymentController controller;

    @BeforeAll
    static void startInfrastructure() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(config);

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        repository = new DeploymentRepository(dataSource);
        controller = new DeploymentController(repository);
    }

    @AfterAll
    static void stopInfrastructure() {
        dataSource.close();
    }

    @BeforeEach
    void cleanTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE deployment");
        }
    }

    protected Javalin buildApp() {
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(AppConfig.mapper(), false));
            Routes.register(config, controller);
        });
    }
}
