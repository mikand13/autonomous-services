package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.repository.models.Cacheable
import com.nannoq.tools.repository.models.DynamoDBModel
import com.nannoq.tools.repository.models.ETagable
import com.nannoq.tools.repository.models.Model
import com.nannoq.tools.repository.utils.GenericItemList
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

    override fun receiverCreate(json: JsonObject): Receiver {
        dynamoDbReceiver.receiverCreate(json)

        return dynamoDbReceiver
    }

    override fun receiverCreateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        dynamoDbReceiver.receiverCreateWithReceipt(json, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverUpdate(json: JsonObject): Receiver {
        dynamoDbReceiver.receiverUpdate(json)

        return dynamoDbReceiver
    }

    override fun receiverUpdateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        dynamoDbReceiver.receiverUpdateWithReceipt(json, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverRead(json: JsonObject, resultHandler: Handler<AsyncResult<JsonObject>>): Receiver {
        dynamoDbReceiver.receiverRead(json, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverIndex(json: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver {
        dynamoDbReceiver.receiverIndex(json, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverIndexWithQuery(json: JsonObject, queryPack: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver {
        dynamoDbReceiver.receiverIndexWithQuery(json, queryPack, resultHandler)

        return dynamoDbReceiver
    }

    override fun receiverDelete(json: JsonObject): Receiver {
        dynamoDbReceiver.receiverDelete(json)

        return dynamoDbReceiver
    }

    override fun receiverDeleteWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver {
        dynamoDbReceiver.receiverDeleteWithReceipt(json, resultHandler)

        return dynamoDbReceiver
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): Receiver {
        dynamoDbReceiver.fetchSubscriptionAddress(addressHandler)

        return dynamoDbReceiver
    }
}