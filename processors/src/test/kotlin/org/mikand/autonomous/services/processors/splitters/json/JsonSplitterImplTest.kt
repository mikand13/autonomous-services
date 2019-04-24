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

package org.mikand.autonomous.services.processors.splitters.json

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
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
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.processors.splitters.impl.JsonSplitterImpl
import org.mikand.autonomous.services.processors.utils.ConfigSupport

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class JsonSplitterImplTest : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun testSplitWithReceipt(vertx: Vertx, context: VertxTestContext) {
        vertx.runOnContext {
            val splitter = JsonSplitterImpl()

            splitter.splitWithReceipt(CommandEventBuilder()
                    .withSuccess()
                    .withAction("SPLIT")
                    .build(), Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                    assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(200)

                    context.completeNow()
                }
            })
        }
    }

    @Test
    fun testSplitWithReceiptAndDataReception(vertx: Vertx, context: VertxTestContext) {
        vertx.runOnContext {
            val splitter = JsonSplitterImpl()

            splitter.fetchSubscriptionAddress(Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                val address = it.result()

                context.verify {
                    assertThat(address).isNotNull
                }

                vertx.eventBus().consumer<JsonObject>(address.body.getString("address")).handler {
                    context.verify {
                        assertThat(it.body()).isNotNull

                        context.completeNow()
                    }
                }

                splitter.splitWithReceipt(CommandEventBuilder()
                        .withSuccess()
                        .withAction("SPLIT")
                        .build(), Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                        assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(200)

                        context.completeNow()
                    }
                })
            })
        }
    }

    @Test
    fun testSplitWithReceiptAndDataReceptionAndDataObject(vertx: Vertx, context: VertxTestContext) {
        vertx.runOnContext {
            val extractsArray = JsonArray()
                    .add("someStringOne")
                    .add("someObjectOne.someObjectOneStringOne")
                    .add("someObjectTwo.someObjectTwoObjectOne.someObjectTwoObjectOneStringOne")
            val splitter = JsonSplitterImpl(JsonObject()
                    .put("extractables", extractsArray))
            val testObject = JsonObject()
                    .put("someStringOne", "someStringOne")
                    .put("someStringTwo", "someStringTwo")
                    .put("someObjectOne", JsonObject()
                            .put("someObjectOneStringOne", "someObjectOneStringOne")
                            .put("someObjectOneStringTwo", "someObjectOneStringTwo"))
                    .put("someObjectTwo", JsonObject()
                            .put("someObjectTwoStringOne", "someObjectTwoStringOne")
                            .put("someObjectTwoStringTwo", "someObjectTwoStringTwo")
                            .put("someObjectTwoObjectOne", JsonObject()
                                    .put("someObjectTwoObjectOneStringOne", "someObjectTwoObjectOneStringOne")
                                    .put("someObjectTwoObjectOneStringTwo", "someObjectTwoObjectOneStringTwo")))

            splitter.fetchSubscriptionAddress(Handler { result ->
                context.verify {
                    assertThat(result.succeeded()).isTrue()
                }

                val address = result.result()

                context.verify {
                    assertThat(address).isNotNull
                }

                vertx.eventBus().consumer<JsonObject>(address.body.getString("address")).handler {
                    val body = DataEventImpl(it.body()).body

                    context.verify {
                        assertThat(body).isNotNull
                        assertThat(body.getJsonObject("someObjectOne")).isNotNull
                        assertThat(body
                                .getJsonObject("someObjectTwo")
                                .getJsonObject("someObjectTwoObjectOne")).isNotNull
                        assertThat(body.size()).isEqualTo(3)
                        assertThat(body.getJsonObject("someObjectOne").size()).isEqualTo(1)
                        assertThat(body
                                .getJsonObject("someObjectTwo")
                                .getJsonObject("someObjectTwoObjectOne").size()).isEqualTo(1)
                        assertThat(body.getString("someStringOne")).isEqualTo("someStringOne")
                        assertThat(body
                                .getJsonObject("someObjectOne")
                                .getString("someObjectOneStringOne"))
                                .isEqualTo("someObjectOneStringOne")
                        assertThat(body
                                .getJsonObject("someObjectTwo")
                                .getJsonObject("someObjectTwoObjectOne")
                                .getString("someObjectTwoObjectOneStringOne"))
                                .isEqualTo("someObjectTwoObjectOneStringOne")

                        context.completeNow()
                    }
                }

                val input = CommandEventBuilder()
                        .withSuccess()
                        .withAction("SPLIT")
                        .withBody(testObject)
                        .build()

                splitter.splitWithReceipt(input, Handler {
                    context.verify {
                        assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(200)
                    }
                })
            })
        }
    }
}