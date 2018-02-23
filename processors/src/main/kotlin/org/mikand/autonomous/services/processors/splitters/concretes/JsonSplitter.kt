package org.mikand.autonomous.services.processors.splitters.concretes

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.GenIgnore
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.processors.splitters.splitter.Splitter
import org.mikand.autonomous.services.processors.splitters.splitter.SplitterStatus

@VertxGen
@ProxyGen
interface JsonSplitter: Splitter<JsonObject, JsonObject> {
    @Fluent
    override fun split(data: JsonObject): JsonSplitter

    @Fluent
    override fun splitWithReceipt(data: JsonObject,
                                  responseHandler: Handler<AsyncResult<SplitterStatus>>)
            : JsonSplitter

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>)
            : JsonSplitter
}