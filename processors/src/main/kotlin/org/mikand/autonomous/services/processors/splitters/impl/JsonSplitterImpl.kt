package org.mikand.autonomous.services.processors.splitters.impl

import io.vertx.codegen.annotations.Fluent
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import org.mikand.autonomous.services.processors.splitters.concretes.JsonSplitter
import org.mikand.autonomous.services.processors.splitters.splitter.SplitStatus

open class JsonSplitterImpl(config: JsonObject = JsonObject()) : AbstractVerticle(), JsonSplitter {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val subscriptionAddress: String = config.getString("customSubscriptionAddress") ?: javaClass.name
    private val extractables: List<String> = config.getJsonArray("extractables")?.map { it.toString() } ?: ArrayList()
    private val thisVertx: Vertx = vertx ?: Vertx.currentContext().owner()

    @Fluent
    override fun split(data: JsonObject): JsonSplitter {
        splitWithReceipt(data, Handler {
            if (it.failed()) logger.error("Field splitting ${data.encodePrettily()}", it.cause())
        })

        return this
    }

    @Fluent
    override fun splitWithReceipt(data: JsonObject, responseHandler: Handler<AsyncResult<SplitStatus>>): JsonSplitter {
        val output = JsonObject()

        try {
            extractables.forEach {
                if (it.contains("\\.")) {
                    recurseIntoKey(data, output, extractables, it)
                } else {
                    output.put(it, data.getValue(it))
                }
            }
        } catch (ise: IllegalStateException) {
            logger.error("Field splitting ${data.encodePrettily()}", ise)

            responseHandler.handle(Future.failedFuture(Json.encodePrettily(SplitStatus(500, "Unparseable"))))
        } finally {
            responseHandler.handle(Future.succeededFuture(SplitStatus(200, "Processed")))

            thisVertx.eventBus().publish(subscriptionAddress, output)
        }

        return this
    }

    private fun recurseIntoKey(data: JsonObject, output: JsonObject, extractables: List<String>, it: String) {
        val keyMap = it.split(".")

        if (keyMap.isEmpty()) throw IllegalStateException("$keyMap should not be empty!")

        if (keyMap.size == 2) {
            if (extractables.contains(it)) {
                if (!output.containsKey(keyMap[0])) {
                    output.put(keyMap[0], JsonObject())
                }

                val keyObject = data.getJsonObject(keyMap[0])

                if (keyObject.containsKey(keyMap[1])) {
                    val value = keyObject.getValue(keyMap[1])

                    output.getJsonObject(keyMap[0]).put(keyMap[1], value)
                }
            }
        } else {
            val modifiedIt = keyMap.drop(1).toTypedArray().joinToString { "." }
            val newExtractables = arrayListOf(modifiedIt)

            recurseIntoKey(data.getJsonObject(keyMap[0]), output, newExtractables, modifiedIt)
        }
    }

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): JsonSplitter {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}