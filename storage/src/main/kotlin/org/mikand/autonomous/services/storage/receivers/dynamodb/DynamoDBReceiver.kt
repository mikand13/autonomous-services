package org.mikand.autonomous.services.storage.receivers.dynamodb

import com.nannoq.tools.repository.dynamodb.DynamoDBRepository
import com.nannoq.tools.repository.models.Cacheable
import com.nannoq.tools.repository.models.DynamoDBModel
import com.nannoq.tools.repository.models.ETagable
import com.nannoq.tools.repository.models.Model
import com.nannoq.tools.repository.repository.results.ItemListResult
import com.nannoq.tools.repository.utils.GenericItemList
import com.nannoq.tools.repository.utils.ItemList
import com.nannoq.tools.repository.utils.QueryPack
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.serviceproxy.ServiceException
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.CommandEventType.COMMAND_FAILURE
import org.mikand.autonomous.services.core.events.DataEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.storage.receivers.Receiver
import java.util.function.Function

class DynamoDBReceiver<T> : DynamoDBRepository<T>, Receiver
        where T : Model, T : DynamoDBModel, T : Cacheable, T : ETagable {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val type: Class<T>
    private val config: JsonObject
    private val subscriptionAddress: String

    constructor(type: Class<T>, config: JsonObject) : this(Vertx.currentContext().owner(), type, config)

    constructor(vertx: Vertx, type: Class<T>, config: JsonObject) : super(vertx, type, config) {
        this.type = type
        this.config = config
        this.vertx = vertx
        this.subscriptionAddress = config.getString("subscriptionAddress")
                ?: "${Receiver::class.java.name}.${type.simpleName}"
    }

    override fun receiverCreate(receiveInputEvent: CommandEventImpl): Receiver {
        receiverCreateWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun receiverCreateWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        val record: T = toT(receiveInputEvent.body)

        create(record, Handler {
            val result = it.result()

            when {
                it.succeeded() -> {
                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_CREATE")
                            .withMetadata(JsonObject().put("statusCode", 201))
                            .withBody(result.item.toJsonFormat())
                            .build()

                    resultHandler.handle(Future.succeededFuture(outputEvent))

                    vertx.eventBus().publish(subscriptionAddress, outputEvent.toJson())
                }
                else -> {
                    logger.error("Error in receiverCreateWithReceipt for ${type.simpleName}", it.cause())

                    val errorEvent = CommandEventBuilder()
                            .withFailure()
                            .withAction("RECEIVE_CREATE")
                            .withMetadata(JsonObject().put("statusCode", 500))
                            .withBody(JsonObject().put("error", "Unparseable"))
                            .build()

                    resultHandler.handle(ServiceException.fail(500, COMMAND_FAILURE.name, errorEvent.toJson()))
                }
            }
        })

        return this
    }

    override fun receiverUpdate(receiveInputEvent: CommandEventImpl): Receiver {
        receiverUpdateWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun receiverUpdateWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        val record: T = toT(receiveInputEvent.body)

        update(record, Function { r -> r.setModifiables(record) as T }, Handler {
            when {
                it.succeeded() -> {
                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_UPDATE")
                            .withMetadata(JsonObject().put("statusCode", 202))
                            .withBody(it.result().item.toJsonFormat())
                            .build()

                    resultHandler.handle(Future.succeededFuture(outputEvent))

                    vertx.eventBus().publish(subscriptionAddress, outputEvent.toJson())
                }
                else -> {
                    logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                    val errorEvent = CommandEventBuilder()
                            .withFailure()
                            .withAction("RECEIVE_UPDATE")
                            .withMetadata(JsonObject().put("statusCode", 500))
                            .withBody(JsonObject().put("error", "Unparseable"))
                            .build()

                    resultHandler.handle(ServiceException.fail(500, COMMAND_FAILURE.name, errorEvent.toJson()))
                }
            }
        })

        return this
    }

    override fun receiverRead(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        read(receiveInputEvent.body, Handler {
            when {
                it.succeeded() -> {
                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_READ")
                            .withMetadata(JsonObject().put("statusCode", 200))
                            .withBody(it.result().item.toJsonFormat())
                            .build()

                    resultHandler.handle(Future.succeededFuture(outputEvent))
                }
                else -> {
                    logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                    val errorEvent = CommandEventBuilder()
                            .withFailure()
                            .withAction("RECEIVE_READ")
                            .withMetadata(JsonObject().put("statusCode", 500))
                            .withBody(JsonObject().put("error", "Unparseable"))
                            .build()

                    resultHandler.handle(ServiceException.fail(500, COMMAND_FAILURE.name, errorEvent.toJson()))
                }
            }
        })

        return this
    }

    override fun receiverIndex(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        val queryJson: JsonObject? = receiveInputEvent.metadata.getJsonObject("query")
        val queryPack = if (queryJson == null) null else Json.decodeValue(
                queryJson.encode(),
                QueryPack::class.java
        )

        val query: QueryPack = queryPack
            ?: QueryPack.builder()
                .withCustomRoute("receiverIndex-${receiveInputEvent.body.encode().hashCode()}")
                .withProjections(arrayOf())
                .build()

        readAll(receiveInputEvent.body, query, readAllResult(resultHandler))

        return this
    }

    private fun readAllResult(resultHandler: Handler<AsyncResult<DataEventImpl>>): Handler<AsyncResult<ItemListResult<T>>> {
        return Handler { result ->
            when {
                result.succeeded() -> {
                    val items: ItemList<T> = result.result().itemList!!
                    val generic = GenericItemList(items.paging!!, items.meta?.count!!, items.items?.map { it.toJsonFormat() })

                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_INDEX")
                            .withMetadata(JsonObject().put("statusCode", 200))
                            .withBody(generic.toJson())
                            .build()

                    resultHandler.handle(Future.succeededFuture(outputEvent))
                }
                else -> {
                    logger.error("Error in receiverIndexWithQuery for ${type.simpleName}", result.cause())

                    val errorEvent = CommandEventBuilder()
                            .withFailure()
                            .withAction("RECEIVE_INDEX")
                            .withMetadata(JsonObject().put("statusCode", 500))
                            .withBody(JsonObject().put("error", "Unparseable"))
                            .build()

                    resultHandler.handle(ServiceException.fail(500, COMMAND_FAILURE.name, errorEvent.toJson()))
                }
            }
        }
    }

    override fun receiverDelete(receiveInputEvent: CommandEventImpl): Receiver {
        receiverDeleteWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun receiverDeleteWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        val record: T = toT(receiveInputEvent.body)
        val id = JsonObject()
                .put("hash", record.hash)
                .put("range", record.range)

        delete(id, Handler {
            val result = it.result()

            when {
                it.succeeded() -> {
                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_DELETE")
                            .withMetadata(JsonObject().put("statusCode", 204))
                            .withBody(result.item.toJsonFormat())
                            .build()

                    resultHandler.handle(Future.succeededFuture(outputEvent))

                    vertx.eventBus().publish(subscriptionAddress, outputEvent.toJson())
                }
                else -> {
                    logger.error("Error in receiverDeleteWithReceipt for ${type.simpleName}", it.cause())

                    val errorEvent = CommandEventBuilder()
                            .withFailure()
                            .withAction("RECEIVE_DELETE")
                            .withMetadata(JsonObject().put("statusCode", 500))
                            .withBody(JsonObject().put("error", "Unparseable"))
                            .build()

                    resultHandler.handle(ServiceException.fail(500, COMMAND_FAILURE.name, errorEvent.toJson()))
                }
            }
        })

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): Receiver {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("ADDRESS")
                .withMetadata(JsonObject().put("statusCode", 200))
                .withBody(JsonObject().put("address", subscriptionAddress))
                .build()

        addressHandler.handle(Future.succeededFuture(outputEvent))

        return this
    }

    private fun toT(json: JsonObject): T {
        return Json.decodeValue(json.encode(), type)
    }
}