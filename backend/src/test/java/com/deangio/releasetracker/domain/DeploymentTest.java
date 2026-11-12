package com.deangio.releasetracker.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DeploymentTest {

    private static final Instant START = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-06T10:05:00Z");

    private Deployment inProgress() {
        return Deployment.start("billing-api", "1.0.0", TargetEnvironment.DEV,
                START);
    }

    @Test
    void transitionReturnsNewDeploymentWithNewStatus() {
        var original = inProgress();

        var failed = original.transitionTo(DeploymentStatus.FAILED, END);

        assertThat(failed.status()).isEqualTo(DeploymentStatus.FAILED);
        assertThat(failed.endTime()).isEqualTo(END);
    }

    @Test
    void transitionLeavesOriginalDeploymentUnchanged() {
        var original = inProgress();

        original.transitionTo(DeploymentStatus.SUCCEEDED, END);

        assertThat(original.status()).isEqualTo(DeploymentStatus.IN_PROGRESS);
        assertThat(original.endTime()).isNull();
    }

    @Test
    void finishedDeploymentCannotTransitionAgain() {
        var failed = inProgress().transitionTo(DeploymentStatus.FAILED, END);

        assertThatThrownBy(() -> failed.transitionTo(DeploymentStatus.SUCCEEDED, END))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void blankVersionIsRejected() {
        assertThatThrownBy(() -> Deployment.start("billing-api", "   ", TargetEnvironment.DEV, START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }

    @Test
    void finishedDeploymentWithoutEndTimeIsRejected() {
        assertThatThrownBy(() -> new Deployment(UUID.randomUUID(), "billing-api", "1.0.0", TargetEnvironment.PROD,
                DeploymentStatus.SUCCEEDED, START, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end time");
    }

    private Deployment newDeployment(DeploymentStatus deploymentStatus) {
        Instant endTime = null;
        if (deploymentStatus != DeploymentStatus.IN_PROGRESS) {
            endTime = END;
        }
        return new Deployment(UUID.randomUUID(), "billing-api", "1.0.0", TargetEnvironment.DEV,
                deploymentStatus, START, endTime);
    }

    @ParameterizedTest
    @CsvSource({
            "IN_PROGRESS, FAILED, true",
            "IN_PROGRESS, SUCCEEDED, true",
            "IN_PROGRESS, IN_PROGRESS, false",
            "FAILED, FAILED, false",
            "FAILED, SUCCEEDED, false",
            "FAILED, IN_PROGRESS, false",
            "SUCCEEDED, FAILED, false",
            "SUCCEEDED, SUCCEEDED, false",
            "SUCCEEDED, IN_PROGRESS, false",
    })
    void transitionDeployment(DeploymentStatus from, DeploymentStatus to, boolean allowed) {
        var deployment = newDeployment(from);

        if (allowed) {
            var updated = deployment.transitionTo(to, END);

            assertThat(updated.status()).isEqualTo(to);
            assertThat(updated.endTime()).isEqualTo(END);
        } else {
            assertThatThrownBy(() -> deployment.transitionTo(to, END))
                    .as("%s -> %s", from, to)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @ParameterizedTest
    @EnumSource(value = DeploymentStatus.class, names = {"SUCCEEDED", "FAILED"})
    void transitionEndTimeCannotBeBeforeStartTime(DeploymentStatus status) {
        var deployment = newDeployment(DeploymentStatus.IN_PROGRESS);
        assertThatThrownBy(() -> deployment.transitionTo(status, START.minusSeconds(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("end time");
    }

    @ParameterizedTest
    @EnumSource(value = DeploymentStatus.class, names = {"SUCCEEDED", "FAILED"})
    void cannotTransitionToWithNullEndDate(DeploymentStatus status) {
        var deployment = newDeployment(DeploymentStatus.IN_PROGRESS);
        assertThatThrownBy(() -> deployment.transitionTo(status, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("end time");
    }

    @ParameterizedTest
    @EnumSource(value = DeploymentStatus.class, names = {"SUCCEEDED", "FAILED"})
    void endTimeEqualsStartAllowed(DeploymentStatus status) {
        var deployment = newDeployment(DeploymentStatus.IN_PROGRESS);
        var finished = deployment.transitionTo(status, START);
        assertThat(finished.endTime()).isEqualTo(START);
        assertThat(finished.status()).isEqualTo(status);
    }

}
