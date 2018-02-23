package org.mikand.autonomous.services.processors.splitters.impl

import io.vertx.codegen.annotations.Fluent
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import org.mikand.autonomous.services.processors.splitters.concretes.JsonSplitter
import org.mikand.autonomous.services.processors.splitters.splitter.Splitter
import org.mikand.autonomous.services.processors.splitters.splitter.SplitterStatus

class JsonSplitterImpl: JsonSplitter {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private var subscriptionAddress: String? = null
    private var itemsToExtract: List<String>? = null

    fun JsonSplitterImpl(config: JsonObject) {
        subscriptionAddress = config.getString("customSubscriptionAddress") ?: javaClass.name
        itemsToExtract = config.getJsonArray("itemsToExtract").map { it.toString() }
    }

    @Fluent
    override fun split(data: JsonObject): Splitter<JsonObject, JsonObject> {
        splitWithReceipt(data, Handler {
            if (it.failed()) logger.error("Field splitting ${data.encodePrettily()}", it.cause())
        })

        return this
    }

    @Fluent
    override fun splitWithReceipt(data: JsonObject, responseHandler: Handler<AsyncResult<SplitterStatus>>)
            : Splitter<JsonObject, JsonObject> {
        val output = JsonObject()
        val extractables = itemsToExtract ?: ArrayList()

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

            responseHandler.handle(Future.failedFuture(Json.encodePrettily(SplitterStatus(500, "Unparseable"))))
        } finally {
            responseHandler.handle(Future.succeededFuture(SplitterStatus(200, "Processed")))
        }

        return this
    }

    protected fun recurseIntoKey(data: JsonObject, output: JsonObject, extractables: List<String>, it: String) {
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
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>)
            : Splitter<JsonObject, JsonObject> {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}