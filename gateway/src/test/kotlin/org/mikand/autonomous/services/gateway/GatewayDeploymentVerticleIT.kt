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

package org.mikand.autonomous.services.gateway

import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.gateway.utils.ConfigSupport

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@RunWith(VertxUnitRunner::class)
class GatewayDeploymentVerticleIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun shouldDeployDeploymentVerticleWithSuccess(context : TestContext) {
        val config = getTestConfig().put("bridgePort", Integer.parseInt(System.getProperty("vertx.port")))
        val depOptions = DeploymentOptions().setConfig(config)
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(GatewayDeploymentVerticle(), depOptions, {
            context.assertTrue(it.succeeded())

            val busFuture = Future.future<Void>()
            val healthFuture = Future.future<Void>()

            vertx.createHttpClient()
                    .getAbs("http://localhost:${config.getInteger("bridgePort")}/eventbus")
                    .handler({
                        context.assertTrue(it.statusCode() == 200)
                        busFuture.complete()
                    })
                    .exceptionHandler({ context.fail(it) })
                    .end()

            vertx.createHttpClient()
                    .getAbs("http://localhost:${config.getInteger("bridgePort")}/eventbus-health")
                    .handler({
                        context.assertTrue(it.statusCode() == 200)
                        healthFuture.complete()
                    })
                    .exceptionHandler({ context.fail(it) })
                    .end()

            CompositeFuture.all(busFuture, healthFuture).setHandler({
                if (it.succeeded()) {
                    async.complete()
                } else {
                    context.fail(it.cause())
                }
            })
        })
    }
}
