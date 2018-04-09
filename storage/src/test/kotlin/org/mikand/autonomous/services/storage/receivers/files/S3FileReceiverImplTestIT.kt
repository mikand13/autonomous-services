package org.mikand.autonomous.services.storage.receivers.files

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.utils.S3TestClass

@RunWith(VertxUnitRunner::class)
class S3FileReceiverImplTestIT : S3TestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var fileReceiver: S3FileReceiverImpl

    @Before
    fun setupFileReceiver(context: TestContext) {
        fileReceiver = S3FileReceiverImpl(getTestConfig())
    }

    @Test
    fun testInitializeCreate(context: TestContext) {

    }

    @Test
    fun testInitializeDelete(context: TestContext) {

    }

    @Test
    fun testInitializeDeleteWithReceipt(context: TestContext) {

    }

    @Test
    fun testFetchSubscriptionAddress(context: TestContext) {

    }
}