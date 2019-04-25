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
import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import org.mikand.autonomous.services.gateway.bridge.BridgeVerticle.Companion.DEFAULT_BRIDGE_PORT

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
class GatewayDeploymentVerticle : AbstractVerticle() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var heartbeatServiceImpl: GatewayHeartbeatServiceImpl
    private lateinit var heartBeatRecord: Record
    private var periodicTimerId: Long = 0

    private var coreCount = Runtime.getRuntime().availableProcessors()
    private val deploymentOptions = DeploymentOptions()
            .setInstances(coreCount * 2)

    companion object {
        const val GATEWAY_HEARTBEAT_ADDRESS: String = "GatewayHeartbeat"
        private const val BRIDGE_VERTICLE = "org.mikand.autonomous.services.gateway.bridge.BridgeVerticle"
    }

    override fun start(startFuture: Future<Void>?) {
        val bridgeFuture = Future.future<String>()
        val heartBeatFuture = Future.future<Record>()
        val gatewayConfig = config().getJsonObject("gateway") ?: config()
        val depOptions = deploymentOptions.setConfig(gatewayConfig)

        logger.info("Launching gateway with config: " + gatewayConfig.encodePrettily())

        deployBridgeAndHealthCheck(BRIDGE_VERTICLE, depOptions, bridgeFuture, heartBeatFuture)

        CompositeFuture.all(bridgeFuture, heartBeatFuture).setHandler {
            logger.info("GatewayDeploymentVerticle has deployed: ${it.succeeded()}")

            when {
                it.succeeded() -> {
                    val port = gatewayConfig.getInteger("bridgePort") ?: DEFAULT_BRIDGE_PORT

                    logger.info("GatewayDeploymentVerticle is running on port: $port!")

                    startFuture?.complete()

                    periodicTimerId = deployPeriodicCheck()
                }
                else -> startFuture?.fail(it.cause())
            }
        }
    }

    @Suppress("SpellCheckingInspection", "SameParameterValue")
    private fun deployBridgeAndHealthCheck(
        bridgeVerticle: String,
        deploymentOptions: DeploymentOptions?,
        bridgeFuture: Future<String>,
        heartBeatFuture: Future<Record>
    ) {
        vertx.deployVerticle(bridgeVerticle, deploymentOptions) { deploymentID ->
            when {
                deploymentID.succeeded() -> {
                    val result = deploymentID.result()
                    logger.info("Deployed bridge: $result")

                    deployHeartbeat(heartBeatFuture)

                    bridgeFuture.complete(result)
                }
                else -> bridgeFuture.fail(deploymentID.cause())
            }
        }
    }

    private fun deployHeartbeat(heartBeatFuture: Future<Record>) {
        val gatewayConfig = config().getJsonObject("gateway") ?: config()
        
        heartbeatServiceImpl = GatewayHeartbeatServiceImpl(vertx, gatewayConfig)

        ServiceManager.getInstance().publishService(HeartbeatService::class.java, GATEWAY_HEARTBEAT_ADDRESS,
                heartbeatServiceImpl, Handler {
            when {
                it.succeeded() -> {
                    logger.info("GatewayHeartbeat is live!")

                    heartBeatRecord = it.result()

                    heartBeatFuture.complete(heartBeatRecord)
                }
                else -> {
                    logger.error("GatewayHeartbeat deployment failed...", it.cause())

                    heartBeatFuture.fail(it.cause())
                }
            }
        })
    }

    private fun deployPeriodicCheck(): Long {
        return vertx.setPeriodic(10000L) {
            ServiceManager.getInstance().consumeService(HeartbeatService::class.java, GATEWAY_HEARTBEAT_ADDRESS, Handler {
                when {
                    it.failed() -> heartbeatDown()
                    else -> heartbeatAvailable(it)
                }
            })
        }
    }

    private fun heartbeatDown() {
        logger.error("GatewayHeartbeat is unavailable, relaunching!")

        killHeartBeat()

        val heartbeatFuture = Future.future<Record>()
        heartbeatFuture.setHandler {
            when {
                it.succeeded() -> logger.info("GatewayHeartbeat backup is live!")
                else -> logger.error("GatewayHeartbeat backup failed...", it.cause())
            }
        }

        deployHeartbeat(heartbeatFuture)
    }

    private fun heartbeatAvailable(it: AsyncResult<HeartbeatService>) {
        when {
            it.failed() -> {
                logger.error("GatewayHeartbeat reports bridge down, redeploying!", it.cause())

                killHeartBeat()

                val bridgeFuture = Future.future<String>()
                val heartBeatFuture = Future.future<Record>()

                deployBridgeAndHealthCheck(BRIDGE_VERTICLE, deploymentOptions, bridgeFuture, heartBeatFuture)

                CompositeFuture.all(bridgeFuture, heartBeatFuture).setHandler {
                    logger.info("GatewayDeploymentVerticle has deployed: ${it.succeeded()}")

                    if (it.succeeded()) {
                        logger.info("Bridge redeployed!")
                    } else {
                        logger.error("Unable to deploy bridge, killing application!")

                        vertx.close()
                    }
                }
            }
            else -> logger.trace("Health Check OK")
        }
    }

    private fun killHeartBeat() {
        ServiceManager.getInstance().unPublishService(GATEWAY_HEARTBEAT_ADDRESS, heartBeatRecord)
    }
}
