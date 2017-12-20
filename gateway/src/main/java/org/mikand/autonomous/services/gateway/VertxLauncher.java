package org.mikand.autonomous.services.gateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
public class VertxLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {
    static {
        System.getProperties().setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
    }

    private static final Logger logger = LoggerFactory.getLogger(VertxLauncher.class.getSimpleName());

    public static void main(String[] args) {
        logger.info("Running from main with: " + Arrays.toString(args));

        new VertxLauncher().dispatch(args);
    }

    public static void executeCommand(String cmd, String... args) {
        logger.info("Running from executeCommand with cmd: " + cmd + " and args: " + Arrays.toString(args));

        new VertxLauncher().execute(cmd, args);
    }

    @Override
    public void afterConfigParsed(JsonObject config) {

    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {

    }

    @Override
    public void afterStartingVertx(Vertx vertx) {

    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {

    }

    @Override
    public void beforeStoppingVertx(Vertx vertx) {

    }

    @Override
    public void afterStoppingVertx() {

    }

    @Override
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {

    }
}
