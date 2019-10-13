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
 *
 */

package org.mikand.autonomous.services.data.compute

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.data.model.JsonCompute
import org.mikand.autonomous.services.data.utils.ConfigSupport

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class ComputeIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun testCompute(vertx: Vertx, context: VertxTestContext) {
        val compute = JsonCompute()

        ServiceManager.getInstance(vertx).publishService(Compute::class.java, compute, Handler { result ->
            logger.error(result.cause())

            context.verify {
                assertThat(result.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).consumeService(Compute::class.java, Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                val computeService = it.result()
                val inputEvent = CommandEventBuilder()
                        .withSuccess()
                        .withAction("JSON_COMPUTE")
                        .build()

                computeService.compute(inputEvent, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()

                        context.completeNow()
                    }
                })
            })
        })
    }

    @Test
    fun testFetchSubscriptionAddress(vertx: Vertx, context: VertxTestContext) {
        val compute = JsonCompute()

        ServiceManager.getInstance(vertx).publishService(Compute::class.java, compute, Handler {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).consumeService(Compute::class.java, Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                val computeService = it.result()
                computeService.fetchSubscriptionAddress(Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                        assertThat(JsonCompute::class.java.simpleName)
                                .isEqualTo(it.result().body.getString("address"))

                        context.completeNow()
                    }
                })
            })
        })
    }
}
