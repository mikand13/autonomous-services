package org.mikand.autonomous.services.data.model

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.data.compute.Compute

class JsonCompute : Compute {
    override fun compute(computeInputEvent: CommandEventImpl,
                         responseHandler: Handler<AsyncResult<DataEventImpl>>): Compute {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("JSON_COMPUTE")
                .withMetadata(JsonObject().put("statusCode", 200))
                .withBody(JsonObject().put("output", JsonCompute::class.java.simpleName))
                .build()

        responseHandler.handle(Future.succeededFuture(outputEvent))

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): Compute {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("ADDRESS")
                .withMetadata(JsonObject().put("statusCode", 200))
                .withBody(JsonObject().put("address", JsonCompute::class.java.simpleName))
                .build()

        addressHandler.handle(Future.succeededFuture(outputEvent))

        return this
    }
}