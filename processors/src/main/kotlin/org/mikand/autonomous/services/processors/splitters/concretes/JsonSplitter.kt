package org.mikand.autonomous.services.processors.splitters.concretes

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.processors.splitters.splitter.Splitter

@VertxGen
@ProxyGen
interface JsonSplitter : Splitter<CommandEventImpl, DataEventImpl> {
    @Fluent
    override fun split(splitInputEvent: CommandEventImpl): JsonSplitter

    @Fluent
    override fun splitWithReceipt(splitInputEvent: CommandEventImpl, responseHandler: Handler<AsyncResult<DataEventImpl>>): JsonSplitter

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): JsonSplitter
}
