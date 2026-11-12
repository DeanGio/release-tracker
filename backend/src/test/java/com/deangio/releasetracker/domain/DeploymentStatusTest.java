package com.deangio.releasetracker.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentStatusTest {

    @Test
    void inProgressCanMoveToSucceeded() {
        assertThat(DeploymentStatus.IN_PROGRESS.canTransitionTo(DeploymentStatus.SUCCEEDED)).isTrue();
    }

    @Test
    void inProgressCanMoveToFailed() {
        assertThat(DeploymentStatus.IN_PROGRESS.canTransitionTo(DeploymentStatus.FAILED)).isTrue();
    }

    @Test
    void inProgressCannotMoveToItself() {
        assertThat(DeploymentStatus.IN_PROGRESS.canTransitionTo(DeploymentStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void inProgressIsNotTerminal() {
        assertThat(DeploymentStatus.IN_PROGRESS.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DeploymentStatus.class)
    void onlyFinishedStatusesAreTerminal(DeploymentStatus status) {
        assertThat(status.isTerminal()).isEqualTo(status != DeploymentStatus.IN_PROGRESS);
    }

    @Test
    void nextStatusCannotBeNull() {
        assertThatThrownBy(() -> DeploymentStatus.IN_PROGRESS.canTransitionTo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("next");
    }

}