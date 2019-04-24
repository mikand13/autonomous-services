/*
 * MIT License
 *
 * Copyright (c) 2017 Anders Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.mikand.autonomous.services.core.events

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.json.JsonObject
import java.util.UUID

@DataObject(generateConverter = true)
class DataEventImpl : DataEvent {
    var id: String = UUID.randomUUID().toString()
    var type: String = ""
    var action: String = ""
    var metadata: JsonObject = JsonObject()
    var body: JsonObject = JsonObject()

    internal constructor(
        id: String = UUID.randomUUID().toString(),
        type: DataEventType,
        action: String,
        metadata: JsonObject = JsonObject(),
        body: JsonObject
    ) {
        this.id = id
        this.type = type.name
        this.action = action
        this.metadata = metadata
        this.body = body
    }

    constructor(jsonObject: JsonObject) {
        DataEventImplConverter.fromJson(jsonObject, this)
    }

    fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataEventImpl

        return when {
            id != other.id -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}