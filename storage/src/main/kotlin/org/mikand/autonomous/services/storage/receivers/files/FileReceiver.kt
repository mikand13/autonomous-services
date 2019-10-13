package org.mikand.autonomous.services.storage.receivers.files

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventImpl

@VertxGen
@ProxyGen
interface FileReceiver {
    @Fluent
    fun fileReceiverInitializeRead(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): FileReceiver

    @Fluent
    fun fileReceiverInitializeCreate(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): FileReceiver

    @Fluent
    fun fileReceiverDelete(receiveInputEvent: CommandEventImpl): FileReceiver

    @Fluent
    fun fileReceiverDeleteWithReceipt(
        receiveInputEvent: CommandEventImpl,
        resultHandler: Handler<AsyncResult<DataEventImpl>>
    ): FileReceiver

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): FileReceiver
}
