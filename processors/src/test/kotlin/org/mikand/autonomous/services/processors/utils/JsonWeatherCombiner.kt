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

package org.mikand.autonomous.services.processors.utils

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.processors.combiners.impl.JsonCombinerImpl

class JsonWeatherCombiner(config: JsonObject = JsonObject()) : JsonCombinerImpl(config) {
    override fun combine(combineInputEvent: CommandEventImpl, responseHandler: Handler<AsyncResult<DataEventImpl>>): JsonWeatherCombiner {
        responseHandler.handle(Future.succeededFuture(DataEventBuilder()
                .withSuccess()
                .withAction("WEATHER")
                .withMetadata(JsonObject().put("statusCode", 200))
                .build()))

        return this
    }
}
