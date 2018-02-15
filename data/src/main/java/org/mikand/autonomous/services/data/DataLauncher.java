/*
 * MIT License
 *
 * Copyright (c) 2017 Anders Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.mikand.autonomous.services.data;

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
public class DataLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {
    static {
        System.getProperties().setProperty(
                "vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
    }

    private static final Logger logger = LoggerFactory.getLogger(DataLauncher.class.getSimpleName());

    public static void main(final String[] args) {
        logger.info("Running from main with: " + Arrays.toString(args));

        new DataLauncher().dispatch(args);
    }

    public static void executeCommand(final String cmd, final String... args) {
        logger.info("Running from executeCommand with cmd: " + cmd + " and args: " + Arrays.toString(args));

        new DataLauncher().execute(cmd, args);
    }

    @Override
    public void afterConfigParsed(final JsonObject config) {

    }

    @Override
    public void beforeStartingVertx(final VertxOptions options) {

    }

    @Override
    public void afterStartingVertx(final Vertx vertx) {

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
