package org.mikand.autonomous.services.storage.receivers.files

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.storage.receivers.ReceiveEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent

@VertxGen
@ProxyGen
interface FileReceiver {
    @Fluent
    fun fileReceiverInitializeRead(receiveInputEvent: ReceiveInputEvent,
                                   resultHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver

    @Fluent
    fun fileReceiverInitializeCreate(receiveInputEvent: ReceiveInputEvent,
                                     resultHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver

    @Fluent
    fun fileReceiverDelete(receiveInputEvent: ReceiveInputEvent): FileReceiver

    @Fluent
    fun fileReceiverDeleteWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                      resultHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver
}