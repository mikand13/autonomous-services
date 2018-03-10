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
        this.subscriptionAddress = "${Receiver::class.java.name}.${type::class.java.simpleName}"
    }

    override fun receiverCreate(json: JsonObject): Receiver {
        receiverCreateWithReceipt(json, Handler {})

        return this
    }

    override fun receiverCreateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        val record: T = toT(json)

        create(record, {
            val result = it.result()

            if (it.succeeded()) {
                val status = ReceiveStatus(201, statusObject = result.item.toJsonFormat())

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, Json.encodePrettily(status))
            } else {
                logger.error("Error in receiverCreateWithReceipt for ${type.simpleName}", it.cause())

                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        })

        return this
    }

    override fun receiverUpdate(json: JsonObject): Receiver {
        receiverUpdateWithReceipt(json, Handler {})

        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun receiverUpdateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        val record: T = toT(json)

        update(record, { r -> r.setModifiables(record) as T }) {
            if (it.succeeded()) {
                val status = ReceiveStatus(202, statusObject = it.result().item.toJsonFormat())

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, Json.encodePrettily(status))
            } else {
                logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        }

        return this
    }

    override fun receiverRead(json: JsonObject, resultHandler: Handler<AsyncResult<JsonObject>>): Receiver {
        read(json) {
            if (it.succeeded()) {
                resultHandler.handle(Future.succeededFuture(it.result().item.toJsonFormat()))
            } else {
                logger.error("Error in receiverRead for ${type.simpleName}", it.cause())

                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        }

        return this
    }

    override fun receiverIndex(json: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver {
        readAll(json, QueryPack.builder().withProjections(arrayOf()).build(), readAllResult(resultHandler))

        return this
    }

    override fun receiverIndexWithQuery(json: JsonObject, queryPack: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver {
        val query: QueryPack = Json.decodeValue(json.encode(), QueryPack::class.java)

        readAll(json, query, readAllResult(resultHandler))

        return this
    }

    fun readAllResult(resultHandler: Handler<AsyncResult<GenericItemList>>) : Handler<AsyncResult<ItemListResult<T>>> {
        return Handler {
            if (it.succeeded()) {
                val items: ItemList<T> = it.result().itemList
                val generic = GenericItemList(items.pageToken, items.count, items.items.map { it.toJsonFormat() })

                resultHandler.handle(Future.succeededFuture(generic))
            } else {
                logger.error("Error in receiverIndexWithQuery for ${type.simpleName}", it.cause())

                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        }
    }

    override fun receiverDelete(json: JsonObject): Receiver {
        receiverDeleteWithReceipt(json, Handler {})

        return this
    }

    override fun receiverDeleteWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        val record: T = toT(json)
        val id = JsonObject()
                .put("hash", record.hash)
                .put("range", record.range)

        delete(id, {
            val result = it.result()

            if (it.succeeded()) {
                val status = ReceiveStatus(204, statusObject = result.item.toJsonFormat())

                resultHandler.handle(Future.succeededFuture(status))

                vertx.eventBus().publish(subscriptionAddress, Json.encodePrettily(status))
            } else {
                logger.error("Error in receiverDeleteWithReceipt for ${type.simpleName}", it.cause())

                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        })

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): Receiver {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }

    private fun toT(json: JsonObject): T {
        return Json.decodeValue(json.encode(), type)
    }
}