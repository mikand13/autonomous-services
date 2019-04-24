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

package org.mikand.autonomous.services.processors.splitters.typed

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
import org.mikand.autonomous.services.processors.test.gen.TestModelSplitter
import org.mikand.autonomous.services.processors.test.gen.models.TestModel
import org.mikand.autonomous.services.processors.utils.ConfigSupport
import org.mikand.autonomous.services.processors.utils.TestModelRepository

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class TypedSplitterIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun testSplitCreate(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler { result ->
                    context.verify {
                        assertThat(result.succeeded()).isTrue()
                    }

                    val service = result.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        vertx.eventBus().consumer<JsonObject>(it.result()) {
                            context.verify {
                                assertThat(it).isNotNull

                                context.completeNow()
                            }
                        }

                        service.splitCreate(TestModel())
                    })
                })
            })
        }
    }

    @Test
    fun testSplitCreateWithReceipt(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()

                    service.splitCreateWithReceipt(model, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result().getSomeStringOne()).isEqualTo(model.getSomeStringOne())

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }

    @Test
    fun testSplitUpdate(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler { result ->
                    context.verify {
                        assertThat(result.succeeded()).isTrue()
                    }

                    val service = result.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        vertx.eventBus().consumer<TestModel>(it.result()) {
                            context.verify {
                                assertThat(it.body()).isNotNull

                                context.completeNow()
                            }
                        }

                        service.splitUpdate(TestModel())
                    })
                })
            })
        }
    }

    @Test
    fun testSplitUpdateWithReceipt(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()

                    service.splitUpdateWithReceipt(model, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result().getSomeStringOne()).isEqualTo(model.getSomeStringOne())

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }

    @Test
    fun testSplitDelete(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler { result ->
                    context.verify {
                        assertThat(result.succeeded()).isTrue()
                    }

                    val service = result.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        vertx.eventBus().consumer<TestModel>(it.result()) {
                            context.verify {
                                assertThat(it.body()).isNotNull

                                context.completeNow()
                            }
                        }

                        service.splitDelete(TestModel())
                    })
                })
            })
        }
    }

    @Test
    fun testSplitDeleteWithReceipt(vertx: Vertx, context: VertxTestContext) {
        val splitter = TestModelRepository()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter, Handler {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()

                    service.splitDeleteWithReceipt(model, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result().getSomeStringOne()).isEqualTo(model.getSomeStringOne())

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }
}