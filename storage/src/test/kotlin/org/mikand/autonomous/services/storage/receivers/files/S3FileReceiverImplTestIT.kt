package org.mikand.autonomous.services.storage.receivers.files

import com.nannoq.tools.cluster.services.ServiceManager
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent
import org.mikand.autonomous.services.storage.utils.S3TestClass

@RunWith(VertxUnitRunner::class)
class S3FileReceiverImplTestIT : S3TestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var fileReceiver: S3FileReceiverImpl

    @Before
    fun setupFileReceiver(context: TestContext) {
        fileReceiver = S3FileReceiverImpl(context.get<JsonObject>("${name.methodName}-config"))
    }

    @Test
    fun testInitializeCreate(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(fileReceiver, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver) {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    service.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                        context.assertTrue(it.succeeded())

                        val uploadUrl = it.result().body.statusObject.getString("uploadUrl")

                        uploadFile(uploadUrl, imageFile.absolutePath, Handler {
                            if (it.succeeded()) {
                                async.complete()
                            } else {
                                context.fail(it.cause())
                            }
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun testInitializeDeleteWithReceipt(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(fileReceiver, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver) {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    service.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                        context.assertTrue(it.succeeded())

                        val uploadUrl = it.result().body.statusObject.getString("uploadUrl")

                        uploadFile(uploadUrl, imageFile.absolutePath, Handler {
                            if (it.succeeded()) {
                                fileReceiver.fileReceiverDeleteWithReceipt(buildDeletwEvent(it.result()), Handler {
                                    context.assertTrue(it.succeeded())
                                    async.complete()
                                })
                            } else {
                                context.fail(it.cause())
                            }
                        })
                    })
                }
            }
        }))
    }

    private fun buildUploadEvent(): ReceiveInputEvent {
        return ReceiveInputEvent(ReceiveEventType.COMMAND.name, "FILE_UPLOAD", JsonObject())
    }

    private fun buildDeletwEvent(key: String): ReceiveInputEvent {
        return ReceiveInputEvent(ReceiveEventType.COMMAND.name, "FILE_DELETE", JsonObject().put("key", key))
    }

    @Test
    fun testFetchSubscriptionAddress(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(fileReceiver, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(FileReceiver::class.java, fileReceiver) {
                ServiceManager.getInstance().consumeService(FileReceiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()
                    val address = S3FileReceiverImpl::class.java.name

                    service.fetchSubscriptionAddress(Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(address, it.result().body.statusObject.getString("address"))
                        async.complete()
                    })
                }
            }
        }))
    }
}