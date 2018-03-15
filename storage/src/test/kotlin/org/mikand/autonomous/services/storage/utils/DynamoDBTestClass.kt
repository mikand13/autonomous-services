package org.mikand.autonomous.services.storage.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.nannoq.tools.repository.dynamodb.DynamoDBRepository
import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.*
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.receivers.ReceiveEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType
import org.mikand.autonomous.services.storage.receivers.ReceiveStatus
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream


@RunWith(VertxUnitRunner::class)
abstract class DynamoDBTestClass : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val testDate = Date()
    private val nonNullTestModel = {
        TestModel()
                .setSomeStringOne("testString")
                .setSomeStringTwo("testStringRange")
                .setSomeStringThree("testStringThree")
                .setSomeLong(1L)
                .setSomeDate(testDate)
                .setSomeDateTwo(Date())
    }

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(30)

    @JvmField
    @Rule
    var name = TestName()

    companion object {
        private lateinit var dynamoDBUtils: DynamoDBUtils

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            dynamoDBUtils = DynamoDBUtils()
        }

        @AfterClass
        @JvmStatic
        fun teardownClass() {
            dynamoDBUtils.stopAll()
        }
    }

    @Before
    fun setup(context: TestContext) {
        val freePort = findFreePort()
        dynamoDBUtils.startDynamoDB(freePort)
        context.put<String>("${name.methodName}-port", freePort)
        context.put<String>("${name.methodName}-endpoint", "http://localhost:$freePort")

        Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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

    @After
    fun teardown(context: TestContext) {
        val freePort = context.get<Int>("${name.methodName}-port")
        dynamoDBUtils.stopDynamoDB(freePort)
    }

    fun createItem(repo: TestModelReceiverImpl, createHandler: Handler<AsyncResult<ReceiveEvent>>) {
        val receiveEvent = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_CREATE",
                ReceiveStatus(201, statusObject = TestModel().toJson()))

        repo.receiverCreateWithReceipt(receiveEvent, createHandler)
    }

    fun createXItems(repo: TestModelReceiverImpl, count: Int,
                     resultHandler: Handler<AsyncResult<List<TestModel>>>) {
        val items = ArrayList<TestModel>()
        val futures = CopyOnWriteArrayList<Future<*>>()

        IntStream.range(0, count).forEach { i ->
            val testModel = nonNullTestModel().setRange(UUID.randomUUID().toString())

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
            val future = Future.future<ReceiveEvent>()
            val receiveEvent = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_CREATE",
                    ReceiveStatus(201, statusObject = item.toJson()))

            repo.receiverCreateWithReceipt(receiveEvent, future.completer())

            futures.add(future)

            Thread.sleep(10)
        }

        CompositeFuture.all(futures).setHandler { res ->
            if (res.failed()) {
                resultHandler.handle(Future.failedFuture(res.cause()))
            } else {
                val collect = futures
                        .map { TestModel((it.result() as ReceiveEvent).body.statusObject) }

                resultHandler.handle(Future.succeededFuture(collect))
            }
        }
    }
}