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

import com.nannoq.tools.repository.utils.GenericItemList
import com.nannoq.tools.repository.utils.PageTokens
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.processors.test.gen.TestModelCombiner
import org.mikand.autonomous.services.processors.test.gen.TestModelSplitter
import org.mikand.autonomous.services.processors.test.gen.models.TestModel
import org.mikand.autonomous.services.processors.test.gen.models.TestModelCodec
import java.util.*

class TestModelRepository : AbstractVerticle(), TestModelSplitter, TestModelCombiner {
    private val subscriptionAddress: String = javaClass.name

    override fun start() {
        initializeCodec()
    }

    private fun initializeCodec() {
        try {
            vertx.eventBus().registerDefaultCodec(TestModel::class.java, TestModelCodec())
        } catch (ignored: Exception) {}
    }

    override fun splitCreate(record: TestModel): TestModelRepository {
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitCreateWithReceipt(record: TestModel,
                                        createHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        createHandler.handle(Future.succeededFuture(record))
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdate(record: TestModel): TestModelRepository {
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdateWithReceipt(record: TestModel,
                                        updateHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        updateHandler.handle(Future.succeededFuture(record))
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDelete(record: TestModel): TestModelRepository {
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDeleteWithReceipt(record: TestModel,
                                        deleteHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        deleteHandler.handle(Future.succeededFuture(record))
        vertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun combineRead(query: JsonObject, readHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        readHandler.handle(Future.succeededFuture(TestModel()))

        return this
    }

    override fun combineReadAll(query: JsonObject, readAllHandler: Handler<AsyncResult<GenericItemList>>): TestModelRepository {
        readAllHandler.handle(Future.succeededFuture(GenericItemList(PageTokens(), 1, Collections.singletonList(TestModel().toJsonFormat()))))

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): TestModelRepository {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}