/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017 Anders Mikkelsen
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

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

public class BackgroundGatewayLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {
    static {
        System.getProperties().setProperty(
                "vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
    }

    protected static final Logger logger = LoggerFactory.getLogger(GatewayLauncher.class.getSimpleName());
    @SuppressWarnings("WeakerAccess")
    protected GatewayDeploymentVerticle gatewayDeploymentVerticle;
    @SuppressWarnings("WeakerAccess")
    protected JsonObject config;

    public static void main(final String[] args) {
        logger.info("Running from main with: " + Arrays.toString(args));

        new BackgroundGatewayLauncher().dispatch(args);
    }

    public static void executeCommand(final String cmd, final String... args) {
        logger.info("Running from executeCommand with cmd: " + cmd + " and args: " + Arrays.toString(args));

        new BackgroundGatewayLauncher().execute(cmd, args);
    }

    @Override
    public void afterConfigParsed(final JsonObject config) {
        this.config = config == null ? new JsonObject() : config;

        logger.info("Config parsed for BackgroundGatewayLauncher: " + this.config.encodePrettily());
    }

    @Override
    public void beforeStartingVertx(final VertxOptions options) {

    }

    @Override
    public void afterStartingVertx(final Vertx vertx) {
        gatewayDeploymentVerticle = new GatewayDeploymentVerticle();

        final DeploymentOptions opts = new DeploymentOptions().setConfig(this.config);

        vertx.deployVerticle(gatewayDeploymentVerticle, opts, res -> {
            if (res.failed()) {
                logger.error("Failed to deploy Gateway!", res.cause());
            }
        });
    }

    @Override
    public void beforeDeployingVerticle(final DeploymentOptions deploymentOptions) {

    }

    @Override
    public void beforeStoppingVertx(final Vertx vertx) {

    }

    @Override
    public void afterStoppingVertx() {

    }

    @Override
    public void handleDeployFailed(final Vertx vertx,
                                   final String mainVerticle,
                                   final DeploymentOptions deploymentOptions,
                                   final Throwable cause) {

    }
}
