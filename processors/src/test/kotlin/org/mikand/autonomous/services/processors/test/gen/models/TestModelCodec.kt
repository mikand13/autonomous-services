package org.mikand.autonomous.services.processors.test.gen.models

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

class TestModelCodec : MessageCodec<TestModel, TestModel> {
    override fun encodeToWire(buffer: Buffer?, s: TestModel?) {
        val json = s?.toJson()
        val jsonString = json?.encode()
        val jsonBytes = jsonString?.toByteArray()?.size ?: 0

        buffer?.appendInt(jsonBytes)
        buffer?.appendString(jsonString)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer?): TestModel {
        val position = pos
        val length = buffer?.getInt(position) ?: 0

        val first = position + 4
        val second = first + length
        val jsonString = buffer?.getString(first, second) // getInt() == 4 bytes

        return TestModel(JsonObject(jsonString))
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    override fun transform(s: TestModel?): TestModel {
        return s ?: throw IllegalArgumentException("This cannot and should not be null!")
    }

    override fun name(): String {
        return javaClass.simpleName
    }
}