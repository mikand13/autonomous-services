package org.mikand.autonomous.services.processors.splitters.typed.impl

import com.nannoq.tools.repository.repository.results.CreateResult
import com.nannoq.tools.repository.repository.results.DeleteResult
import com.nannoq.tools.repository.repository.results.UpdateResult
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import org.mikand.autonomous.services.processors.splitters.splitter.TypedSplitter
import org.mikand.autonomous.services.processors.splitters.typed.impl.models.TestModel

class TestModelRepository : TestModelSplitter {
    override fun splitCreate(record: TestModel): TypedSplitter<TestModel> {
        return this
    }

    override fun splitCreateWithReceipt(record: TestModel,
                                        createHandler: Handler<AsyncResult<CreateResult<TestModel>>>): TypedSplitter<TestModel> {
        createHandler.handle(Future.succeededFuture(CreateResult(record)))

        return this
    }

    override fun splitUpdate(record: TestModel): TypedSplitter<TestModel> {
        return this
    }

    override fun splitUpdateWithReceipt(record: TestModel,
                                        updateHandler: Handler<AsyncResult<UpdateResult<TestModel>>>): TypedSplitter<TestModel> {
        updateHandler.handle(Future.succeededFuture(UpdateResult(record)))

        return this
    }

    override fun splitDelete(record: TestModel): TypedSplitter<TestModel> {
        return this
    }

    override fun splitDeleteWithReceipt(record: TestModel,
                                        deleteHandler: Handler<AsyncResult<DeleteResult<TestModel>>>): TypedSplitter<TestModel> {
        deleteHandler.handle(Future.succeededFuture(DeleteResult(record)))

        return this
    }
}