package org.mikand.autonomous.services.processors.splitters.impl

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.codegen.annotations.Fluent
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.processors.splitters.concretes.JsonSplitter

open class JsonSplitterImpl(config: JsonObject = JsonObject()) : AbstractVerticle(), JsonSplitter {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val publishAddress: String = config.getString("customPublishAddress") ?: JsonSplitter::class.java.simpleName
    private val deployService: Boolean = config.getBoolean("deployAsService") ?: true
    private val subscriptionAddress: String = config.getString("customSubscriptionAddress") ?: javaClass.name
    private val extractables: List<String> = config.getJsonArray("extractables")?.map { it.toString() } ?: ArrayList()
    private val thisVertx: Vertx = vertx ?: Vertx.currentContext().owner()

    private lateinit var service: Record

    override fun start(startFuture: Future<Void>?) {
        if (deployService) {
            ServiceManager.getInstance().publishService(JsonSplitter::class.java, publishAddress, this) {
                if (it.succeeded()) {
                    service = it.result()

                    startFuture?.complete()
                } else {
                    startFuture?.fail(it.cause())
                }
            }
        } else {
            startFuture?.complete()
        }
    }

    override fun stop(stopFuture: Future<Void>?) {
        if (deployService) {
            try {
                ServiceManager.getInstance().unPublishService(JsonSplitter::class.java, service) {
                    stopFuture?.complete()
                }
            } catch (error: Exception) {
                stopFuture?.complete()
            }
        } else {
            stopFuture?.complete()
        }
    }

    @Fluent
    override fun split(splitInputEvent: CommandEventImpl): JsonSplitter {
        splitWithReceipt(splitInputEvent, Handler {
            if (it.failed()) logger.error("Field splitting ${splitInputEvent.body.encodePrettily()}", it.cause())
        })

        return this
    }

    @Fluent
    override fun splitWithReceipt(splitInputEvent: CommandEventImpl, responseHandler: Handler<AsyncResult<DataEventImpl>>): JsonSplitter {
        val output = JsonObject()

        try {
            extractables.forEach {
                if (it.contains('.')) {
                    recurseIntoKey(splitInputEvent.body, output, extractables, it)
                } else {
                    output.put(it, splitInputEvent.body.getValue(it))
                }
            }
        } catch (ise: IllegalStateException) {
            logger.error("Field splitting ${splitInputEvent.body.encodePrettily()}", ise)

            val errorEvent = CommandEventBuilder()
                    .withFailure()
                    .withAction("SPLIT_FAILURE")
                    .withMetadata(JsonObject().put("statusCode", 500))
                    .withBody(JsonObject().put("error", "Unparseable"))
                    .build()

            responseHandler.handle(Future.failedFuture(Json.encodePrettily(errorEvent)))
        } finally {
            val outputEvent = DataEventBuilder()
                    .withSuccess()
                    .withAction("SPLIT")
                    .withMetadata(JsonObject().put("statusCode", 200))
                    .withBody(output)
                    .build()

            responseHandler.handle(Future.succeededFuture(outputEvent))

            thisVertx.eventBus().publish(subscriptionAddress, outputEvent.toJson())
        }

        return this
    }

    private fun recurseIntoKey(data: JsonObject, output: JsonObject, extractables: List<String>, it: String) {
        val keyMap = it.split('.')

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
            if (!output.containsKey(keyMap[0])) {
                output.put(keyMap[0], JsonObject())
            }

            val subObject = data.getJsonObject(keyMap[0])
            val modifiedIt = keyMap.drop(1).joinToString(".")
            val newExtractables = arrayListOf(modifiedIt)

            recurseIntoKey(subObject, output.getJsonObject(keyMap[0]), newExtractables, modifiedIt)
        }
    }

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): JsonSplitter {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("ADDRESS")
                .withMetadata(JsonObject().put("statusCode", 200))
                .withBody(JsonObject().put("address", subscriptionAddress))
                .build()

        addressHandler.handle(Future.succeededFuture(outputEvent))

        return this
    }
}