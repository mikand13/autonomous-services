package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.repository.dynamodb.DynamoDBRepository
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.utils.DynamoDBTestClass

@RunWith(VertxUnitRunner::class)
class DynamoDBReceiverTest : DynamoDBTestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Before
    fun before(context: TestContext) {
        val async = context.async()
        val endpoint = context.get<String>("${name.methodName}-endpoint")
        val config = JsonObject().put("dynamo_endpoint", endpoint)
        val classCollection = mapOf(Pair("testModels", TestModel::class.java))

        context.put<String>("${name.methodName}-repo", TestModelReceiverImpl(rule.vertx(), config))

        DynamoDBRepository.initializeDynamoDb(getTestConfig().mergeIn(config), classCollection) {
            if (it.failed()) context.fail(it.cause())
            async.complete()
        }
    }

    @Test
    fun receiverCreate(context: TestContext) {
    }

    @Test
    fun receiverCreateWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        repo.receiverCreateWithReceipt(TestModel().toJson(), Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(201, it.result().statusCode)
            context.assertNotNull(it.result().statusObject)
            async.complete()
        })
    }

    @Test
    fun receiverUpdate(context: TestContext) {
    }

    @Test
    fun receiverUpdateWithReceipt(context: TestContext) {
    }

    @Test
    fun receiverRead(context: TestContext) {
    }

    @Test
    fun receiverIndex(context: TestContext) {
    }

    @Test
    fun receiverIndexWithQuery(context: TestContext) {
    }

    @Test
    fun receiverDelete(context: TestContext) {
    }

    @Test
    fun receiverDeleteWithReceipt(context: TestContext) {
    }

    @Test
    fun fetchSubscriptionAddress(context: TestContext) {
    }
}