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

interface IHaveIt<T> {
    val haveItResponseMap: HashMap<String, Handler<AsyncResult<T>>>
    val haveItAddress: String
        get() = "${javaClass.name}.haveIt"
    @Suppress("PropertyName")
    val DEFAULT_HAVE_IT_TIMEOUT: Long
        get() = 1000

    fun initializeIHaveIt() : IHaveIt<T> {
        getVertx().eventBus().consumer<JsonObject>(getIHaveItTAddress(), {
            val inputEvent = CommandEventImpl(it.body())
            val key = inputEvent.body.getString("key")

            iHaveIt(key, Handler {
                if (it.succeeded()) {
                    val handler = haveItResponseMap.remove(key)

                    handler?.handle(Future.succeededFuture(it.result()))
                }
            })
        })

        return this
    }

    fun doesAnyoneHaveIt(key: String, resultHandler: Handler<AsyncResult<T>>) : IHaveIt<T> {
        val vertx = getVertx()
        val haveIt = CommandEventBuilder()
                .withSuccess()
                .withAction("HAVE_IT")
                .withBody(JsonObject().put("key", key))
                .build()
        haveItResponseMap[key] = resultHandler

        vertx.eventBus().publish(getIHaveItTAddress(), haveIt.toJson())

        vertx.setTimer(getIHaveItTimeout(), {
            val handler = haveItResponseMap.remove(key)

            handler?.handle(ServiceException.fail(404, "", DataEventBuilder()
                    .withFailure()
                    .withAction("NOONE_HAS_IT")
                    .build()
                    .toJson()))
        })

        return this
    }

    fun iHaveIt(key: String, resultHandler: Handler<AsyncResult<T>>)

    fun getIHaveItTAddress() : String {
        return haveItAddress
    }

    fun getIHaveItTimeout() : Long {
        return DEFAULT_HAVE_IT_TIMEOUT
    }

    fun getVertx() : Vertx {
        return Vertx.currentContext().owner()
    }
}