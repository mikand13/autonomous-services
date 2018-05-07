package org.mikand.autonomous.services.core.communication

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.serviceproxy.ServiceException
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventBuilder
import java.util.*

interface Collector<T> {
    val collectorMap: HashMap<String, Handler<AsyncResult<T>>>
    val collectorAddress: String
        get() = "${javaClass.name}.collector"
    @Suppress("PropertyName")
    val DEFAULT_COLLECTOR_TIMEOUT: Long
        get() = 1000

    fun initializeCollector() : Collector<T> {
        getVertx().eventBus().consumer<JsonObject>(collectorAddress, {
            val inputEvent = CommandEventImpl(it.body())
            val key = inputEvent.body.getString("key")

            iHaveIt(key, Handler {
                if (it.succeeded()) {
                    val handler = collectorMap.remove(key)

                    handler?.handle(Future.succeededFuture(it.result()))
                }
            })
        })

        return this
    }

    fun hasAnyoneCollectedIt(key: String, resultHandler: Handler<AsyncResult<T>>) : Collector<T> {
        val vertx = getVertx()
        val haveIt = CommandEventBuilder()
                .withSuccess()
                .withAction("COLLECT_IT")
                .withBody(JsonObject().put("key", key))
                .build()
        collectorMap[key] = resultHandler

        vertx.eventBus().publish(collectorAddress, haveIt.toJson())

        vertx.setTimer(getCollectorTimeout(), {
            val handler = collectorMap.remove(key)

            handler?.handle(ServiceException.fail(404, "", DataEventBuilder()
                    .withFailure()
                    .withAction("NOONE_HAS_IT")
                    .build()
                    .toJson()))
        })

        return this
    }

    fun iHaveIt(key: String, resultHandler: Handler<AsyncResult<T>>)

    fun getCollectorTimeout() : Long {
        return DEFAULT_COLLECTOR_TIMEOUT
    }

    fun getVertx() : Vertx {
        return Vertx.currentContext().owner()
    }
}