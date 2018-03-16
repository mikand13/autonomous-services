package org.mikand.autonomous.services.data.compute

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject

@DataObject(generateConverter = true)
class ComputeStatus {
    var statusCode: Int = 500
    var statusMessage: String = ""
    var statusObject: JsonObject = JsonObject()

    constructor(statusMessage: String = "", statusObject: JsonObject = JsonObject()) :
            this(500, statusMessage, statusObject)

    constructor(statusCode: Int, statusMessage: String = "", statusObject: JsonObject = JsonObject()) {
        this.statusCode = statusCode
        this.statusMessage = statusMessage
        this.statusObject = statusObject
    }

    constructor(jsonObject: JsonObject) {
        ComputeStatusConverter.fromJson(jsonObject, this)
    }

    fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}