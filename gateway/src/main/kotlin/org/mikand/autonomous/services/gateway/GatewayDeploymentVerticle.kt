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
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
class GatewayDeploymentVerticle : AbstractVerticle() {
    private val logger : Logger = LoggerFactory.getLogger(GatewayDeploymentVerticle::class.java.simpleName)

    private lateinit var heartbeatServiceImpl: GatewayHeartbeatService
    private lateinit var heartBeatRecord: Record

    override fun start(startFuture: Future<Void>?) {
        logger.info("GatewayDeploymentVerticle is running!")

        val bridgeVerticle = "org.mikand.autonomous.services.gateway.bridge.BridgeVerticle"
        val coreCount = Runtime.getRuntime().availableProcessors()
        val deploymentOptions = DeploymentOptions()
                .setInstances(coreCount * 2)

        val bridgeFuture = Future.future<Void>()
        val heartBeatFuture = Future.future<Record>()

        vertx.deployVerticle(bridgeVerticle, deploymentOptions, { deploymentID ->
            if (deploymentID.succeeded()) {
                val result = deploymentID.result()
                logger.info("Deployed bridge: $result")

                heartbeatServiceImpl = GatewayHeartbeatServiceImpl(vertx, result)

                ServiceManager.getInstance()
                        .publishService(GatewayHeartbeatService::class.java, heartbeatServiceImpl, {
                            if (it.succeeded()) {
                                heartBeatRecord = it.result()

                                heartBeatFuture.complete()
                            } else {
                                heartBeatFuture.fail(it.cause())
                            }
                        })

                bridgeFuture.complete()
            } else {
                bridgeFuture.fail(deploymentID.cause())
            }
        })

        CompositeFuture.all(bridgeFuture, heartBeatFuture).setHandler({
            logger.info("GatewayDeploymentVerticle has deployed: ${it.succeeded()}")

            if (it.succeeded()) {
                startFuture?.complete()
            } else {
                startFuture?.fail(it.cause())
            }
        })
    }

    override fun stop(stopFuture: Future<Void>?) {
        if (stopFuture == null) return

        ServiceManager.getInstance()
                .unPublishService(GatewayHeartbeatService::class.java, heartBeatRecord, stopFuture.completer())
    }
}