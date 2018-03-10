package org.mikand.autonomous.services.storage.receivers

import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.utils.DynamoDBTestClass

@Ignore
@RunWith(VertxUnitRunner::class)
class DynamoDBReceiverTestIT : DynamoDBTestClass() {
    @Before
    fun before(context: TestContext) {
    }

    @After
    fun after(context: TestContext) {
    }

    @Test
    fun receiverCreate(context: TestContext) {
    }

    @Test
    fun receiverCreateWithReceipt(context: TestContext) {
    }

    @Test
    fun receiverUpdate(context: TestContext) {
    }

    @Test
    fun receiverUpdateWithReceipt(context: TestContext) {
    }

    @Test
    fun receiverRead(context: TestContext) {
    }

    @Test
    fun receiverIndex(context: TestContext) {
    }

    @Test
    fun receiverIndexWithQuery(context: TestContext) {
    }

    @Test
    fun receiverDelete(context: TestContext) {
    }

    @Test
    fun receiverDeleteWithReceipt(context: TestContext) {
    }

    @Test
    fun fetchSubscriptionAddress(context: TestContext) {
    }
}