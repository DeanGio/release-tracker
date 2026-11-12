package com.deangio.releasetracker.domain;

import java.util.Objects;

public enum DeploymentStatus {
    IN_PROGRESS,
    SUCCEEDED,
    FAILED;

    public boolean canTransitionTo(DeploymentStatus next) {
        Objects.requireNonNull(next, "next");
        return this == IN_PROGRESS && (next == SUCCEEDED || next == FAILED);
    }

    public boolean isTerminal() {
        return this != IN_PROGRESS;
    }
}
