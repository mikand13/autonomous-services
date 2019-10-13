package org.mikand.autonomous.services.storage.receivers.dynamodb

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
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.storage.receivers.Receiver

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

    override fun receiverCreate(receiveInputEvent: CommandEventImpl): Receiver {
        dynamoDbReceiver.receiverCreate(receiveInputEvent)

        return dynamoDbReceiver
    }

    override fun receiverCreateWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        dynamoDbReceiver.receiverCreateWithReceipt(receiveInputEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverUpdate(receiveInputEvent: CommandEventImpl): Receiver {
        dynamoDbReceiver.receiverUpdate(receiveInputEvent)

        return dynamoDbReceiver
    }

    override fun receiverUpdateWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        dynamoDbReceiver.receiverUpdateWithReceipt(receiveInputEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverRead(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        dynamoDbReceiver.receiverRead(receiveInputEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverIndex(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        dynamoDbReceiver.receiverIndex(receiveInputEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverDelete(receiveInputEvent: CommandEventImpl): Receiver {
        dynamoDbReceiver.receiverDelete(receiveInputEvent)

        return dynamoDbReceiver
    }

    override fun receiverDeleteWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): Receiver {
        dynamoDbReceiver.receiverDeleteWithReceipt(receiveInputEvent, resultHandler)

        return dynamoDbReceiver
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): Receiver {
        dynamoDbReceiver.fetchSubscriptionAddress(addressHandler)

        return dynamoDbReceiver
    }
}
