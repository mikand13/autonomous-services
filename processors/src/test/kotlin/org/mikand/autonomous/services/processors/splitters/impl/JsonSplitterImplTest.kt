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

import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
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
import org.mikand.autonomous.services.processors.utils.ConfigSupport

@RunWith(VertxUnitRunner::class)
class JsonSplitterImplTest : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun testSplitWithReceipt(context: TestContext) {
        val async = context.async()
        val splitter = JsonSplitterImpl()

        splitter.splitWithReceipt(JsonObject(), Handler {
            context.assertEquals(200, it.result().statusCode, "Statuscode is not 200!")
            async.complete()
        })
    }

    @Test
    fun testSplitWithReceiptAndDataReception(context: TestContext) {
        val async = context.async()
        val splitter = JsonSplitterImpl()

        splitter.fetchSubscriptionAddress(Handler {
            context.assertTrue(it.succeeded())

            val address = it.result()

            context.assertNotNull(address, "Address is null!")

            rule.vertx().eventBus().consumer<JsonObject>(address).handler({
                context.assertNotNull(it.body(), "Body is null!")
                async.complete()
            })

            splitter.splitWithReceipt(JsonObject(), Handler {
                context.assertEquals(200, it.result().statusCode, "Statuscode is not 200!")
            })
        })
    }

    @Test
    fun testSplitWithReceiptAndDataReceptionAndDataObject(context: TestContext) {
        val async = context.async()
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

        splitter.fetchSubscriptionAddress(Handler {
            context.assertTrue(it.succeeded())

            val address = it.result()

            context.assertNotNull(address, "Address is null!")

            rule.vertx().eventBus().consumer<JsonObject>(address).handler({
                val body = it.body()

                context.assertNotNull(body, "Body is null!")
                context.assertNotNull(body.getJsonObject("someObjectOne"), "Object 1 is null!")
                context.assertNotNull(body
                        .getJsonObject("someObjectTwo")
                        .getJsonObject("someObjectTwoObjectOne"), "Object 2 -> 1 is null!")
                context.assertEquals(3, body.size(), "Extracted more than expected!")
                context.assertEquals(1, body.getJsonObject("someObjectOne").size(),
                        "Extracted > than expected in object 1!")
                context.assertEquals(1, body
                        .getJsonObject("someObjectTwo")
                        .getJsonObject("someObjectTwoObjectOne").size(),
                        "Extracted more than expected in Object2 -> 1!")
                context.assertEquals("someStringOne", body.getString("someStringOne"))
                context.assertEquals("someObjectOneStringOne", body
                        .getJsonObject("someObjectOne")
                        .getString("someObjectOneStringOne"))
                context.assertEquals("someObjectTwoObjectOneStringOne", body
                        .getJsonObject("someObjectTwo")
                        .getJsonObject("someObjectTwoObjectOne")
                        .getString("someObjectTwoObjectOneStringOne"))

                async.complete()
            })

            splitter.splitWithReceipt(testObject, Handler {
                context.assertEquals(200, it.result().statusCode, "Statuscode is not 200!")
            })
        })
    }
}