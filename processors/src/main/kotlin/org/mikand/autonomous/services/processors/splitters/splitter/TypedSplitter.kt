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

package org.mikand.autonomous.services.processors.splitters.splitter

import com.nannoq.tools.repository.models.Model
import com.nannoq.tools.repository.repository.results.CreateResult
import com.nannoq.tools.repository.repository.results.DeleteResult
import com.nannoq.tools.repository.repository.results.UpdateResult
import io.vertx.codegen.annotations.Fluent
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface TypedSplitter<T : Model> {
    @Fluent
    fun splitCreate(record: T) : TypedSplitter<T>

    @Fluent
    fun splitCreateWithReceipt(record: T, createHandler: Handler<AsyncResult<CreateResult<T>>>) : TypedSplitter<T>

    @Fluent
    fun splitUpdate(record: T) : TypedSplitter<T>

    @Fluent
    fun splitUpdateWithReceipt(record: T, updateHandler: Handler<AsyncResult<UpdateResult<T>>>) : TypedSplitter<T>

    @Fluent
    fun splitDelete(record: T) : TypedSplitter<T>

    @Fluent
    fun splitDeleteWithReceipt(record: T, deleteHandler: Handler<AsyncResult<DeleteResult<T>>>) : TypedSplitter<T>

    @Fluent
    fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): TypedSplitter<T>
}