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

import com.nannoq.tools.cluster.services.HeartbeatService
import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.Repeat
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle
import org.mikand.autonomous.services.gateway.utils.ConfigSupport

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@RunWith(VertxUnitRunner::class)
class GatewayHeartbeatServiceImplIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext({
        val options = VertxOptions()
                .setMaxWorkerExecuteTime(Long.MAX_VALUE)
                .setMaxEventLoopExecuteTime(Long.MAX_VALUE)
        
        Vertx.vertx(options)
    })

    @JvmField
    @Rule
    val timeout = Timeout.seconds(15)

    @Test
    @Repeat(50)
    fun testPing(context: TestContext) {
        val async = context.async()
        val verticle = BridgeVerticle()
        val config = getTestConfig().put("bridgePort", findFreePort())
        val depOptions = DeploymentOptions().setConfig(config)
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, depOptions, {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                    GatewayHeartbeatServiceImpl(vertx, config), Handler {
                context.assertTrue(it.succeeded())

                ServiceManager.getInstance().consumeService(HeartbeatService::class.java,
                        GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS, Handler {
                    context.assertTrue(it.succeeded())

                    it.result().ping(Handler {
                        context.assertTrue(it.succeeded())
                        context.assertTrue(it.result())

                        async.complete()
                    })
                })
            })
        })
    }

    @Test
    @Repeat(50)
    fun testPingHttp(context: TestContext) {
        val async = context.async()
        val verticle = GatewayDeploymentVerticle()
        val port = findFreePort()
        val config = getTestConfig().put("bridgePort", port)
        val depOptions = DeploymentOptions().setConfig(config)
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, depOptions, {
            context.assertTrue(it.succeeded())

            vertx.createHttpClient().getAbs("http://localhost:$port/eventbus-health")
                    .handler({
                        context.assertTrue(it.statusCode() == 200)

                        async.complete()
                    })
                    .exceptionHandler({ context.fail(it) })
                    .end()
        })
    }

    @Test
    @Repeat(50)
    fun testFailedPing(context: TestContext) {
        val async = context.async()
        val config = getTestConfig().put("bridgePort", findFreePort())
        val vertx = rule.vertx()

        ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                GatewayHeartbeatServiceImpl(vertx, config), Handler {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance().consumeService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS, Handler {
                context.assertTrue(it.succeeded())

                it.result().ping(Handler {
                    context.assertTrue(it.failed())
                    context.assertNull(it.result())

                    async.complete()
                })
            })
        })
    }

    @Test
    @Repeat(50)
    fun testPingRuby(context: TestContext) {
        testLang(context, "rb/gateway_heartbeat_service_impl_test.rb")
    }

    @Test
    @Repeat(50)
    fun testPingJs(context: TestContext) {
        testLang(context, "js/gatewayHeartbeatServiceImplTest.js")
    }

    fun testLang(context: TestContext, langVerticle: String) {
        val async = context.async()
        val verticle = BridgeVerticle()
        val config = getTestConfig().put("bridgePort", findFreePort())
        val depOptions = DeploymentOptions().setConfig(config)
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, depOptions, {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                    GatewayHeartbeatServiceImpl(vertx, config), Handler {
                context.assertTrue(it.succeeded())

                vertx.deployVerticle(langVerticle, {
                    if (it.succeeded()) {
                        async.complete()
                    } else {
                        context.fail(it.cause())
                    }
                })
            })
        })
    }
}

