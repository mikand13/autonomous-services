package org.mikand.autonomous.services.gateway.bridge

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import java.util.ArrayList
import io.vertx.core.json.Json
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.bridge.BridgeEventType.*
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions

class BridgeVerticle() : AbstractVerticle() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val DEFAULT_BRIDGE_BASE = "org.mikand.autonomous.services.gateway.bridge"
    private val DEFAULT_BRIDGE_PATH = "/eventbus/*"
    private val DEFAULT_BRIDGE_PORT = 443
    private val DEFAULT_BRIDGE_BASE_API = ".api"
    private val DEFAULT_BRIDGE_BASE_DATA = ".data"
    private val DEFAULT_BRIDGE_BASE_STORAGE = ".storage"

    private var bridgeHandlerOptions: SockJSHandlerOptions? = null
    private var router: Router? = null
    private var httpServerOptions: HttpServerOptions? = null

    private var server: HttpServer? = null

    constructor(bridgeHandlerOptions: SockJSHandlerOptions) : this(null, bridgeHandlerOptions, null)
    constructor(router: Router) : this(router, null, null)
    constructor(httpServerOptions: HttpServerOptions) : this(null, null, httpServerOptions)

    constructor(router: Router?,
                bridgeHandlerOptions: SockJSHandlerOptions?,
                httpServerOptions: HttpServerOptions?) : this() {
        this.router = router
        this.bridgeHandlerOptions = bridgeHandlerOptions
        this.httpServerOptions = httpServerOptions
    }

    override fun start(startFuture: Future<Void>?) {
        val bridgeBase = config().getString("bridgeBase") ?: DEFAULT_BRIDGE_BASE
        val bridgePath = config().getString("bridgePath") ?: DEFAULT_BRIDGE_PATH
        val bridgePort = config().getInteger("bridgePort") ?: DEFAULT_BRIDGE_PORT

        startBridge(bridgeBase, bridgePath, bridgePort, startFuture)
    }

    private fun startBridge(bridgeBase: String, bridgePath: String, bridgePort: Int, startFuture: Future<Void>?) {
        val options = bridgeHandlerOptions ?: SockJSHandlerOptions()
        val router = router ?: Router.router(vertx)
        val ebHandler = SockJSHandler.create(vertx, options)

        addEventBusBridge(ebHandler, router, bridgeBase, bridgePath)
        deployBridge(router, bridgePort, startFuture)
    }

    private fun addEventBusBridge(ebHandler: SockJSHandler, router: Router,
                                  bridgeBase: String, bridgePath: String) {
        ebHandler.bridge(createBridgeOptions(bridgeBase), { bridgeEvent ->
            logger.debug("Event received from external client!")

            if (bridgeEvent.type() != null) {
                when (bridgeEvent.type()) {
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
            } else {
                logger.error("Type is null!, Message is: " + Json.encodePrettily(bridgeEvent))
            }
        })

        router.route(bridgePath).handler(CookieHandler.create())
        router.route(bridgePath).handler(ebHandler)
    }

    private fun deployBridge(router: Router, bridgePort: Int, startFuture: Future<Void>?) {
        val options = httpServerOptions ?: HttpServerOptions()
                .setCompressionSupported(true)
                .setTcpKeepAlive(true)

        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(bridgePort, { server ->
                    if (server.succeeded()) {
                        this.server = server.result()

                        startFuture?.complete()
                    } else {
                        startFuture?.fail(server.cause())
                    }
                })
    }

    private fun authorize(bridgeEvent: BridgeEvent) {
        val rawMessage = bridgeEvent.rawMessage
        val address = rawMessage.getString("address")

        if (address != null) {
            bridgeEvent.complete(java.lang.Boolean.TRUE)
        } else {
            bridgeEvent.fail("Unknown address!")
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

        return permissions
    }

    private fun createOutAdresses(bridgeBase: String): List<PermittedOptions> {
        val permissions = ArrayList<PermittedOptions>()
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_API).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_DATA).*$"))
        permissions.add(PermittedOptions().setAddressRegex("^($bridgeBase$DEFAULT_BRIDGE_BASE_STORAGE).*$"))

        return permissions
    }
}