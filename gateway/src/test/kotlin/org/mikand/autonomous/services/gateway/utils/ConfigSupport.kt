package org.mikand.autonomous.services.gateway.utils

import io.vertx.core.json.JsonObject
import java.io.File

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
interface ConfigSupport {
    fun getTestConfig() : JsonObject {
        val configFile = File(this::class.java.classLoader.getResource("app-conf.json").toURI())

        return JsonObject(configFile.readText())
    }
}