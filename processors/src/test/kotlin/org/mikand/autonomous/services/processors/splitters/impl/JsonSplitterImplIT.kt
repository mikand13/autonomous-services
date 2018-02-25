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

package org.mikand.autonomous.services.processors.splitters.impl

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.processors.splitters.concretes.JsonSplitter
import org.mikand.autonomous.services.processors.utils.ConfigSupport

@RunWith(VertxUnitRunner::class)
class JsonSplitterImplIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun testSplit(context: TestContext) {
        val splitter = JsonSplitterImpl()
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(splitter, {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance().consumeService(JsonSplitter::class.java) {
                context.assertTrue(it.succeeded())
                val service = it.result()

                service.fetchSubscriptionAddress(Handler {
                    context.assertTrue(it.succeeded())

                    vertx.eventBus().consumer<JsonObject>(it.result()) {
                        context.assertNotNull(it.body())
                        async.complete()
                    }

                    service.split(JsonObject())
                })
            }
        })
    }

    @Test
    fun testSplitWithReceipt(context: TestContext) {
        val splitter = JsonSplitterImpl()
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(splitter, {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance().consumeService(JsonSplitter::class.java) {
                context.assertTrue(it.succeeded())
                val service = it.result()

                service.fetchSubscriptionAddress(Handler {
                    context.assertTrue(it.succeeded())

                    vertx.eventBus().consumer<JsonObject>(it.result()) {
                        context.assertNotNull(it.body())
                        async.complete()
                    }

                    service.splitWithReceipt(JsonObject(), Handler {
                        context.assertEquals(200, it.result().statusCode, "Statuscode is not 200!")
                    })
                })
            }
        })
    }
}