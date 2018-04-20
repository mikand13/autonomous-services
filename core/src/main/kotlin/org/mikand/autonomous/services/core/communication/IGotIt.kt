package org.mikand.autonomous.services.core.communication

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.serviceproxy.ServiceException
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import java.time.Instant.now
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.HashSet

interface IGotIt<T> {
    val gotItMap: HashMap<Int, HashSet<Long>>
    val gotItAddress: String
        get() = "${javaClass.name}.gotIt"
    @Suppress("PropertyName")
    val DEFAULT_GOT_IT_TIMEOUT: Long
        get() = 1000

    fun initializeIGotIt() : IGotIt<T> {
        getVertx().eventBus().consumer<JsonObject>(getIGotItTAddress(), {
            val inputEvent = CommandEventImpl(it.body())

            gotItMap[inputEvent.body.getInteger("hash")]?.add(inputEvent.body.getLong("time"))
        })

        return this
    }

    fun iGotIt(gotItObject: T, resultHandler: Handler<AsyncResult<T>>) : IGotIt<T> {
        if (gotItObject == null) throw IllegalArgumentException("The gotItObject cannot be null!")

        val vertx = getVertx()
        val timeStamp: Long = now()
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.DAYS)
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.HOURS)
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.MINUTES)
                .toEpochMilli()
        val initialTime: Long = now().toEpochMilli()
        val objectHash: Int = gotItObject.hashCode()
        val gotItEvent = CommandEventBuilder()
                .withSuccess()
                .withAction("GOT_IT")
                .withBody(JsonObject()
                    .put("time", timeStamp)
                    .put("hash", objectHash))
                .build()

        gotItMap[objectHash] = HashSet()

        vertx.eventBus().publish(getIGotItTAddress(), gotItEvent.toJson())

        return checkIfIGotIt(initialTime, vertx, gotItObject, objectHash, timeStamp, resultHandler)
    }

    private fun checkIfIGotIt(initialTime: Long, vertx: Vertx, gotItObject: T, objectHash: Int, timeStamp: Long,
                             resultHandler: Handler<AsyncResult<T>>) : IGotIt<T> {
        vertx.setTimer(200L, {
            if (gotItMap[objectHash]!!.any { it < timeStamp }) {
                gotItMap.remove(objectHash)

                resultHandler.handle(ServiceException.fail(400, "Already Taken!"))
            } else {
                if (now().toEpochMilli() > (initialTime + getIGotItTimeout())) {
                    gotItMap.remove(objectHash)

                    resultHandler.handle(Future.succeededFuture(gotItObject))
                } else {
                    checkIfIGotIt(initialTime, vertx, gotItObject, objectHash, timeStamp, resultHandler)
                }
            }
        })

        return this
    }

    fun getIGotItTAddress() : String {
        return gotItAddress
    }

    fun getIGotItTimeout() : Long {
        return DEFAULT_GOT_IT_TIMEOUT
    }

    fun getVertx() : Vertx {
        return Vertx.currentContext().owner()
    }
}