package org.mikand.autonomous.services.storage.receivers

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventImpl

@VertxGen
@ProxyGen
interface Receiver {
    @Fluent
    fun receiverCreate(receiveInputEvent: CommandEventImpl): Receiver

    @Fluent
    fun receiverCreateWithReceipt(receiveInputEvent: CommandEventImpl, resultHandler: Handler<AsyncResult<DataEventImpl>>): Receiver

    @Fluent
    fun receiverUpdate(receiveInputEvent: CommandEventImpl): Receiver

    @Fluent
    fun receiverUpdateWithReceipt(receiveInputEvent: CommandEventImpl, resultHandler: Handler<AsyncResult<DataEventImpl>>): Receiver

    @Fluent
    fun receiverRead(receiveInputEvent: CommandEventImpl, resultHandler: Handler<AsyncResult<DataEventImpl>>): Receiver

    @Fluent
    fun receiverIndex(receiveInputEvent: CommandEventImpl, resultHandler: Handler<AsyncResult<DataEventImpl>>): Receiver

    @Fluent
    fun receiverDelete(receiveInputEvent: CommandEventImpl): Receiver

    @Fluent
    fun receiverDeleteWithReceipt(receiveInputEvent: CommandEventImpl, resultHandler: Handler<AsyncResult<DataEventImpl>>): Receiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): Receiver
}