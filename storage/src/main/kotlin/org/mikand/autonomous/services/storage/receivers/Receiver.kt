package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.repository.utils.GenericItemList
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

@VertxGen
@ProxyGen
interface Receiver {
    @Fluent
    fun receiverCreate(json: JsonObject): Receiver

    @Fluent
    fun receiverCreateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver

    @Fluent
    fun receiverUpdate(json: JsonObject): Receiver

    @Fluent
    fun receiverUpdateWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver

    @Fluent
    fun receiverRead(json: JsonObject, resultHandler: Handler<AsyncResult<JsonObject>>): Receiver

    @Fluent
    fun receiverIndex(json: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver

    @Fluent
    fun receiverIndexWithQuery(json: JsonObject, queryPack: JsonObject, resultHandler: Handler<AsyncResult<GenericItemList>>): Receiver

    @Fluent
    fun receiverDelete(json: JsonObject): Receiver

    @Fluent
    fun receiverDeleteWithReceipt(json: JsonObject, resultHandler: Handler<AsyncResult<ReceiveStatus>>): Receiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): Receiver
}