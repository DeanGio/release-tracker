package com.deangio.releasetracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.deangio.releasetracker.api.TransitionDeploymentRequest;
import com.deangio.releasetracker.config.AppConfig;
import com.deangio.releasetracker.domain.Deployment;
import com.deangio.releasetracker.domain.DeploymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.testtools.JavalinTest;

class DeploymentStatusIntegrationTest extends AbstractIntegrationTest {

    private ObjectMapper mapper = AppConfig.mapper();

    @Test
    void patchPersistsStatusAndIsVisibleToGet() {
        JavalinTest.test(buildApp(), (server, client) -> {

            var createBody = """
                    {"serviceName":"billing","version":"1.4.2","environment":"PROD"}
                    """;
            var created = client.post("/api/deployments", createBody);
            assertThat(created.code()).isEqualTo(201);

            var createdDeployment = mapper.readValue(created.body().string(), Deployment.class);

            String id = createdDeployment.id().toString();
            var endTime = createdDeployment.startTime().plusSeconds(10).truncatedTo(ChronoUnit.MICROS);

            // var transition = new TransitionDeploymentRequest(DeploymentStatus.SUCCEEDED,
            // endTime);
            // var patchBody = mapper.writeValueAsString(transition);

            var patchBody = """
                    {"transitionTo":"SUCCEEDED", "endTime":"%s"}
                    """.formatted(endTime.toString());

            var patchedBody = client.patch("/api/deployments/" + id + "/status", patchBody);
            assertThat(patchedBody.code()).isEqualTo(200);

            var fetched = client.get("/api/deployments/" + id);
            assertThat(fetched.code()).isEqualTo(200);
            var fetchedDeployment = mapper.readValue(fetched.body().string(), Deployment.class);

            assertThat(fetchedDeployment.status()).isEqualTo(DeploymentStatus.SUCCEEDED);
            assertThat(fetchedDeployment.endTime()).isEqualTo(endTime);
        });
    }

    @Test
    void patchReturns409AndLeavesRowUnchangedForInvalidTransition() {
        JavalinTest.test(buildApp(), (server, client) -> {
            var createBody = """
                    {"serviceName":"billing","version":"1.4.2","environment":"PROD"}
                    """;
            var created = client.post("/api/deployments", createBody);
            assertThat(created.code()).isEqualTo(201);

            var createdDeployment = mapper.readValue(created.body().string(), Deployment.class);
            UUID id = createdDeployment.id();
            var endTime = createdDeployment.startTime()
                    .plusSeconds(10)
                    .truncatedTo(ChronoUnit.MICROS);

            var firstPatch = mapper.writeValueAsString(
                    new TransitionDeploymentRequest(DeploymentStatus.SUCCEEDED, endTime));
            assertThat(client.patch("/api/deployments/" + id + "/status", firstPatch).code())
                    .isEqualTo(200);

            var secondPatch = mapper.writeValueAsString(
                    new TransitionDeploymentRequest(DeploymentStatus.FAILED, endTime.plusSeconds(5)));
            assertThat(client.patch("/api/deployments/" + id + "/status", secondPatch).code())
                    .isEqualTo(409);

            Deployment stored = repository.findById(id).orElseThrow();
            assertThat(stored.status()).isEqualTo(DeploymentStatus.SUCCEEDED);
            assertThat(stored.endTime()).isEqualTo(endTime);
        });
    }

    @Test
    void concurrentPatchesOnlyOneWinsAndRowMatchesWinner() throws Exception {
        JavalinTest.test(buildApp(), (server, client) -> {
            var createBody = """
                    {"serviceName":"billing","version":"1.4.2","environment":"PROD"}
                    """;
            var created = client.post("/api/deployments", createBody);
            assertThat(created.code()).isEqualTo(201);

            var createdDeployment = mapper.readValue(created.body().string(), Deployment.class);
            UUID id = createdDeployment.id();
            var endTime = createdDeployment.startTime()
                    .plusSeconds(10)
                    .truncatedTo(ChronoUnit.MICROS);

            var succeeded = mapper.writeValueAsString(
                    new TransitionDeploymentRequest(DeploymentStatus.SUCCEEDED, endTime));
            var failed = mapper.writeValueAsString(
                    new TransitionDeploymentRequest(DeploymentStatus.FAILED, endTime));

            var start = new CountDownLatch(1);
            var executor = Executors.newFixedThreadPool(2);
            try {
                var first = executor.submit(() -> {
                    start.await();
                    return client.patch("/api/deployments/" + id + "/status", succeeded).code();
                });
                var second = executor.submit(() -> {
                    start.await();
                    return client.patch("/api/deployments/" + id + "/status", failed).code();
                });

                start.countDown();

                int firstCode = first.get(10, TimeUnit.SECONDS);
                int secondCode = second.get(10, TimeUnit.SECONDS);

                assertThat(List.of(firstCode, secondCode))
                        .containsExactlyInAnyOrder(200, 409);

                Deployment stored = repository.findById(id).orElseThrow();
                DeploymentStatus winner = firstCode == 200
                        ? DeploymentStatus.SUCCEEDED
                        : DeploymentStatus.FAILED;

                assertThat(stored.status()).isEqualTo(winner);
                assertThat(stored.endTime()).isEqualTo(endTime);
            } finally {
                executor.shutdownNow();
            }
        });
    }

}