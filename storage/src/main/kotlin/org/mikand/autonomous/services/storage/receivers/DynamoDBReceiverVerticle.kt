package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.repository.models.Cacheable
import com.nannoq.tools.repository.models.DynamoDBModel
import com.nannoq.tools.repository.models.ETagable
import com.nannoq.tools.repository.models.Model
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

open class DynamoDBReceiverVerticle<T> : AbstractVerticle, Receiver
        where T : Model, T : DynamoDBModel, T : Cacheable, T : ETagable {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val type: Class<T>
    private val config: JsonObject
    private val dynamoDbReceiver: DynamoDBReceiver<T>

    constructor(vertx: Vertx, type: Class<T>, config: JsonObject) : super() {
        this.vertx = vertx
        this.type = type
        this.config = config
        this.dynamoDbReceiver = DynamoDBReceiver(vertx, type, config)
    }

    constructor(type: Class<T>, config: JsonObject) : super() {
        this.type = type
        this.config = config
        this.dynamoDbReceiver = DynamoDBReceiver(vertx, type, config)
    }

    override fun receiverCreate(receiveEvent: ReceiveEvent): Receiver {
        dynamoDbReceiver.receiverCreate(receiveEvent)

        return dynamoDbReceiver
    }

    override fun receiverCreateWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.receiverCreateWithReceipt(receiveEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverUpdate(receiveEvent: ReceiveEvent): Receiver {
        dynamoDbReceiver.receiverUpdate(receiveEvent)

        return dynamoDbReceiver
    }

    override fun receiverUpdateWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.receiverUpdateWithReceipt(receiveEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverRead(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.receiverRead(receiveEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverIndex(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.receiverIndex(receiveEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverDelete(receiveEvent: ReceiveEvent): Receiver {
        dynamoDbReceiver.receiverDelete(receiveEvent)

        return dynamoDbReceiver
    }

    override fun receiverDeleteWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.receiverDeleteWithReceipt(receiveEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver {
        dynamoDbReceiver.fetchSubscriptionAddress(addressHandler)

        return dynamoDbReceiver
    }
}