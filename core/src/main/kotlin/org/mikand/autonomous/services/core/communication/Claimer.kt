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

interface Claimer<T> {
    val claimerMap: HashMap<Int, HashSet<Long>>
    val claimerAddress: String
        get() = "${javaClass.name}.claimer"
    @Suppress("PropertyName")
    val DEFAULT_CLAIMER_TIMEOUT: Long
        get() = 1000

    fun inititializeClaimer() : Claimer<T> {
        getVertx().eventBus().consumer<JsonObject>(claimerAddress) {
            val inputEvent = CommandEventImpl(it.body())

            claimerMap[inputEvent.body.getInteger("hash")]?.add(inputEvent.body.getLong("time"))
        }

        return this
    }

    fun claim(claimObject: T, resultHandler: Handler<AsyncResult<T>>) : Claimer<T> {
        if (claimObject == null) throw IllegalArgumentException("The claimObject cannot be null!")

        val vertx = getVertx()
        val timeStamp: Long = now()
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.DAYS)
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.HOURS)
                .plus((Random().nextInt(1_000 - 1) + 1).toLong(), ChronoUnit.MINUTES)
                .toEpochMilli()
        val initialTime: Long = now().toEpochMilli()
        val objectHash: Int = claimObject.hashCode()
        val gotItEvent = CommandEventBuilder()
                .withSuccess()
                .withAction("GOT_IT")
                .withBody(JsonObject()
                    .put("time", timeStamp)
                    .put("hash", objectHash))
                .build()

        claimerMap[objectHash] = HashSet()

        vertx.eventBus().publish(claimerAddress, gotItEvent.toJson())

        return checkIfIveClaimedIt(initialTime, vertx, claimObject, objectHash, timeStamp, resultHandler)
    }

    private fun checkIfIveClaimedIt(initialTime: Long, vertx: Vertx, gotItObject: T, objectHash: Int, timeStamp: Long,
                                    resultHandler: Handler<AsyncResult<T>>) : Claimer<T> {
        vertx.setTimer(200L) {
            if (claimerMap[objectHash]!!.any { it < timeStamp }) {
                claimerMap.remove(objectHash)

                resultHandler.handle(ServiceException.fail(400, "Already Taken!"))
            } else {
                if (now().toEpochMilli() > (initialTime + getClaimerTimeout())) {
                    claimerMap.remove(objectHash)

                    resultHandler.handle(Future.succeededFuture(gotItObject))
                } else {
                    checkIfIveClaimedIt(initialTime, vertx, gotItObject, objectHash, timeStamp, resultHandler)
                }
            }
        }

        return this
    }

    fun getClaimerTimeout() : Long {
        return DEFAULT_CLAIMER_TIMEOUT
    }

    fun getVertx() : Vertx {
        return Vertx.currentContext().owner()
    }
}