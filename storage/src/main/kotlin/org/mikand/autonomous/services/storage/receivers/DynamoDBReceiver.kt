package org.mikand.autonomous.services.storage.receivers

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
        this.subscriptionAddress = "${Receiver::class.java.name}.${type.simpleName}"
    }

    override fun receiverCreate(receiveInputEvent: ReceiveInputEvent): Receiver {
        receiverCreateWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun receiverCreateWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                           resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        val record: T = toT(receiveInputEvent.body)

        create(record, {
            val result = it.result()

            if (it.succeeded()) {
                val status = ReceiveEvent(ReceiveEventType.DATA.name, "RECEIVE_CREATE",
                        ReceiveStatus(201, statusObject = result.item.toJsonFormat()))

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, JsonObject(Json.encode(status)))
            } else {
                logger.error("Error in receiverCreateWithReceipt for ${type.simpleName}", it.cause())

                val receiveResult = ReceiveEvent(ReceiveEventType.COMMAND_FAILURE.name, "RECEIVE_CREATE",
                        ReceiveStatus(500, "Unparseable"))

                resultHandler.handle(ServiceException.fail(
                        500, ReceiveEventType.COMMAND_FAILURE.name, receiveResult.toJson()))
            }
        })

        return this
    }

    override fun receiverUpdate(receiveInputEvent: ReceiveInputEvent): Receiver {
        receiverUpdateWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun receiverUpdateWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                           resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        val record: T = toT(receiveInputEvent.body)

        update(record, { r -> r.setModifiables(record) as T }) {
            if (it.succeeded()) {
                val status = ReceiveEvent(ReceiveEventType.DATA.name, "RECEIVE_UPDATE",
                        ReceiveStatus(202, statusObject = it.result().item.toJsonFormat()))

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, JsonObject(Json.encode(status)))
            } else {
                logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                val receiveResult = ReceiveEvent(ReceiveEventType.COMMAND_FAILURE.name, "RECEIVE_UPDATE",
                        ReceiveStatus(500, "Unparseable"))

                resultHandler.handle(ServiceException.fail(
                        500, ReceiveEventType.COMMAND_FAILURE.name, receiveResult.toJson()))
            }
        }

        return this
    }

    override fun receiverRead(receiveInputEvent: ReceiveInputEvent,
                              resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        read(receiveInputEvent.body) {
            if (it.succeeded()) {
                val status = ReceiveEvent(ReceiveEventType.DATA.name, "RECEIVE_READ",
                        ReceiveStatus(200, statusObject = it.result().item.toJsonFormat()))

                resultHandler.handle(Future.succeededFuture(status))
            } else {
                logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                val receiveResult = ReceiveEvent(ReceiveEventType.COMMAND_FAILURE.name, "RECEIVE_READ",
                        ReceiveStatus(500, "Unparseable"))

                resultHandler.handle(ServiceException.fail(
                        500, ReceiveEventType.COMMAND_FAILURE.name, receiveResult.toJson()))
            }
        }

        return this
    }

    override fun receiverIndex(receiveInputEvent: ReceiveInputEvent,
                               resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        val queryJson: JsonObject? = receiveInputEvent.metaData.getJsonObject("query")
        val queryPack = if (queryJson == null) null else Json.decodeValue(
                queryJson.encode(),
                QueryPack::class.java
        )

        val query: QueryPack = queryPack ?:
            QueryPack.builder()
                .withCustomRoute("receiverIndex-${receiveInputEvent.body.encode().hashCode()}")
                .withProjections(arrayOf())
                .build()

        readAll(receiveInputEvent.body, query, readAllResult(resultHandler))

        return this
    }

    fun readAllResult(resultHandler: Handler<AsyncResult<ReceiveEvent>>) : Handler<AsyncResult<ItemListResult<T>>> {
        return Handler {
            if (it.succeeded()) {
                val items: ItemList<T> = it.result().itemList
                val generic = GenericItemList(items.pageToken, items.count, items.items.map { it.toJsonFormat() })
                val status = ReceiveEvent(ReceiveEventType.DATA.name, "RECEIVE_INDEX",
                        ReceiveStatus(200, statusObject = generic.toJson()))

                resultHandler.handle(Future.succeededFuture(status))
            } else {
                logger.error("Error in receiverIndexWithQuery for ${type.simpleName}", it.cause())

                val receiveResult = ReceiveEvent(ReceiveEventType.COMMAND_FAILURE.name, "RECEIVE_INDEX",
                        ReceiveStatus(500, "Unparseable"))

                resultHandler.handle(ServiceException.fail(
                        500, ReceiveEventType.COMMAND_FAILURE.name, receiveResult.toJson()))
            }
        }
    }

    override fun receiverDelete(receiveInputEvent: ReceiveInputEvent): Receiver {
        receiverDeleteWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun receiverDeleteWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                           resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        val record: T = toT(receiveInputEvent.body)
        val id = JsonObject()
                .put("hash", record.hash)
                .put("range", record.range)

        delete(id, {
            val result = it.result()

            if (it.succeeded()) {
                val status = ReceiveEvent(ReceiveEventType.DATA.name, "RECEIVE_DELETE",
                        ReceiveStatus(204, statusObject = result.item.toJsonFormat()))

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, JsonObject(Json.encode(status)))
            } else {
                logger.error("Error in receiverDeleteWithReceipt for ${type.simpleName}", it.cause())

                val receiveResult = ReceiveEvent(ReceiveEventType.COMMAND_FAILURE.name, "RECEIVE_DELETE",
                        ReceiveStatus(500, "Unparseable"))

                resultHandler.handle(ServiceException.fail(
                        500, ReceiveEventType.COMMAND_FAILURE.name, receiveResult.toJson()))
            }
        })

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        addressHandler.handle(Future.succeededFuture(ReceiveEvent(ReceiveEventType.DATA.name, "ADDRESS",
                ReceiveStatus(200, statusObject = JsonObject().put("address", subscriptionAddress)))))

        return this
    }

    private fun toT(json: JsonObject): T {
        return Json.decodeValue(json.encode(), type)
    }
}