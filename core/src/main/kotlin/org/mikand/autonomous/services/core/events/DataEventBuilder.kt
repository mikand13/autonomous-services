package org.mikand.autonomous.services.core.events

import io.vertx.core.json.JsonObject
import java.util.UUID

class DataEventBuilder {
    private val id: String = UUID.randomUUID().toString()
    private var type: DataEventType? = null
    private var action: String? = ""
    private var metadata: JsonObject? = JsonObject()
    private var body: JsonObject? = JsonObject()

    fun withSuccess(): DataEventBuilder {
        type = DataEventType.DATA

        return this
    }

    fun withFailure(): DataEventBuilder {
        type = DataEventType.DATA

        return this
    }

    fun withAction(action: String): DataEventBuilder {
        this.action = action

        return this
    }

    fun withMetadata(metadata: JsonObject): DataEventBuilder {
        this.metadata = metadata

        return this
    }

    fun withBody(body: JsonObject): DataEventBuilder {
        this.body = body

        return this
    }

    fun build(): DataEventImpl {
        if (type == null) throw IllegalArgumentException("Type cannot be null!")
        if (action == "") throw IllegalArgumentException("Action must be != \"\"")

        return DataEventImpl(id, type!!, action!!, metadata!!, body!!)
    }
}
