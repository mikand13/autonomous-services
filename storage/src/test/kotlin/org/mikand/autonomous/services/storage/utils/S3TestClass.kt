package org.mikand.autonomous.services.storage.utils

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import io.restassured.RestAssured.given
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.apache.tika.Tika
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File

@RunWith(VertxUnitRunner::class)
abstract class S3TestClass : ConfigSupport, RestAssuredFix {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    val imageFile = File(S3TestClass::class.java.classLoader.getResource("binary/image.png").toURI())

    @JvmField
    @Rule
    val rule = RunTestOnContext()

    @JvmField
    @Rule
    val timeout = Timeout.seconds(30)

    @JvmField
    @Rule
    var name = TestName()

    @Before
    fun setup(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()
        val s3Port = findFreePort()
        val port = findFreePort()
        val api = S3Mock.Builder()
                .withPort(s3Port)
                .withInMemoryBackend()
                .build()
        val s3Endpoint = "http://localhost:$s3Port"
        val bucketName = "test-autonomous-services-bucket$port"

        context.put<String>("${name.methodName}-s3", api)
        context.put<String>("${name.methodName}-s3-port", s3Port)
        context.put<String>("${name.methodName}-config", getTestConfig()
                .put("aws_s3_file_receiver_port", port)
                .put("aws_s3_endPoint", s3Endpoint)
                .put("aws_s3_region", "us-east-1")
                .put("aws_s3_file_receiver_bucketName", bucketName))

        vertx.executeBlocking<Boolean>({
            api.start()

            val endpoint = AwsClientBuilder.EndpointConfiguration(s3Endpoint, "us-east-1")
            val client = AmazonS3ClientBuilder
                    .standard()
                    .withPathStyleAccessEnabled(true)
                    .withEndpointConfiguration(endpoint)
                    .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
                    .build()

            client.createBucket(bucketName)

            context.assertTrue(client.doesBucketExistV2(bucketName))

            it.complete()
        }, false, {
            if (it.succeeded()) {
                async.complete()
            } else {
                context.fail(it.cause())
            }
        })
    }

    @After
    fun teardown(context: TestContext) {
        val async = context.async()
        val api = context.get<S3Mock>("${name.methodName}-s3")

        api.stop()

        async.complete()
    }

    fun uploadFile(url: String, path: String, resultHandler: Handler<AsyncResult<String>>) {
        val vertx = rule.vertx()

        vertx.executeBlocking<String>({
            val file = File(path)

            val key = given().
                    multiPart("upload", file, Tika().detect(file)).
                When().
                    put(url).
                then()
                    .statusCode(202)
                        .extract()
                            .response()
                                .jsonPath()
                                    .getString("key")

            it.complete(key)
        }, false, {
            if (it.succeeded()) {
                resultHandler.handle(Future.succeededFuture(it.result()))
            } else {
                resultHandler.handle(Future.failedFuture(it.cause()))
            }
        })
    }
}