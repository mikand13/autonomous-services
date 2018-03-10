package org.mikand.autonomous.services.storage.utils

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.*
import org.junit.rules.TestName
import org.junit.runner.RunWith



@RunWith(VertxUnitRunner::class)
abstract class DynamoDBTestClass : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

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
    }

    @After
    fun teardown(context: TestContext) {
        val freePort = context.get<Int>("${name.methodName}-port")
        dynamoDBUtils.stopDynamoDB(freePort)
    }
}