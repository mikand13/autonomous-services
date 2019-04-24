package org.mikand.autonomous.services.storage.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.nannoq.tools.repository.dynamodb.DynamoDBRepository
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import java.net.ServerSocket
import java.time.LocalDate
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.Random
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream

@ExtendWith(VertxExtension::class)
abstract class DynamoDBTestClass : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    protected val testDate = Date()
    protected val nonNullTestModel = {
        TestModel(
                someStringOne = "testString",
                someStringTwo = "testStringRange",
                someStringThree = "testStringThree",
                someStringFour = null,
                someLong = 1L,
                someLongTwo = 0L,
                someInteger = 0,
                someIntegerTwo = 1,
                someBoolean = false,
                someBooleanTwo = false,
                someDate = testDate,
                someDateTwo = Date(),
                documents = emptyList())
    }

    protected val contextObjects: MutableMap<String, Any> = HashMap()

    companion object {
        private var localPort: Int = 0
        private val dynamoDBUtils = DynamoDBUtils()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            localPort = ServerSocket(0).use { it.localPort }
            dynamoDBUtils.startDynamoDB(localPort)
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            dynamoDBUtils.stopDynamoDB(localPort)
        }
    }

    @BeforeEach
    fun setup(testInfo: TestInfo, vertx: Vertx, context: VertxTestContext) {
        contextObjects["${testInfo.testMethod.get().name}-port"] = localPort
        contextObjects["${testInfo.testMethod.get().name}-endpoint"] = "http://localhost:$localPort"

        Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val endpoint = contextObjects["${testInfo.testMethod.get().name}-endpoint"]
        val config = JsonObject()
                .put("dynamo_endpoint", endpoint)
                .put("dynamo_db_iam_id", UUID.randomUUID().toString())
                .put("dynamo_db_iam_key", UUID.randomUUID().toString())
        val classCollection = mapOf(Pair("testModels", TestModel::class.java))
        val finalConfig = getTestConfig().mergeIn(config)

        contextObjects["${testInfo.testMethod.get().name}-repo"] = TestModelReceiverImpl(vertx, finalConfig)
        contextObjects["${testInfo.testMethod.get().name}-config"] = finalConfig

        DynamoDBRepository.initializeDynamoDb(finalConfig, classCollection, Handler {
            if (it.failed()) context.failNow(it.cause())
            context.completeNow()
        })
    }

    fun createItem(repo: TestModelReceiverImpl, createHandler: Handler<AsyncResult<DataEventImpl>>) {
        val receiveEvent = CommandEventBuilder()
                .withSuccess()
                .withAction("RECEIVE_CREATE")
                .withBody(TestModel().toJson())
                .build()

        repo.receiverCreateWithReceipt(receiveEvent, createHandler)
    }

    fun createXItems(
        repo: TestModelReceiverImpl,
        count: Int,
        resultHandler: Handler<AsyncResult<List<TestModel>>>
    ) {
        val items = ArrayList<TestModel>()
        val futures = CopyOnWriteArrayList<Future<*>>()

        IntStream.range(0, count).forEach { i ->
            val testModel = nonNullTestModel()
            testModel.range = (UUID.randomUUID().toString())

            val startDate = LocalDate.of(1990, 1, 1)
            val endDate = LocalDate.now()
            val start = startDate.toEpochDay()
            val end = endDate.toEpochDay()

            val randomEpochDay = ThreadLocalRandom.current().longs(start, end).findAny().asLong

            testModel.setSomeDate(Date(randomEpochDay + 1000L))
            testModel.setSomeDateTwo(Date(randomEpochDay))
            testModel.setSomeLong(Random().nextLong())

            items.add(testModel)
        }

        items.forEach { item ->
            val future = Future.future<DataEventImpl>()
            val receiveEvent = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_CREATE")
                    .withBody(item.toJson())
                    .build()

            repo.receiverCreateWithReceipt(receiveEvent, future)

            futures.add(future)

            Thread.sleep(10)
        }

        CompositeFuture.all(futures).setHandler { res ->
            if (res.failed()) {
                resultHandler.handle(Future.failedFuture(res.cause()))
            } else {
                val collect = futures
                        .map { TestModel((it.result() as DataEventImpl).body) }

                resultHandler.handle(Future.succeededFuture(collect))
            }
        }
    }
}