package org.mikand.autonomous.services.data.compute

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

@VertxGen
@ProxyGen
interface Compute {
    @Fluent
    fun combine(computeInputEvent: ComputeInputEvent, responseHandler: Handler<AsyncResult<ComputeEvent>>): Compute

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ComputeEvent>>): Compute
}