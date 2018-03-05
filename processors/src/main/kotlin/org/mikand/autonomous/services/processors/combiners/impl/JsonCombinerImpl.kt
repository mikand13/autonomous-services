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

package org.mikand.autonomous.services.processors.combiners.impl

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.codegen.annotations.Fluent
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import org.mikand.autonomous.services.processors.combiners.concretes.JsonCombiner

abstract class JsonCombinerImpl(config: JsonObject = JsonObject()) : AbstractVerticle(), JsonCombiner {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val publishAddress: String = config.getString("customPublishAddress") ?: JsonCombiner::class.java.simpleName
    private val deployService: Boolean = config.getBoolean("deployAsService") ?: true
    private val subscriptionAddress: String = config.getString("customSubscriptionAddress") ?: javaClass.name
    @Suppress("unused")
    private val thisVertx: Vertx = vertx ?: Vertx.currentContext().owner()

    private lateinit var service: Record

    override fun start(startFuture: Future<Void>?) {
        if (deployService) {
            ServiceManager.getInstance().publishService(JsonCombiner::class.java, publishAddress, this) {
                if (it.succeeded()) {
                    service = it.result()

                    startFuture?.complete()
                } else {
                    startFuture?.fail(it.cause())
                }
            }
        } else {
            startFuture?.complete()
        }
    }

    override fun stop(stopFuture: Future<Void>?) {
        if (deployService) {
            ServiceManager.getInstance().unPublishService(JsonCombiner::class.java, service) {
                if (it.succeeded()) {
                    stopFuture?.complete()
                } else {
                    stopFuture?.fail(it.cause())
                }
            }
        } else {
            stopFuture?.complete()
        }
    }

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): JsonCombiner {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}