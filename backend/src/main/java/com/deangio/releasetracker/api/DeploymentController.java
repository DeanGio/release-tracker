package com.deangio.releasetracker.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.deangio.releasetracker.domain.Deployment;
import com.deangio.releasetracker.repository.DeploymentRepository;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

public class DeploymentController {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final DeploymentRepository repository;

    public DeploymentController(DeploymentRepository repository) {
        this.repository = repository;
    }

    public void getById(Context ctx) {
        UUID id;
        try {
            id = UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Malformed deployment id");
        }

        Deployment deployment = repository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Deployment " + id + " not found"));

        ctx.json(deployment);
    }

    public void list(Context ctx) {
        int limit = ctx.queryParamAsClass("limit", Integer.class)
                .check(v -> v >= 1 && v <= MAX_LIMIT,
                        "limit must be between 1 and " + MAX_LIMIT)
                .getOrDefault(DEFAULT_LIMIT);

        int offset = ctx.queryParamAsClass("offset", Integer.class)
                .check(v -> v >= 0, "offset must be 0 or greater")
                .getOrDefault(0);

        var items = repository.findAll(limit, offset);
        long total = repository.count();

        ctx.json(PageResponse.of(items, limit, offset, total));
    }

    public void create(Context ctx) {
        final CreateDeploymentRequest req;
        try {
            req = ctx.bodyAsClass(CreateDeploymentRequest.class);
        } catch (Exception e) {
            throw new BadRequestResponse("Malformed body or unknown environment");
        }

        final Deployment deployment;
        try {
            deployment = Deployment.start(
                    req.serviceName(), req.version(), req.environment(), Instant.now().truncatedTo(ChronoUnit.MICROS));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestResponse(e.getMessage());
        }

        repository.save(deployment);
        ctx.header("Location", "/api/deployments/" + deployment.id());
        ctx.status(HttpStatus.CREATED).json(deployment);
    }

    public void updateStatus(Context ctx) {
        final UUID id;
        try {
            id = UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Malformed deployment id");
        }

        final TransitionDeploymentRequest req;
        try {
            req = ctx.bodyAsClass(TransitionDeploymentRequest.class);
        } catch (Exception e) {
            throw new BadRequestResponse("Malformed JSON body");
        }
        if (req.transitionTo() == null) {
            throw new BadRequestResponse("transitionTo is required");
        }

        Deployment deployment = repository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Deployment " + id + " not found"));

        Instant endTime = req.endTime() == null
                ? null
                : req.endTime().truncatedTo(ChronoUnit.MICROS);

        Deployment updated = deployment.transitionTo(req.transitionTo(), endTime);
        repository.update(updated);

        ctx.json(updated);
    }

}