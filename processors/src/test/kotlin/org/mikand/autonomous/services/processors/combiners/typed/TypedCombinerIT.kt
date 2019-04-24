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

package org.mikand.autonomous.services.processors.combiners.typed

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.processors.test.gen.TestModelCombiner
import org.mikand.autonomous.services.processors.utils.ConfigSupport
import org.mikand.autonomous.services.processors.utils.TestModelRepository

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class TypedCombinerIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun testCombineRead(vertx: Vertx, context: VertxTestContext) {
        val combiner = TestModelRepository()

        vertx.deployVerticle(combiner) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).publishService(TestModelCombiner::class.java, combiner, Handler {
                ServiceManager.getInstance().consumeService(TestModelCombiner::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()

                    service.combineRead(JsonObject(), Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }

    @Test
    fun testCombineReadAll(vertx: Vertx, context: VertxTestContext) {
        val combiner = TestModelRepository()

        vertx.deployVerticle(combiner) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance(vertx).publishService(TestModelCombiner::class.java, combiner, Handler {
                ServiceManager.getInstance().consumeService(TestModelCombiner::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()

                    service.combineReadAll(JsonObject(), Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }
}