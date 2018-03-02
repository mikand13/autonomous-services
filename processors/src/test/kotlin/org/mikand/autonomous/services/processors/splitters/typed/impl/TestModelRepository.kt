package org.mikand.autonomous.services.processors.splitters.typed.impl

import com.nannoq.tools.cluster.services.ServiceManager
import com.nannoq.tools.repository.repository.results.CreateResult
import com.nannoq.tools.repository.repository.results.DeleteResult
import com.nannoq.tools.repository.repository.results.UpdateResult
import io.vertx.core.*
import org.mikand.autonomous.services.processors.splitters.splitter.TypedSplitter
import org.mikand.autonomous.services.processors.splitters.typed.impl.models.TestModel

class TestModelRepository : AbstractVerticle(), TestModelSplitter {
    private val publishAddress: String = javaClass.simpleName
    private val subscriptionAddress: String = javaClass.name
    private val thisVertx: Vertx = vertx ?: Vertx.currentContext().owner()

    override fun start(startFuture: Future<Void>?) {
        ServiceManager.getInstance().publishService(TestModelSplitter::class.java, publishAddress, this) {
            if (it.succeeded()) {
                startFuture?.complete()
            } else {
                startFuture?.fail(it.cause())
            }
        }
    }

    override fun splitCreate(record: TestModel): TypedSplitter<TestModel> {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitCreateWithReceipt(record: TestModel,
                                        createHandler: Handler<AsyncResult<CreateResult<TestModel>>>): TypedSplitter<TestModel> {
        createHandler.handle(Future.succeededFuture(CreateResult(record)))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdate(record: TestModel): TypedSplitter<TestModel> {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdateWithReceipt(record: TestModel,
                                        updateHandler: Handler<AsyncResult<UpdateResult<TestModel>>>): TypedSplitter<TestModel> {
        updateHandler.handle(Future.succeededFuture(UpdateResult(record)))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDelete(record: TestModel): TypedSplitter<TestModel> {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDeleteWithReceipt(record: TestModel,
                                        deleteHandler: Handler<AsyncResult<DeleteResult<TestModel>>>): TypedSplitter<TestModel> {
        deleteHandler.handle(Future.succeededFuture(DeleteResult(record)))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): TypedSplitter<TestModel> {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}