package org.mikand.autonomous.services.data.compute

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventImpl

@VertxGen
@ProxyGen
interface Compute {
    @Fluent
    fun compute(computeInputEvent: CommandEventImpl, responseHandler: Handler<AsyncResult<DataEventImpl>>): Compute

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): Compute
}