package org.mikand.autonomous.services.processors.splitters.concretes

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.processors.splitters.splitter.SplitEvent
import org.mikand.autonomous.services.processors.splitters.splitter.SplitInputEvent
import org.mikand.autonomous.services.processors.splitters.splitter.Splitter

@VertxGen
@ProxyGen
interface JsonSplitter: Splitter<SplitInputEvent, SplitEvent> {
    @Fluent
    override fun split(splitInputEvent: SplitInputEvent): JsonSplitter

    @Fluent
    override fun splitWithReceipt(splitInputEvent: SplitInputEvent, responseHandler: Handler<AsyncResult<SplitEvent>>): JsonSplitter

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<SplitEvent>>): JsonSplitter
}