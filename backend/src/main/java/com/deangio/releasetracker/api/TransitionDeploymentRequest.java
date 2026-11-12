package com.deangio.releasetracker.api;

import java.time.Instant;

import com.deangio.releasetracker.domain.DeploymentStatus;

public record TransitionDeploymentRequest(
        DeploymentStatus transitionTo,
        Instant endTime) {
}
