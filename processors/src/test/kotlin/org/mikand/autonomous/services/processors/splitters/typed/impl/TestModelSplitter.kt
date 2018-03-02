package org.mikand.autonomous.services.processors.splitters.typed.impl

import com.nannoq.tools.repository.repository.results.CreateResult
import com.nannoq.tools.repository.repository.results.DeleteResult
import com.nannoq.tools.repository.repository.results.UpdateResult
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.processors.splitters.splitter.TypedSplitter
import org.mikand.autonomous.services.processors.splitters.typed.impl.models.TestModel

@VertxGen
@ProxyGen
interface TestModelSplitter : TypedSplitter<TestModel> {
    @Fluent
    override fun splitCreate(record: TestModel): TypedSplitter<TestModel>

    @Fluent
    override fun splitCreateWithReceipt(record: TestModel, createHandler: Handler<AsyncResult<CreateResult<TestModel>>>): TypedSplitter<TestModel>

    @Fluent
    override fun splitUpdate(record: TestModel): TypedSplitter<TestModel>

    @Fluent
    override fun splitUpdateWithReceipt(record: TestModel, updateHandler: Handler<AsyncResult<UpdateResult<TestModel>>>): TypedSplitter<TestModel>

    @Fluent
    override fun splitDelete(record: TestModel): TypedSplitter<TestModel>

    @Fluent
    override fun splitDeleteWithReceipt(record: TestModel, deleteHandler: Handler<AsyncResult<DeleteResult<TestModel>>>): TypedSplitter<TestModel>
}