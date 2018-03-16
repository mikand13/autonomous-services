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
    fun receiverCreate(receiveInputEvent: ReceiveInputEvent): Receiver

    @Fluent
    fun receiverCreateWithReceipt(receiveInputEvent: ReceiveInputEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverUpdate(receiveInputEvent: ReceiveInputEvent): Receiver

    @Fluent
    fun receiverUpdateWithReceipt(receiveInputEvent: ReceiveInputEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverRead(receiveInputEvent: ReceiveInputEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverIndex(receiveInputEvent: ReceiveInputEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun receiverDelete(receiveInputEvent: ReceiveInputEvent): Receiver

    @Fluent
    fun receiverDeleteWithReceipt(receiveInputEvent: ReceiveInputEvent, resultHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): Receiver
}