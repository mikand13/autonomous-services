package org.mikand.autonomous.services.core.events

import io.vertx.core.json.JsonObject
import java.util.UUID

class CommandEventBuilder {
    private val id: String = UUID.randomUUID().toString()
    private var type: CommandEventType? = null
    private var action: String? = ""
    private var metadata: JsonObject? = JsonObject()
    private var body: JsonObject? = JsonObject()

    fun withSuccess(): CommandEventBuilder {
        type = CommandEventType.COMMAND

        return this
    }

    fun withFailure(): CommandEventBuilder {
        type = CommandEventType.COMMAND_FAILURE

        return this
    }

    fun withAction(action: String): CommandEventBuilder {
        this.action = action

        return this
    }

    fun withMetadata(metadata: JsonObject): CommandEventBuilder {
        this.metadata = metadata

        return this
    }

    fun withBody(body: JsonObject): CommandEventBuilder {
        this.body = body

        return this
    }

    fun build(): CommandEventImpl {
        if (type == null) throw IllegalArgumentException("Type cannot be null!")
        if (action == "") throw IllegalArgumentException("Action must be != \"\"")

        return CommandEventImpl(id, type!!, action!!, metadata!!, body!!)
    }
}
