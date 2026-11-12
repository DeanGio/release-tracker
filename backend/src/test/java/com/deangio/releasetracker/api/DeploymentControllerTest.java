package com.deangio.releasetracker.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.deangio.releasetracker.config.AppConfig;
import com.deangio.releasetracker.domain.Deployment;
import com.deangio.releasetracker.domain.DeploymentStatus;
import com.deangio.releasetracker.domain.TargetEnvironment;
import com.deangio.releasetracker.repository.DeploymentRepository;
import com.fasterxml.jackson.databind.JsonNode;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

class DeploymentControllerTest {

    DeploymentRepository repository;
    Javalin app;

    @BeforeEach
    void init() {
        repository = mock(DeploymentRepository.class);
        var controller = new DeploymentController(repository);
        app = Javalin.create(config -> Routes.register(config, controller));

    }

    @Test
    void listPassesLimitAndOffsetToRepository() {
        when(repository.findAll(5, 10)).thenReturn(List.of());

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments?limit=5&offset=10");

            assertThat(response.code()).isEqualTo(200);
            verify(repository).findAll(5, 10);
        });
    }

    @Test
    void listUsesDefaultsWhenParamsAbsent() {
        when(repository.findAll(20, 0)).thenReturn(List.of());

        JavalinTest.test(app, (server, client) -> {
            assertThat(client.get("/api/deployments").code()).isEqualTo(200);
            verify(repository).findAll(20, 0);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "limit=0", "limit=101", "limit=-1", "offset=-1", "limit=abc" })
    void listRejectsInvalidPaging(String query) {

        JavalinTest.test(app, (server, client) -> {
            assertThat(client.get("/api/deployments?" + query).code()).isEqualTo(400);
            verifyNoInteractions(repository);
        });
    }

    private Deployment sampleDeployment() {
        return Deployment.start("sample", "1.0.0", TargetEnvironment.DEV, Instant.parse("2026-11-01T10:00:00Z"));
    }

    @Test
    void listPassesLimitAndOffsetAndReturnsEnvelope() {
        when(repository.findAll(5, 10)).thenReturn(
                List.of(sampleDeployment(), sampleDeployment(), sampleDeployment(),
                        sampleDeployment(), sampleDeployment()));
        when(repository.count()).thenReturn(42L);

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments?limit=5&offset=10");

            assertThat(response.code()).isEqualTo(200);

            JsonNode body = AppConfig.mapper().readTree(response.body().string());
            assertThat(body.isObject()).as("response should be an envelope").isTrue();
            assertThat(body.get("total").asLong()).isEqualTo(42L);
            assertThat(body.get("limit").asInt()).isEqualTo(5);
            assertThat(body.get("offset").asInt()).isEqualTo(10);
            assertThat(body.get("nextOffset").asInt()).isEqualTo(15);

            verify(repository).findAll(5, 10);
            verify(repository).count();
        });
    }

    @Test
    void listReturnsNullNextOffsetOnLastPage() {
        when(repository.findAll(5, 10)).thenReturn(
                List.of(sampleDeployment(), sampleDeployment()));
        when(repository.count()).thenReturn(12L);

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments?limit=5&offset=10");

            assertThat(response.code()).isEqualTo(200);

            JsonNode body = AppConfig.mapper().readTree(response.body().string());
            assertThat(body.get("items")).hasSize(2);
            assertThat(body.get("total").asLong()).isEqualTo(12L);
            assertThat(body.get("nextOffset").isNull()).isTrue();

            verify(repository).findAll(5, 10);
            verify(repository).count();
        });
    }

    @Test
    void getByIdReturnsDeployment() {
        Deployment deployment = sampleDeployment();
        UUID id = deployment.id();
        when(repository.findById(id)).thenReturn(Optional.of(deployment));

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments/" + id);

            assertThat(response.code()).isEqualTo(200);

            JsonNode body = AppConfig.mapper().readTree(response.body().string());
            assertThat(body.get("id").asText()).isEqualTo(id.toString());
            assertThat(body.get("serviceName").asText()).isEqualTo(deployment.serviceName());
            assertThat(body.get("status").asText()).isEqualTo(deployment.status().name());

            verify(repository).findById(id);
        });
    }

    @Test
    void getByIdReturns400ForMalformedUuid() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments/not-a-uuid");

            assertThat(response.code()).isEqualTo(400);

            verifyNoInteractions(repository);
        });
    }

    @Test
    void getByIdReturns404WhenMissing() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/deployments/" + missing);

            assertThat(response.code()).isEqualTo(404);

            verify(repository).findById(missing);
        });
    }

    @Test
    void createReturns201WithLocationAndPersists() {
        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"serviceName":"billing","version":"1.4.2","environment":"STAGING"}
                    """;
            var res = client.post("/api/deployments", body);

            assertThat(res.code()).isEqualTo(201);
            assertThat(res.headers().get("Location")).first(InstanceOfAssertFactories.STRING)
                    .startsWith("/api/deployments/");
        });
        verify(repository).save(any(Deployment.class));
    }

    @Test
    void createReturns400ForUnknownEnvironment() {
        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"serviceName":"billing","version":"1.4.2","environment":"QA"}
                    """;
            assertThat(client.post("/api/deployments", body).code()).isEqualTo(400);
        });
        verify(repository, never()).save(any());
    }

    @Test
    void createReturns400WhenValidationFails() {
        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"serviceName":"","version":"1.4.2","environment":"DEV"}
                    """;
            assertThat(client.post("/api/deployments", body).code()).isEqualTo(400);
        });
        verify(repository, never()).save(any());
    }

    private static final String PATCH_PATH = "/api/deployments/";

    @Test
    void updateStatusTransitionsAndPersists() {
        Deployment inProgress = sampleDeployment();
        when(repository.findById(inProgress.id())).thenReturn(Optional.of(inProgress));

        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"transitionTo":"SUCCEEDED","endTime":"2026-11-08T10:00:00Z"}
                    """;
            var res = client.patch(PATCH_PATH + inProgress.id() + "/status", body);

            assertThat(res.code()).isEqualTo(200);
            verify(repository).update(argThat(d -> d.status() == DeploymentStatus.SUCCEEDED));
        });
    }

    @Test
    void updateStatusReturns409ForInvalidTransition() {
        Deployment finished = sampleDeployment().transitionTo(DeploymentStatus.FAILED, Instant.now());
        when(repository.findById(finished.id())).thenReturn(Optional.of(finished));

        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"transitionTo":"SUCCEEDED","endTime":"2026-11-08T10:00:00Z"}
                    """;
            assertThat(client.patch(PATCH_PATH + finished.id() + "/status", body).code()).isEqualTo(409);
            verify(repository, never()).update(any());
        });
    }

    @Test
    void updateStatusReturns409WhenRepositoryLosesRace() {
        Deployment inProgress = sampleDeployment();
        when(repository.findById(inProgress.id())).thenReturn(Optional.of(inProgress));
        doThrow(new IllegalStateException("invalid transition"))
                .when(repository).update(any());

        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"transitionTo":"FAILED","endTime":"2026-11-08T10:00:00Z"}
                    """;
            assertThat(client.patch(PATCH_PATH + inProgress.id() + "/status", body).code()).isEqualTo(409);
            verify(repository).update(any());
        });
    }

    @Test
    void updateStatusReturns400WhenTransitionMissing() {
        JavalinTest.test(app, (server, client) -> {
            var id = UUID.randomUUID();
            assertThat(client.patch(PATCH_PATH + id + "/status", "{}").code()).isEqualTo(400);
            verifyNoInteractions(repository);
        });
    }

    @Test
    void updateStatusReturns404ForUnknownId() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"transitionTo":"SUCCEEDED","endTime":"2026-11-08T10:00:00Z"}
                    """;
            assertThat(client.patch(PATCH_PATH + missing + "/status", body).code()).isEqualTo(404);
            verify(repository, never()).update(any());
        });
    }

    @Test
    void updateStatusReturns400ForMissingEndTime() {
        Deployment inProgress = sampleDeployment();
        when(repository.findById(inProgress.id())).thenReturn(Optional.of(inProgress));

        JavalinTest.test(app, (server, client) -> {
            var body = """
                    {"transitionTo":"SUCCEEDED"}
                    """;
            assertThat(client.patch(PATCH_PATH + inProgress.id() + "/status", body).code()).isEqualTo(400);
            verify(repository, never()).update(any());
        });
    }

}
