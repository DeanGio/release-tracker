package com.deangio.releasetracker.api;

import com.deangio.releasetracker.domain.TargetEnvironment;

public record CreateDeploymentRequest(
                String serviceName,
                String version,
                TargetEnvironment environment) {
}