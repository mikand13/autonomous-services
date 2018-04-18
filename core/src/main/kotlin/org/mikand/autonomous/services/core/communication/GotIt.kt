package org.mikand.autonomous.services.core.communication

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.serviceproxy.ServiceException
import java.time.Instant
import java.util.*
import kotlin.collections.HashSet

interface GotIt<T> {
    val gotItMap: HashMap<Int, HashSet<Long>>
    val gotItAddress: String
        get() = "${javaClass.name}.gotIt"
    @Suppress("PropertyName")
    val DEFAULT_GOT_IT_TIMEOUT: Long
        get() = 5000

    fun initializeGotIt() {
        getVertx().eventBus().consumer<JsonObject>(gotItAddress, {
            val hashSet = gotItMap[it.body().getInteger("hash")]

            if (hashSet != null) {
                hashSet.add(it.body().getLong("time"))
            }
        })
    }

    fun publishGotIt(gotItObject: T, resultHandler: Handler<AsyncResult<T>>) {
        if (gotItObject == null) throw IllegalArgumentException("The gotItObject cannot be null!")

        val vertx = getVertx()
        val randomOffset: Long = (Random().nextInt(1000 - 1) + 1).toLong()
        val timeStamp: Long = Instant.now().toEpochMilli() + randomOffset
        val initialTime: Long = Instant.now().toEpochMilli()
        val objectHash: Int = gotItObject.hashCode()
        val gotItJson = JsonObject()
                .put("time", timeStamp)
                .put("hash", objectHash)
        gotItMap[objectHash] = HashSet()

        vertx.eventBus().publish(gotItAddress, gotItJson)

        checkIfGotIt(initialTime, vertx, gotItObject, objectHash, timeStamp, resultHandler)
    }

    private fun checkIfGotIt(initialTime: Long, vertx: Vertx, gotItObject: T, objectHash: Int, timeStamp: Long,
                             resultHandler: Handler<AsyncResult<T>>) {
        vertx.setTimer(200L, {
            if (gotItMap[objectHash]!!.any { it < timeStamp }) {
                gotItMap.remove(objectHash)

                resultHandler.handle(ServiceException.fail(400, "Already Taken!"))
            } else {
                if (Instant.now().toEpochMilli() > (initialTime + getGotItTimeout())) {
                    gotItMap.remove(objectHash)

                    resultHandler.handle(Future.succeededFuture(gotItObject))
                } else {
                    checkIfGotIt(initialTime, vertx, gotItObject, objectHash, timeStamp, resultHandler)
                }
            }
        })
    }

    fun getGotItTimeout() : Long {
        return DEFAULT_GOT_IT_TIMEOUT
    }

    fun getVertx() : Vertx {
        return Vertx.currentContext().owner()
    }
}