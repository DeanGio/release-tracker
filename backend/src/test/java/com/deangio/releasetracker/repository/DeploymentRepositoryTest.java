package com.deangio.releasetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.deangio.releasetracker.domain.Deployment;
import com.deangio.releasetracker.domain.DeploymentStatus;
import com.deangio.releasetracker.domain.TargetEnvironment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Testcontainers
class DeploymentRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static HikariDataSource dataSource;

    @BeforeAll
    static void setUp() {
        var config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        dataSource = new HikariDataSource(config);

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    @BeforeEach
    void clearTable() {
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement()) {
            stmt.execute("truncate table deployment");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findByIdReturnsEmptyWhenDeploymentDoesNotExist() {
        var repository = new DeploymentRepository(dataSource);

        Optional<Deployment> result = repository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdReturnsIdRequested() {
        var repository = new DeploymentRepository(dataSource);
        var deployment = Deployment.start("billing service", "1.0.2", TargetEnvironment.PROD, Instant.now());

        repository.save(deployment);

        assertThat(repository.findById(deployment.id()))
                .isPresent()
                .hasValueSatisfying(found -> assertThat(found.id()).isEqualTo(deployment.id()));
    }

    @Test
    void findAllPagesNewestFirst() {
        var repository = new DeploymentRepository(dataSource);
        var oldest = Deployment.start("svc", "1.0.0", TargetEnvironment.DEV, Instant.parse("2026-11-01T10:00:00Z"));
        var middle = Deployment.start("svc", "1.0.1", TargetEnvironment.DEV, Instant.parse("2026-11-02T10:00:00Z"));
        var newest = Deployment.start("svc", "1.0.2", TargetEnvironment.DEV, Instant.parse("2026-11-03T10:00:00Z"));
        repository.save(oldest);
        repository.save(middle);
        repository.save(newest);

        assertThat(repository.findAll(2, 0))
                .extracting(Deployment::id)
                .containsExactly(newest.id(), middle.id());

        assertThat(repository.findAll(2, 2))
                .extracting(Deployment::id)
                .containsExactly(oldest.id());
    }

    @Test
    void updateFinishesInProgressDeployment() {
        var repository = new DeploymentRepository(dataSource);
        var deployment = Deployment.start("svc", "1.0.0", TargetEnvironment.DEV, Instant.parse("2026-11-01T10:00:00Z"));
        repository.save(deployment);

        var updated = deployment.transitionTo(DeploymentStatus.SUCCEEDED, Instant.parse("2026-11-02T10:00:00Z"));
        repository.update(updated);

        var recalled = repository.findById(deployment.id()).orElseThrow();

        assertThat(recalled.status()).isEqualTo(updated.status());
        assertThat(recalled.endTime()).isEqualTo(updated.endTime());
    }

    @Test
    void updateDeploymentOnAlreadyFinished() {
        var repository = new DeploymentRepository(dataSource);
        var deployment = Deployment.start("svc", "1.0.0", TargetEnvironment.DEV, Instant.parse("2026-11-01T10:00:00Z"));
        repository.save(deployment);

        var updated = deployment.transitionTo(DeploymentStatus.SUCCEEDED, Instant.parse("2026-11-02T10:00:00Z"));
        repository.update(updated);

        var recalled = repository.findById(deployment.id()).get();

        assertThatThrownBy(() -> repository.update(recalled))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid transition");

    }

    @Test
    void updateRejectsStaleInProgressRead() {
        var repository = new DeploymentRepository(dataSource);
        var deployment = Deployment.start("svc", "1.0.0", TargetEnvironment.DEV, Instant.parse("2026-11-01T10:00:00Z"));
        repository.save(deployment);

        repository.update(deployment.transitionTo(DeploymentStatus.SUCCEEDED, Instant.parse("2026-11-02T10:00:00Z")));

        var stale = deployment.transitionTo(DeploymentStatus.FAILED, Instant.parse("2026-11-02T10:01:00Z"));

        assertThatThrownBy(() -> repository.update(stale))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid transition");

        assertThat(repository.findById(deployment.id()).get().status())
                .isEqualTo(DeploymentStatus.SUCCEEDED);
    }

    @Test
    void databaseRejectsFinishedDeploymentWithoutEndTime() {
        try (var conn = dataSource.getConnection();
                var stmt = conn.prepareStatement("""
                        insert into deployment (id, service_name, version, environment, status, start_time)
                        values (gen_random_uuid(), 'svc', '1.0.0', 'DEV', 'SUCCEEDED', now())
                        """)) {
            assertThatThrownBy(stmt::executeUpdate)
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("deployment_end_time_check");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
