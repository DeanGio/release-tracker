package com.deangio.releasetracker.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Deployment(
        UUID id,
        String serviceName,
        String version,
        TargetEnvironment environment,
        DeploymentStatus status,
        Instant startTime,
        Instant endTime) {

    public Deployment {
        id = Objects.requireNonNull(id, "id must not be null");
        serviceName = requireText(serviceName, "service name");
        version = requireText(version, "version");
        Objects.requireNonNull(environment, "target environment must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(startTime, "start time must not be null");

        if (status.isTerminal() && endTime == null) {
            throw new IllegalArgumentException("finished deployment needs an end time");
        }
        if (!status.isTerminal() && endTime != null) {
            throw new IllegalArgumentException("in-progress deployment cannot have an end time");
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("end time must not be before start time");
        }
    }

    public static Deployment start(String serviceName, String version, TargetEnvironment targetEnvironment,
            Instant startTime) {
        return new Deployment(UUID.randomUUID(), serviceName, version, targetEnvironment, DeploymentStatus.IN_PROGRESS,
                startTime, null);
    }

    public Deployment transitionTo(DeploymentStatus next, Instant at) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "cannot move deployment from " + status + " to " + next);
        }
        return new Deployment(id, serviceName, version, environment, next, startTime, at);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return stripped;
    }

}
