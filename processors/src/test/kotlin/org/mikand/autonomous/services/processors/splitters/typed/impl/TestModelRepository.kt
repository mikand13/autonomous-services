package org.mikand.autonomous.services.processors.splitters.typed.impl

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.*
import org.mikand.autonomous.services.processors.test.gen.TestModelSplitter
import org.mikand.autonomous.services.processors.test.gen.models.TestModel
import org.mikand.autonomous.services.processors.test.gen.models.TestModelCodec

class TestModelRepository : AbstractVerticle, TestModelSplitter {
    private val publishAddress: String = javaClass.simpleName
    private val subscriptionAddress: String = javaClass.name
    private val thisVertx: Vertx = vertx ?: Vertx.currentContext().owner()

    companion object {
        var initializedCodec = false
    }

    constructor() {
        initializeCodec()
    }

    override fun start(startFuture: Future<Void>?) {
        initializeCodec()

        ServiceManager.getInstance().publishService(TestModelSplitter::class.java, publishAddress, this) {
            if (it.succeeded()) {
                startFuture?.complete()
            } else {
                startFuture?.fail(it.cause())
            }
        }
    }

    @Synchronized
    private fun initializeCodec() {
        if (initializedCodec) return else {
            thisVertx.eventBus().registerDefaultCodec(TestModel::class.java, TestModelCodec())
        }
    }

    override fun splitCreate(record: TestModel): TestModelRepository {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitCreateWithReceipt(record: TestModel,
                                        createHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        createHandler.handle(Future.succeededFuture(record))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdate(record: TestModel): TestModelRepository {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitUpdateWithReceipt(record: TestModel,
                                        updateHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        updateHandler.handle(Future.succeededFuture(record))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDelete(record: TestModel): TestModelRepository {
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun splitDeleteWithReceipt(record: TestModel,
                                        deleteHandler: Handler<AsyncResult<TestModel>>): TestModelRepository {
        deleteHandler.handle(Future.succeededFuture(record))
        thisVertx.eventBus().publish(subscriptionAddress, record)

        return this
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<String>>): TestModelRepository {
        addressHandler.handle(Future.succeededFuture(subscriptionAddress))

        return this
    }
}