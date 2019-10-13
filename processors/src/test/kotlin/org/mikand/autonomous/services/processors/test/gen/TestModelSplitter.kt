package org.mikand.autonomous.services.processors.test.gen

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import org.mikand.autonomous.services.processors.splitters.splitter.TypedSplitter
import org.mikand.autonomous.services.processors.test.gen.models.TestModel

@VertxGen
@ProxyGen
interface TestModelSplitter : TypedSplitter<TestModel> {
    @Fluent
    override fun splitCreate(record: TestModel): TestModelSplitter

    @Fluent
    override fun splitCreateWithReceipt(
        record: TestModel,
        createHandler: Handler<AsyncResult<TestModel>>
    ): TestModelSplitter

    @Fluent
    override fun splitUpdate(record: TestModel): TestModelSplitter

    @Fluent
    override fun splitUpdateWithReceipt(
        record: TestModel,
        updateHandler: Handler<AsyncResult<TestModel>>
    ): TestModelSplitter

    @Fluent
    override fun splitDelete(record: TestModel): TestModelSplitter

    @Fluent
    override fun splitDeleteWithReceipt(
        record: TestModel,
        deleteHandler: Handler<AsyncResult<TestModel>>
    ): TestModelSplitter

    @Fluent
    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): TestModelSplitter
}
