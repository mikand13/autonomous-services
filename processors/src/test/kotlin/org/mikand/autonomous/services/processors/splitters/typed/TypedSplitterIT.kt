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
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.processors.test.gen.TestModelSplitter
import org.mikand.autonomous.services.processors.test.gen.models.TestModel
import org.mikand.autonomous.services.processors.utils.ConfigSupport
import org.mikand.autonomous.services.processors.utils.TestModelRepository

@RunWith(VertxUnitRunner::class)
class TypedSplitterIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext({ Vertx.vertx() })

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun testSplitCreate(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.assertTrue(it.succeeded())

                        vertx.eventBus().consumer<JsonObject>(it.result()) {
                            context.assertNotNull(it.body())
                            async.complete()
                        }

                        service.splitCreate(TestModel())
                    })
                }
            }
        }))
    }

    @Test
    fun testSplitCreateWithReceipt(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.splitCreateWithReceipt(model, Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(model.getSomeStringOne(), it.result().getSomeStringOne())
                        async.complete()
                    })
                }
            }
        }))
    }

    @Test
    fun testSplitUpdate(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.assertTrue(it.succeeded())

                        vertx.eventBus().consumer<TestModel>(it.result()) {
                            context.assertNotNull(it.body())
                            async.complete()
                        }

                        service.splitUpdate(TestModel())
                    })
                }
            }
        }))
    }

    @Test
    fun testSplitUpdateWithReceipt(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.splitUpdateWithReceipt(model, Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(model.getSomeStringOne(), it.result().getSomeStringOne())
                        async.complete()
                    })
                }
            }
        }))
    }

    @Test
    fun testSplitDelete(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.assertTrue(it.succeeded())

                        vertx.eventBus().consumer<TestModel>(it.result()) {
                            context.assertNotNull(it.body())
                            async.complete()
                        }

                        service.splitDelete(TestModel())
                    })
                }
            }
        }))
    }

    @Test
    fun testSplitDeleteWithReceipt(context: TestContext) {
        val splitter = TestModelRepository()
        val async = context.async()
        val vertx = rule.vertx()
        val model = TestModel()
        model.setSomeStringOne("String")

        vertx.deployVerticle(splitter, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(TestModelSplitter::class.java, splitter) {
                ServiceManager.getInstance().consumeService(TestModelSplitter::class.java) {
                    context.assertTrue(it.succeeded())
                    val service = it.result()

                    service.splitDeleteWithReceipt(model, Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(model.getSomeStringOne(), it.result().getSomeStringOne())
                        async.complete()
                    })
                }
            }
        }))
    }
}