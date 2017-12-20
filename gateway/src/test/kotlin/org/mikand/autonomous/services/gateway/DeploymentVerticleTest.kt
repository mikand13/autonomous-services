package org.mikand.autonomous.services.gateway

import org.mikand.autonomous.services.gateway.utils.ConfigSupport
import io.vertx.core.DeploymentOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.runner.RunWith

import org.junit.Rule
import org.junit.Test

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
@RunWith(VertxUnitRunner::class)
class DeploymentVerticleTest : ConfigSupport {
    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @Test
    fun shouldDeployDeploymentVerticleWithSuccess(context : TestContext) {
        val deploymentOptions = DeploymentOptions().setConfig(getTestConfig())
        rule.vertx().deployVerticle(DeploymentVerticle(), deploymentOptions, context.asyncAssertSuccess())
    }
}
