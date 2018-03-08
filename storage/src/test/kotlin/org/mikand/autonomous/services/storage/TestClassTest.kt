package org.mikand.autonomous.services.storage

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.*
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.utils.ConfigSupport
import org.mikand.autonomous.services.storage.utils.DynamoDBUtils



@RunWith(VertxUnitRunner::class)
class TestClassTest : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(30)

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
        dynamoDBUtils.startDynmodDB(freePort)
        context.put<String>("port", freePort)
        context.put<String>("endpoint", "http://localhost:$freePort")
    }

    @After
    fun teardown(context: TestContext) {
        val freePort = context.get<Int>("port")
        dynamoDBUtils.stopDynamoDB(freePort)
    }

    @Test
    fun testDynamoDB(context: TestContext) {
        val amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(context.get("endpoint"), "eu-west-1"))
                .build()

        amazonDynamoDB.listTables()
    }
}