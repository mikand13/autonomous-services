package org.mikand.autonomous.services.storage.receivers.files

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
class S3FileReceiverImplTest : S3TestClass() {
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

        vertx.deployVerticle(fileReceiver, {
            if (it.failed()) logger.error("Failure!", it.cause())

            context.assertTrue(it.succeeded())

            fileReceiver.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                context.assertTrue(it.succeeded())

                uploadFile(it.result().body.statusObject.getString("uploadUrl"), imageFile.absolutePath, Handler {
                    if (it.succeeded()) {
                        async.complete()
                    } else {
                        context.fail(it.cause())
                    }
                })
            })
        })
    }

    @Test
    fun testDeleteWithReceipt(context: TestContext) {
        val async = context.async()
        val vertx = rule.vertx()

        vertx.deployVerticle(fileReceiver, {
            if (it.failed()) logger.error("Failure!", it.cause())

            context.assertTrue(it.succeeded())

            fileReceiver.fileReceiverInitializeCreate(buildUploadEvent(), Handler {
                context.assertTrue(it.succeeded())

                uploadFile(it.result().body.statusObject.getString("uploadUrl"), imageFile.absolutePath, Handler {
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
        })
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
        val address = S3FileReceiverImpl::class.java.name

        vertx.deployVerticle(fileReceiver, {
            if (it.failed()) logger.error("Failure!", it.cause())

            context.assertTrue(it.succeeded())

            fileReceiver.fetchSubscriptionAddress(Handler {
                context.assertTrue(it.succeeded())
                context.assertEquals(address, it.result().body.statusObject.getString("address"))
                async.complete()
            })
        })
    }
}