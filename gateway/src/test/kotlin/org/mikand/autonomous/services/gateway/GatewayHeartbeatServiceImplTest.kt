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

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@RunWith(VertxUnitRunner::class)
class GatewayHeartbeatServiceImplTest {
    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @Test
    fun testPing(context: TestContext) {
        val async = context.async()
        val verticle = BridgeVerticle()
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, {
            context.assertTrue(it.succeeded())
            val id = it.result()

            ServiceManager.getInstance(vertx)
                    .publishService(GatewayHeartbeatService::class.java, GatewayHeartbeatServiceImpl(vertx, id), {
                        context.assertTrue(it.succeeded())

                        ServiceManager.getInstance().consumeService(GatewayHeartbeatService::class.java, {
                            context.assertTrue(it.succeeded())

                            it.result().ping({
                                context.assertTrue(it.succeeded())
                                context.assertTrue(it.result())

                                async.complete()
                            })
                        })
                    })
        })
    }

    @Test
    fun testFailedPing(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()

        ServiceManager.getInstance(vertx)
                .publishService(GatewayHeartbeatService::class.java, GatewayHeartbeatServiceImpl(vertx, ""), {
                    context.assertTrue(it.succeeded())

                    ServiceManager.getInstance().consumeService(GatewayHeartbeatService::class.java, {
                        context.assertTrue(it.succeeded())

                        it.result().ping({
                            context.assertTrue(it.succeeded())
                            context.assertTrue(it.result())

                            async.complete()
                        })
                    })
                })
    }
}