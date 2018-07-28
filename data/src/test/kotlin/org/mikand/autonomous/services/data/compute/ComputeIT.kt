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
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.data.model.JsonCompute
import org.mikand.autonomous.services.gateway.utils.ConfigSupport

@RunWith(VertxUnitRunner::class)
class ComputeIT : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun testCompute(context: TestContext) {
        val async = context.async()
        val compute = JsonCompute()

        ServiceManager.getInstance().publishService(Compute::class.java, compute, Handler {
            logger.error(it.cause())
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance().consumeService(Compute::class.java, Handler {
                context.assertTrue(it.succeeded())

                val computeService = it.result()
                val inputEvent = CommandEventBuilder()
                        .withSuccess()
                        .withAction("JSON_COMPUTE")
                        .build()

                computeService.compute(inputEvent, Handler {
                    context.assertTrue(it.succeeded())
                    async.complete()
                })
            })
        })
    }

    @Test
    fun testFetchSubscriptionAddress(context: TestContext) {
        val async = context.async()
        val compute = JsonCompute()

        ServiceManager.getInstance().publishService(Compute::class.java, compute, Handler {
            context.assertTrue(it.succeeded())

            ServiceManager.getInstance().consumeService(Compute::class.java, Handler {
                context.assertTrue(it.succeeded())

                val computeService = it.result()
                computeService.fetchSubscriptionAddress(Handler {
                    context.assertTrue(it.succeeded())
                    context.assertEquals(JsonCompute::class.java.simpleName,
                            it.result().body.getString("address"))
                    async.complete()
                })
            })
        })
    }
}