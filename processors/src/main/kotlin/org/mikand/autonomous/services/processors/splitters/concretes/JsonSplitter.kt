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
    override fun split(data: JsonObject): Splitter<JsonObject, JsonObject>

    @Fluent
    override fun splitWithReceipt(data: JsonObject,
                                  responseHandler: Handler<AsyncResult<SplitterStatus>>)
            : Splitter<JsonObject, JsonObject>

    @GenIgnore
    fun recurseIntoKey(data: JsonObject, output: JsonObject, extractables: List<String>, it: String) {
        val keyMap = it.split(".")

        if (keyMap.isEmpty()) throw IllegalStateException("$keyMap should not be empty!")

        if (keyMap.size == 2) {
            if (extractables.contains(it)) {
                if (!output.containsKey(keyMap[0])) {
                    output.put(keyMap[0], JsonObject())
                }

                val keyObject = data.getJsonObject(keyMap[0])

                if (keyObject.containsKey(keyMap[1])) {
                    val value = keyObject.getValue(keyMap[1])

                    output.getJsonObject(keyMap[0]).put(keyMap[1], value)
                }
            }
        } else {
            val modifiedIt = keyMap.drop(1).toTypedArray().joinToString { "." }
            val newExtractables = arrayListOf(modifiedIt)

            recurseIntoKey(data.getJsonObject(keyMap[0]), output, newExtractables, modifiedIt)
        }
    }

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>)
            : Splitter<JsonObject, JsonObject>
}