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

package org.mikand.autonomous.services.gateway.bridge

import com.nannoq.tools.cluster.services.HeartbeatService
import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.bridge.BridgeEventType.PUBLISH
import io.vertx.ext.bridge.BridgeEventType.RECEIVE
import io.vertx.ext.bridge.BridgeEventType.REGISTER
import io.vertx.ext.bridge.BridgeEventType.SEND
import io.vertx.ext.bridge.BridgeEventType.SOCKET_CLOSED
import io.vertx.ext.bridge.BridgeEventType.SOCKET_CREATED
import io.vertx.ext.bridge.BridgeEventType.SOCKET_IDLE
import io.vertx.ext.bridge.BridgeEventType.SOCKET_PING
import io.vertx.ext.bridge.BridgeEventType.UNREGISTER
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions
import java.util.ArrayList
import org.mikand.autonomous.services.gateway.GatewayDeploymentVerticle.Companion.GATEWAY_HEARTBEAT_ADDRESS

internal class BridgeVerticle() : AbstractVerticle() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    companion object {
        const val DEFAULT_BRIDGE_BASE = "org.mikand.autonomous.services"
        const val DEFAULT_BRIDGE_PATH = "/eventbus/*"
        const val DEFAULT_BRIDGE_PORT = 5443
        const val DEFAULT_BRIDGE_BASE_API = ".processors"
        const val DEFAULT_BRIDGE_BASE_DATA = ".data"
        const val DEFAULT_BRIDGE_BASE_STORAGE = ".storage"
    }

    private var bridgeHandlerOptions: SockJSHandlerOptions? = null
    private var router: Router? = null
    private var httpServerOptions: HttpServerOptions? = null

    private var server: HttpServer? = null

    @Suppress("unused")
    constructor(bridgeHandlerOptions: SockJSHandlerOptions) : this(null, bridgeHandlerOptions, null)
    @Suppress("unused")
    constructor(router: Router) : this(router, null, null)
    @Suppress("unused")
    constructor(httpServerOptions: HttpServerOptions) : this(null, null, httpServerOptions)

    constructor(
        router: Router?,
        bridgeHandlerOptions: SockJSHandlerOptions?,
        httpServerOptions: HttpServerOptions?
    ) : this() {
        this.router = router
        this.bridgeHandlerOptions = bridgeHandlerOptions
        this.httpServerOptions = httpServerOptions
    }

    override fun start(startFuture: Future<Void>?) {
        val customBridgePath = config().getString("bridgePath")

        val bridgeBase = config().getString("bridgeBase") ?: DEFAULT_BRIDGE_BASE
        val bridgePath = if (customBridgePath == null) DEFAULT_BRIDGE_PATH else customBridgePath + DEFAULT_BRIDGE_PATH
        val bridgePort = config().getInteger("bridgePort") ?: DEFAULT_BRIDGE_PORT

        startBridge(bridgeBase, bridgePath, bridgePort, startFuture)
    }

    private fun startBridge(bridgeBase: String, bridgePath: String, bridgePort: Int, startFuture: Future<Void>?) {
        val options = bridgeHandlerOptions ?: SockJSHandlerOptions()
                .setHeartbeatInterval(150000L)
                .setInsertJSESSIONID(false)
        val router = router ?: Router.router(vertx)
        val ebHandler = SockJSHandler.create(vertx, options)

        addHealthCheck(router, bridgePath)
        addEventBusBridge(ebHandler, router, bridgeBase, bridgePath)
        deployBridge(router, bridgePort, startFuture)
    }

    private fun addHealthCheck(router: Router, bridgePath: String) {
        val healthCheckHandler = HealthCheckHandler.create(vertx)

        healthCheckHandler.register("bridge-is-live") { future ->
            ServiceManager.getInstance().consumeService(HeartbeatService::class.java, GATEWAY_HEARTBEAT_ADDRESS, Handler { result ->
                when {
                    result.succeeded() -> result.result().ping(Handler {
                        when {
                            it.succeeded() && it.result() ->
                                if (!future.future().isComplete) future.complete(Status.OK(JsonObject().put("bridge", "UP")))
                            else -> {
                                logger.error("Failed heartbeat ping!", it.cause())

                                if (!future.future().isComplete) future.complete(Status.KO(JsonObject().put("bridge", "DOWN")))
                            }
                        }
                    })
                    else -> {
                        logger.error("Failed fetching HeartBeatService!", result.cause())

                        if (!future.future().isComplete) future.complete(Status.KO(JsonObject().put("bridge", "DOWN")))
                    }
                }
            })
        }

        router.get(bridgePath.removeSuffix("/*") + "-health").handler(healthCheckHandler)
    }

    private fun addEventBusBridge(
        ebHandler: SockJSHandler,
        router: Router,
        bridgeBase: String,
        bridgePath: String
    ) {
        ebHandler.bridge(createBridgeOptions(bridgeBase)) { bridgeEvent ->
            logger.debug("Event received from external client!")

            when {
                bridgeEvent.type() != null -> when (bridgeEvent.type()) {
                    SEND -> {
                        logger.debug("Send Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    PUBLISH -> {
                        logger.debug("Publish Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    RECEIVE -> {
                        logger.debug("Receive Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    REGISTER -> {
                        logger.debug("Register Event is: " + Json.encodePrettily(bridgeEvent))

                        authorize(bridgeEvent)
                    }
                    UNREGISTER -> {
                        logger.debug("UnRegister Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    SOCKET_IDLE -> {
                        logger.debug("Socket Idle Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    SOCKET_PING -> {
                        logger.debug("Socket Ping Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    SOCKET_CLOSED -> {
                        logger.debug("Socket Closed Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    SOCKET_CREATED -> {
                        logger.debug("Socket Created Event is: " + Json.encodePrettily(bridgeEvent))

                        bridgeEvent.complete(true)
                    }
                    else -> {
                        logger.error("Unknown bridgevent!")

                        bridgeEvent.complete(true)
                    }
                }
                else -> logger.error("Type is null!, Message is: " + Json.encodePrettily(bridgeEvent))
            }
        }

        logger.info("Adding bridge to: $bridgePath")

        router.route(bridgePath).handler(CookieHandler.create())
        router.route(bridgePath).handler(ebHandler)
    }

    private fun authorize(bridgeEvent: BridgeEvent) {
        val rawMessage = bridgeEvent.rawMessage
        val address = rawMessage.getString("address")

        when {
            address != null -> bridgeEvent.complete(true)
            else -> bridgeEvent.fail("Unknown address!")
        }
    }

    private fun createBridgeOptions(bridgeBase: String): BridgeOptions {
        val opts = BridgeOptions()
        opts.setInboundPermitted(createInAdresses(bridgeBase))
        opts.setOutboundPermitted(createOutAdresses(bridgeBase))

        return opts
    }

    private fun createInAdresses(bridgeBase: String): List<PermittedOptions> {
        val permissions = ArrayList<PermittedOptions>()
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_API).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_DATA).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_STORAGE).*$"))
        permissions.add(PermittedOptions().setAddress(GATEWAY_HEARTBEAT_ADDRESS))

        return permissions
    }

    private fun createOutAdresses(bridgeBase: String): List<PermittedOptions> {
        val permissions = ArrayList<PermittedOptions>()
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_API).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_DATA).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_STORAGE).*$"))
        permissions.add(PermittedOptions().setAddress(GATEWAY_HEARTBEAT_ADDRESS))

        return permissions
    }

    private fun deployBridge(router: Router, bridgePort: Int, startFuture: Future<Void>?) {
        val options = httpServerOptions ?: HttpServerOptions()
                .setCompressionSupported(false)
                .setTcpKeepAlive(true)

        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(bridgePort) { server ->
                    when {
                        server.succeeded() -> {
                            this.server = server.result()

                            logger.info("Bridge deployed on: $bridgePort")

                            startFuture?.complete()
                        }
                        else -> startFuture?.fail(server.cause())
                    }
                }
    }
}
