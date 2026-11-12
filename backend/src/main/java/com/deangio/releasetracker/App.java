package com.deangio.releasetracker;

import com.deangio.releasetracker.api.DeploymentController;
import com.deangio.releasetracker.api.Routes;
import com.deangio.releasetracker.config.AppConfig;
import com.deangio.releasetracker.repository.DeploymentRepository;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {

    public static void main(String[] args) {
        new App().run();
    }

    private void run() {

        var dataSource = AppConfig.configDataSource();
        var controller = new DeploymentController(new DeploymentRepository(dataSource));

        var app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(AppConfig.mapper(), false));
            Routes.register(config, controller);
        });
        app.start(7070);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            dataSource.close();
        }));
    }

}
