package org.mikand.autonomous.services.gateway

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
class DeploymentVerticle : AbstractVerticle() {
    private val logger : Logger = LoggerFactory.getLogger(DeploymentVerticle::class.java.simpleName)

    override fun start(startFuture: Future<Void>?) {
        logger.info("DeploymentVerticle is running!")

        startFuture?.complete()
    }
}