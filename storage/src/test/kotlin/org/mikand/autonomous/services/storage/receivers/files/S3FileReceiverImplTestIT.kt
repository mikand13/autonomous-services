package org.mikand.autonomous.services.storage.receivers.files

import com.nannoq.tools.cluster.services.ServiceManager
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
class S3FileReceiverImplTestIT : S3TestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var fileReceiver: S3FileReceiverImpl

    @BeforeEach
    fun setupFileReceiver(testInfo: TestInfo) {
        fileReceiver = S3FileReceiverImpl(contextObjects["${testInfo.testMethod.get().name}-config"] as JsonObject)
    }

    @Test
    fun testInitializeCreate(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        vertx.deployVerticle(fileReceiver) {
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver, Handler {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    service.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        val uploadUrl = it.result().body.getString("uploadUrl")

                        uploadFile(vertx, uploadUrl, imageFile.absolutePath, Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                            }

                            context.completeNow()
                        })
                    })
                })
            })
        }
    }

    @Test
    fun testInitializeDeleteWithReceipt(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        vertx.deployVerticle(fileReceiver) {
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver, Handler {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    service.fileReceiverInitializeCreate(buildUploadEvent(), Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val uploadUrl = result.result().body.getString("uploadUrl")

                        uploadFile(vertx, uploadUrl, imageFile.absolutePath, Handler {
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
    fun testFetchSubscriptionAddress(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        vertx.deployVerticle(fileReceiver) {
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver, Handler {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()
                    val address = "${S3FileReceiverImpl::class.java.name}.data"

                    service.fetchSubscriptionAddress(Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result().body.getString("address")).isEqualTo(address)

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }
}