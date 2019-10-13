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
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import java.io.File
import java.net.ServerSocket
import java.util.HashMap
import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.mikand.autonomous.services.core.events.DataEventImpl

@ExtendWith(VertxExtension::class)
abstract class S3TestClass : ConfigSupport {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    val imageFile = File(S3TestClass::class.java.classLoader.getResource("binary/image.png").toURI())
    protected val contextObjects: MutableMap<String, Any> = HashMap()

    @BeforeEach
    fun setup(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val s3Port = ServerSocket(0).use { it.localPort }
        val port = ServerSocket(0).use { it.localPort }
        val api = S3Mock.Builder()
                .withPort(s3Port)
                .withInMemoryBackend()
                .build()
        val s3Endpoint = "http://localhost:$s3Port"
        val bucketName = "test-autonomous-services-bucket$port"

        contextObjects["${testInfo.testMethod.get().name}-s3"] = api
        contextObjects["${testInfo.testMethod.get().name}-s3-port"] = s3Port
        contextObjects["${testInfo.testMethod.get().name}-config"] = getTestConfig()
                .put("aws_s3_file_receiver_port", port)
                .put("aws_s3_endPoint", s3Endpoint)
                .put("aws_s3_region", "us-east-1")
                .put("aws_s3_file_receiver_bucketName", bucketName)

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

            context.verify {
                assertThat(client.doesBucketExistV2(bucketName)).isTrue()
            }

            it.complete()
        }, false, {
            if (it.failed()) context.failNow(it.cause())

            context.completeNow()
        })
    }

    @AfterEach
    fun teardown(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val api: S3Mock = contextObjects["${testInfo.testMethod.get().name}-s3"] as S3Mock

        api.stop()

        context.completeNow()
    }

    fun uploadFile(vertx: Vertx, url: String, path: String, resultHandler: Handler<AsyncResult<String>>) {
        vertx.executeBlocking<String>({
            val file = File(path)

            val key = DataEventImpl(JsonObject(given()
                    .multiPart("upload", file, Tika().detect(file))
                .`when`()
                    .post(url)
                .then()
                    .statusCode(202)
                        .extract()
                            .response()
                                .asString()))
                                    .body.getString("key")

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
