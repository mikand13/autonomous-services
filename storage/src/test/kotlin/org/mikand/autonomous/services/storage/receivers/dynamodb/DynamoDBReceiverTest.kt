package org.mikand.autonomous.services.storage.receivers.dynamodb

import com.nannoq.tools.repository.utils.FilterParameter
import com.nannoq.tools.repository.utils.GenericItemList
import com.nannoq.tools.repository.utils.QueryPack
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType.COMMAND
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent
import org.mikand.autonomous.services.storage.receivers.Receiver
import org.mikand.autonomous.services.storage.utils.DynamoDBTestClass
import java.util.*

@RunWith(VertxUnitRunner::class)
class DynamoDBReceiverTest : DynamoDBTestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun receiverCreate(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        repo.fetchSubscriptionAddress(Handler {
            context.assertTrue(it.succeeded())

            val address = it.result().body.statusObject.getString("address")

            rule.vertx().eventBus().consumer<JsonObject>(address) {
                val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                if (statusCode == 201) {
                    context.assertNotNull(it.body().getJsonObject("body").getJsonObject("statusObject"))
                    async.complete()
                }
            }

            repo.receiverCreate(ReceiveInputEvent(COMMAND.name, "RECEIVE_CREATE", TestModel().toJson()))
        })
    }

    @Test
    fun receiverCreateWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        val receiveEvent = ReceiveInputEvent(COMMAND.name, "RECEIVE_CREATE", TestModel().toJson())

        repo.receiverCreateWithReceipt(receiveEvent, Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(201, it.result().body.statusCode)
            context.assertNotNull(it.result().body.statusObject)
            async.complete()
        })
    }

    @Test
    fun receiverUpdate(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body.statusObject)
            testModel.setSomeBoolean(someBoolean = true)

            context.assertTrue(testModel.getSomeBoolean()!!)

            repo.fetchSubscriptionAddress(Handler {
                context.assertTrue(it.succeeded())

                val address = it.result().body.statusObject.getString("address")

                rule.vertx().eventBus().consumer<JsonObject>(address) {
                    val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                    if (statusCode == 202) {
                        val updatedModel = TestModel(it.body().getJsonObject("body").getJsonObject("statusObject"))

                        context.assertNotNull(updatedModel)
                        context.assertTrue(updatedModel.getSomeBoolean()!!)

                        val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                                .put("hash", updatedModel.hash)
                                .put("range", updatedModel.range))

                        repo.receiverRead(idObject, Handler {
                            context.assertTrue(it.succeeded())
                            context.assertTrue(TestModel(it.result().body.statusObject).getSomeBoolean()!!)
                            async.complete()
                        })
                    }
                }

                repo.receiverUpdate(ReceiveInputEvent(
                        COMMAND.name, "RECEIVE_UPDATE", testModel.toJson()))
            })
        })
    }

    @Test
    fun receiverUpdateWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body.statusObject)
            testModel.setSomeBoolean(someBoolean = true)
            val updateEvent = ReceiveInputEvent(COMMAND.name, "RECEIVE_UPDATE", testModel.toJsonFormat())

            repo.receiverUpdateWithReceipt(updateEvent, Handler {
                context.assertEquals(202, it.result().body.statusCode)

                val updatedModel = TestModel(it.result().body.statusObject)

                context.assertNotNull(updatedModel)
                context.assertTrue(updatedModel.getSomeBoolean()!!)

                val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                        .put("hash", updatedModel.hash)
                        .put("range", updatedModel.range))

                repo.receiverRead(idObject, Handler {
                    context.assertTrue(it.succeeded())
                    context.assertTrue(TestModel(it.result().body.statusObject).getSomeBoolean()!!)
                    async.complete()
                })
            })
        })
    }

    @Test
    fun receiverRead(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body.statusObject)
            val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                    .put("hash", testModel.hash)
                    .put("range", testModel.range))

            repo.receiverRead(idObject, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val record = TestModel(it.result().body.statusObject)

                context.assertNotNull(record)

                async.complete()
            })
        })
    }

    @Test
    fun receiverIndex(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createXItems(repo, 20, Handler {
            context.assertTrue(it.succeeded())

            val testModel = it.result()[0]
            val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                    .put("hash", testModel.hash))

            repo.receiverIndex(idObject, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.statusObject.encode(), GenericItemList::class.java)

                context.assertNotNull(list)
                context.assertEquals(20, list.count)
                context.assertEquals(20, list.items.size)
                context.assertNotNull(list.pageToken)

                async.complete()
            })
        })
    }

    @Test
    fun receiverIndexWithQuery(context: TestContext) {
        val async = context.async()
        val asyncTwo = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createXItems(repo, 20, Handler {
            context.assertTrue(it.succeeded())

            val testModel = it.result()[0]
            val queryPack = QueryPack.builder()
                    .withCustomRoute("testRoute")
                    .withProjections(arrayOf())
                    .withFilterParameters(mapOf(Pair("someBoolean",
                            Collections.singletonList(FilterParameter.builder()
                                    .withEq(false)
                                    .withField("someBoolean")
                                    .build()))))
                    .build()

            val indexEvent = ReceiveInputEvent(type = COMMAND.name, action = "RECEIVE_INDEX",
                    body = JsonObject().put("hash", testModel.hash),
                    metaData = JsonObject().put("query", JsonObject(Json.encode(queryPack))))

            repo.receiverIndex(indexEvent, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.statusObject.encode(), GenericItemList::class.java)

                context.assertNotNull(list)
                context.assertEquals(20, list.count)
                context.assertEquals(20, list.items.size)
                context.assertNotNull(list.pageToken)

                async.complete()
            })

            val queryPackTrue = QueryPack.builder()
                    .withCustomRoute("testRouteTrue")
                    .withProjections(arrayOf())
                    .withFilterParameters(mapOf(Pair("someBoolean",
                            Collections.singletonList(FilterParameter.builder()
                                    .withEq(true)
                                    .withField("someBoolean")
                                    .build()))))
                    .build()

            val indexEventTrue = ReceiveInputEvent(type = COMMAND.name, action = "RECEIVE_INDEX",
                    body = JsonObject().put("hash", testModel.hash),
                    metaData = JsonObject().put("query", JsonObject(Json.encode(queryPackTrue))))

            repo.receiverIndex(indexEventTrue, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.statusObject.encode(), GenericItemList::class.java)

                context.assertNotNull(list)
                context.assertEquals(0, list.count)
                context.assertEquals(0, list.items.size)
                context.assertNotNull(list.pageToken)

                asyncTwo.complete()
            })
        })
    }

    @Test
    fun receiverDelete(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            repo.fetchSubscriptionAddress(Handler {
                context.assertTrue(it.succeeded())

                val address = it.result().body.statusObject.getString("address")

                rule.vertx().eventBus().consumer<JsonObject>(address) {
                    val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                    if (statusCode == 204) {
                        context.assertEquals(204, it.body().getJsonObject("body").getInteger("statusCode"))

                        val updatedModel = TestModel(it.body().getJsonObject("body").getJsonObject("statusObject"))

                        context.assertNotNull(updatedModel)

                        val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                                .put("hash", updatedModel.hash)
                                .put("range", updatedModel.range))

                        repo.receiverRead(idObject, Handler {
                            context.assertTrue(it.failed())
                            async.complete()
                        })
                    }
                }

                val deleteEvent = ReceiveInputEvent(COMMAND.name, "RECEIVE_DELETE", TestModel().toJsonFormat())

                repo.receiverDelete(deleteEvent)
            })
        })
    }

    @Test
    fun receiverDeleteWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body.statusObject)
            val deleteEvent = ReceiveInputEvent(COMMAND.name, "RECEIVE_DELETE", testModel.toJsonFormat())

            repo.receiverDeleteWithReceipt(deleteEvent, Handler {
                context.assertEquals(204, it.result().body.statusCode)

                val updatedModel = TestModel(it.result().body.statusObject)

                context.assertNotNull(updatedModel)

                val idObject = ReceiveInputEvent(COMMAND.name, "RECEIVE_READ", JsonObject()
                        .put("hash", updatedModel.hash)
                        .put("range", updatedModel.range))

                repo.receiverRead(idObject, Handler {
                    context.assertTrue(it.failed())
                    async.complete()
                })
            })
        })
    }

    @Test
    fun fetchSubscriptionAddress(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val address = "${Receiver::class.java.name}.${TestModel::class.java.simpleName}"

        repo.fetchSubscriptionAddress(Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(address, it.result().body.statusObject.getString("address"))
            async.complete()
        })
    }
}