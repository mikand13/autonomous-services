package org.mikand.autonomous.services.core.events

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject
import java.util.*

@DataObject(generateConverter = true)
class CommandEventImpl : CommandEvent {
    var id: String = UUID.randomUUID().toString()
    var type: String = ""
    var action: String = ""
    var metadata: JsonObject = JsonObject()
    var body: JsonObject = JsonObject()

    internal constructor(id: String = UUID.randomUUID().toString(), type: CommandEventType, action: String,
                         metadata: JsonObject = JsonObject(), body: JsonObject) {
        this.id = id
        this.type = type.name
        this.action = action
        this.metadata = metadata
        this.body = body
    }

    constructor(jsonObject: JsonObject) {
        CommandEventImplConverter.fromJson(jsonObject, this)
    }

    fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandEventImpl

        return when {
            id != other.id -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}