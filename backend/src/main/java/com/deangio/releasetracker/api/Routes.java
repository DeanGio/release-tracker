package com.deangio.releasetracker.api;

import java.util.Map;

import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;

public final class Routes {

    public static void register(JavalinConfig config, DeploymentController controller) {
        config.routes.get("/api/deployments", controller::list)
                .get("/api/deployments/{id}", controller::getById)
                .post("/api/deployments", controller::create)
                .patch("/api/deployments/{id}/status", controller::updateStatus)

                .exception(IllegalStateException.class,
                        (e, ctx) -> ctx.status(HttpStatus.CONFLICT).json(Map.of("error", e.getMessage())))
                .exception(IllegalArgumentException.class,
                        (e, ctx) -> ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage())));
    }
}