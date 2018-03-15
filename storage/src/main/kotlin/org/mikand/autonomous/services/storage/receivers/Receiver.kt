package org.mikand.autonomous.services.storage.receivers

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@VertxGen
@ProxyGen
interface Receiver {
    @Fluent
    fun receiverCreate(receiveEvent: ReceiveEvent): Receiver

    @Fluent
    fun receiverCreateWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverUpdate(receiveEvent: ReceiveEvent): Receiver

    @Fluent
    fun receiverUpdateWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverRead(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverIndex(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverDelete(receiveEvent: ReceiveEvent): Receiver

    @Fluent
    fun receiverDeleteWithReceipt(receiveEvent: ReceiveEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver
}