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

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.gateway.utils.ConfigSupport
import java.net.ServerSocket

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class GatewayDeploymentVerticleIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    companion object {
        private val mapSet = mutableSetOf<Int>()

        @JvmStatic
        @Synchronized
        private fun getPort(): Int {
            val use = ServerSocket(0).use { it.localPort }

            if (mapSet.contains(use)) {
                return getPort()
            } else {
                mapSet.add(use)

                return use
            }
        }
    }

    @Test
    fun shouldDeployDeploymentVerticleWithSuccess(vertx: Vertx, context: VertxTestContext) {
        val checkpoint = context.checkpoint(2)
        val config = getTestConfig().put("bridgePort", getPort())
        val depOptions = DeploymentOptions().setConfig(config)

        vertx.deployVerticle(GatewayDeploymentVerticle(), depOptions) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            vertx.createHttpClient()
                    .getAbs("http://localhost:${config.getInteger("bridgePort")}/eventbus")
                    .handler { checkpoint.flag() }
                    .exceptionHandler { context.failNow(it) }
                    .end()

            vertx.createHttpClient()
                    .getAbs("http://localhost:${config.getInteger("bridgePort")}/eventbus-health")
                    .handler { checkpoint.flag() }
                    .exceptionHandler { context.failNow(it) }
                    .end()
        }
    }
}
