package org.mikand.autonomous.services.data.model

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.data.compute.Compute
import org.mikand.autonomous.services.data.compute.ComputeEvent
import org.mikand.autonomous.services.data.compute.ComputeEventType.DATA
import org.mikand.autonomous.services.data.compute.ComputeInputEvent
import org.mikand.autonomous.services.data.compute.ComputeStatus

class JsonCompute : Compute {
    override fun compute(computeInputEvent: ComputeInputEvent,
                         responseHandler: Handler<AsyncResult<ComputeEvent>>): Compute {
        responseHandler.handle(Future.succeededFuture(
                ComputeEvent(type = DATA.name, action = "JSON_COMPUTE", body = ComputeStatus(
                        200, statusObject = JsonObject().put("output", JsonCompute::class.java.simpleName)))))

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ComputeEvent>>): Compute {
        addressHandler.handle(Future.succeededFuture(
                ComputeEvent(type = DATA.name, action = "ADDRESS", body = ComputeStatus(
                        200, statusObject = JsonObject().put("address", JsonCompute::class.java.simpleName)))))

        return this
    }
}