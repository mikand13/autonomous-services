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

import com.nannoq.tools.cluster.services.HeartbeatService
import io.vertx.codegen.annotations.Fluent
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle.Companion.DEFAULT_BRIDGE_PATH
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle.Companion.DEFAULT_BRIDGE_PORT

internal class GatewayHeartbeatServiceImpl : HeartbeatService {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val vertx: Vertx
    private val config: JsonObject

    private val bridgePath: String
    private val bridgePort: Int

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(vertx: Vertx, config: JsonObject) {
        this.vertx = vertx
        this.config = config

        val customBridgePath = config.getString("bridgePath")

        bridgePath = if (customBridgePath == null) DEFAULT_BRIDGE_PATH else customBridgePath + DEFAULT_BRIDGE_PATH
        bridgePort = config.getInteger("bridgePort") ?: DEFAULT_BRIDGE_PORT
    }

    @Fluent
    override fun ping(resultHandler: Handler<AsyncResult<Boolean>>?): GatewayHeartbeatServiceImpl {
        val ssl = if (config.getBoolean("ssl") == true) "s" else ""
        val path = "http$ssl://localhost:$bridgePort${bridgePath.removeSuffix("/*")}"

        vertx.createHttpClient()
                .getAbs(path)
                .handler({
                    if (it.statusCode() == 200) {
                        resultHandler?.handle(Future.succeededFuture(true))
                    } else {
                        resultHandler?.handle(Future.failedFuture(it.statusMessage()))
                    }
                })
                .exceptionHandler({ resultHandler?.handle(Future.failedFuture(it.cause)) })
                .end()

        return this
    }
}