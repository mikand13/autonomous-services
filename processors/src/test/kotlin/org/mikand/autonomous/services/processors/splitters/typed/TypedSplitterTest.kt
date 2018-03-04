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

package org.mikand.autonomous.services.processors.splitters.typed

import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.processors.splitters.typed.impl.TestModelRepository
import org.mikand.autonomous.services.processors.splitters.typed.impl.models.TestModel
import org.mikand.autonomous.services.processors.utils.ConfigSupport

@RunWith(VertxUnitRunner::class)
class TypedSplitterTest : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(5)

    @Test
    fun testSplitCreate(context: TestContext) {
        val async = context.async()
        val splitter = TestModelRepository()
        val model = TestModel()

        splitter.splitCreateWithReceipt(model, Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(model, it.result(), "Object is not equal!")

            splitter.splitCreate(model)

            async.complete()
        })
    }

    @Test
    fun testSplitUpdate(context: TestContext) {
        val async = context.async()
        val splitter = TestModelRepository()
        val model = TestModel()

        splitter.splitUpdateWithReceipt(model, Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(model, it.result(), "Object is not equal!")

            splitter.splitUpdate(model)

            async.complete()
        })
    }

    @Test
    fun testSplitDelete(context: TestContext) {
        val async = context.async()
        val splitter = TestModelRepository()
        val model = TestModel()

        splitter.splitDeleteWithReceipt(model, Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(model, it.result(), "Object is not equal!")

            splitter.splitDelete(model)

            async.complete()
        })
    }
}