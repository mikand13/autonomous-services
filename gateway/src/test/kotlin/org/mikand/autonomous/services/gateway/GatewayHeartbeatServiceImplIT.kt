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
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle
import org.mikand.autonomous.services.gateway.utils.ConfigSupport

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class GatewayHeartbeatServiceImplIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    @RepeatedTest(5)
    fun testPing(vertx: Vertx, context: VertxTestContext) {
        val verticle = BridgeVerticle()
        val config = getTestConfig().put("bridgePort", findFreePort())
        val depOptions = DeploymentOptions().setConfig(config)

        vertx.deployVerticle(verticle, depOptions) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                    GatewayHeartbeatServiceImpl(vertx, config), Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                ServiceManager.getInstance(vertx).consumeService(HeartbeatService::class.java,
                        GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    it.result().ping(Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result()).isTrue()

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }

    @Test
    @RepeatedTest(5)
    fun testPingHttp(vertx: Vertx, context: VertxTestContext) {
        val verticle = GatewayDeploymentVerticle()
        val port = findFreePort()
        val config = getTestConfig().put("bridgePort", port)
        val depOptions = DeploymentOptions().setConfig(config)

        vertx.deployVerticle(verticle, depOptions) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            vertx.createHttpClient().getAbs("http://localhost:$port/eventbus-health")
                    .handler {
                        context.verify {
                            assertThat(it.statusCode() == 200).isTrue()

                            context.completeNow()
                        }
                    }
                    .exceptionHandler { context.failNow(it) }
                    .end()
        }
    }

    @Test
    @RepeatedTest(5)
    fun testFailedPing(vertx: Vertx, context: VertxTestContext) {
        val config = getTestConfig().put("bridgePort", findFreePort())

        ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                GatewayHeartbeatServiceImpl(vertx, config), Handler {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).consumeService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS, Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                it.result().ping(Handler {
                    context.verify {
                        assertThat(it.failed()).isTrue()

                        context.completeNow()
                    }
                })
            })
        })
    }

    @Test
    @RepeatedTest(5)
    fun testPingRuby(vertx: Vertx, context: VertxTestContext) {
        testLang(vertx, context, "rb/gateway_heartbeat_service_impl_test.rb")
    }

    private fun testLang(vertx: Vertx, context: VertxTestContext, langVerticle: String) {
        val verticle = BridgeVerticle()
        val config = getTestConfig().put("bridgePort", findFreePort())
        val depOptions = DeploymentOptions().setConfig(config)

        vertx.deployVerticle(verticle, depOptions) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).publishService(HeartbeatService::class.java,
                    GatewayDeploymentVerticle.GATEWAY_HEARTBEAT_ADDRESS,
                    GatewayHeartbeatServiceImpl(vertx, config), Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                vertx.deployVerticle(langVerticle) {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()

                        context.completeNow()
                    }
                }
            })
        }
    }
}
