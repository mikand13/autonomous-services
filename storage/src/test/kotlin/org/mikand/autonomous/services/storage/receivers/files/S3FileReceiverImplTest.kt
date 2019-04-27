package org.mikand.autonomous.services.storage.receivers.files

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.storage.utils.S3TestClass

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(VertxExtension::class)
class S3FileReceiverImplTest : S3TestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var fileReceiver: S3FileReceiverImpl

    @BeforeEach
    fun setupFileReceiver(testInfo: TestInfo) {
        fileReceiver = S3FileReceiverImpl(contextObjects["${testInfo.testMethod.get().name}-config"] as JsonObject)
    }

    @Test
    fun testInitializeCreate(vertx: Vertx, context: VertxTestContext) {
        vertx.deployVerticle(fileReceiver) { asyncResult ->
            if (asyncResult.failed()) logger.error("Failure!", asyncResult.cause())

            context.verify {
                assertThat(asyncResult.succeeded()).isTrue()
            }

            fileReceiver.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                }

                uploadFile(vertx, it.result().body.getString("uploadUrl"), imageFile.absolutePath, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    context.completeNow()
                })
            })
        }
    }

    @Test
    fun testDeleteWithReceipt(vertx: Vertx, context: VertxTestContext) {
        vertx.deployVerticle(fileReceiver) { asyncResult ->
            if (asyncResult.failed()) logger.error("Failure!", asyncResult.cause())

            context.verify {
                assertThat(asyncResult.succeeded()).isTrue()
            }

            fileReceiver.fileReceiverInitializeCreate(buildUploadEvent(), Handler { result ->
                context.verify {
                    assertThat(result.succeeded()).isTrue()
                }

                uploadFile(vertx, result.result().body.getString("uploadUrl"), imageFile.absolutePath, Handler {
                    if (it.succeeded()) {
                        fileReceiver.fileReceiverDeleteWithReceipt(buildDeletwEvent(it.result()), Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()

                                context.completeNow()
                            }
                        })
                    } else {
                        context.failNow(it.cause())
                    }
                })
            })
        }
    }

    private fun buildUploadEvent(): CommandEventImpl {
        return CommandEventBuilder()
                .withSuccess()
                .withAction("FILE_UPLOAD")
                .build()
    }

    private fun buildDeletwEvent(key: String): CommandEventImpl {
        return CommandEventBuilder()
                .withSuccess()
                .withAction("FILE_DELETE")
                .withBody(JsonObject().put("key", key))
                .build()
    }

    @Test
    fun testFetchSubscriptionAddress(vertx: Vertx, context: VertxTestContext) {
        val address = "${S3FileReceiverImpl::class.java.name}.data"

        vertx.deployVerticle(fileReceiver) { result ->
            if (result.failed()) logger.error("Failure!", result.cause())

            context.verify {
                assertThat(result.succeeded()).isTrue()
            }

            fileReceiver.fetchSubscriptionAddress(Handler {
                context.verify {
                    assertThat(it.succeeded()).isTrue()
                    assertThat(it.result().body.getString("address")).isEqualTo(address)

                    context.completeNow()
                }
            })
        }
    }
}