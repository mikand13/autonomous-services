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
import org.junit.Test
import org.junit.runner.RunWith
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
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

            val address = it.result().body.getString("address")

            rule.vertx().eventBus().consumer<JsonObject>(address) {
                val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                if (statusCode == 201) {
                    context.assertNotNull(it.body().getJsonObject("body"))
                    async.complete()
                }
            }

            repo.receiverCreate(CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_CREATE")
                    .withBody(TestModel().toJson())
                    .build())
        })
    }

    @Test
    fun receiverCreateWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        val receiveEvent = CommandEventBuilder()
                .withSuccess()
                .withAction("RECEIVE_CREATE")
                .withBody(TestModel().toJson())
                .build()

        repo.receiverCreateWithReceipt(receiveEvent, Handler {
            context.assertTrue(it.succeeded())
            context.assertEquals(201, it.result().metadata.getInteger("statusCode"))
            context.assertNotNull(it.result().body)
            async.complete()
        })
    }

    @Test
    fun receiverUpdate(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body)
            testModel.setSomeBoolean(someBoolean = true)

            context.assertTrue(testModel.getSomeBoolean()!!)

            repo.fetchSubscriptionAddress(Handler {
                context.assertTrue(it.succeeded())

                val address = it.result().body.getString("address")

                rule.vertx().eventBus().consumer<JsonObject>(address) {
                    val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                    if (statusCode == 202) {
                        val updatedModel = TestModel(it.body().getJsonObject("body"))

                        context.assertNotNull(updatedModel)
                        context.assertTrue(updatedModel.getSomeBoolean()!!)

                        val idObject = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_READ")
                                .withBody(JsonObject()
                                        .put("hash", updatedModel.hash)
                                        .put("range", updatedModel.range))
                                .build()

                        repo.receiverRead(idObject, Handler {
                            context.assertTrue(it.succeeded())
                            context.assertTrue(TestModel(it.result().body).getSomeBoolean()!!)
                            async.complete()
                        })
                    }
                }

                repo.receiverUpdate(CommandEventBuilder()
                        .withSuccess()
                        .withAction("RECEIVE_UPDATE")
                        .withBody(testModel.toJson())
                        .build())
            })
        })
    }

    @Test
    fun receiverUpdateWithReceipt(context: TestContext) {
        val async = context.async()
        val repo: TestModelReceiverImpl = context.get("${name.methodName}-repo")

        createItem(repo, Handler {
            context.assertTrue(it.succeeded())

            val testModel = TestModel(it.result().body)
            testModel.setSomeBoolean(someBoolean = true)
            val updateEvent = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_UPDATE")
                    .withBody(testModel.toJson())
                    .build()

            repo.receiverUpdateWithReceipt(updateEvent, Handler {
                context.assertEquals(202, it.result().metadata.getInteger("statusCode"))

                val updatedModel = TestModel(it.result().body)

                context.assertNotNull(updatedModel)
                context.assertTrue(updatedModel.getSomeBoolean()!!)

                val idObject = CommandEventBuilder()
                        .withSuccess()
                        .withAction("RECEIVE_READ")
                        .withBody(JsonObject()
                                .put("hash", updatedModel.hash)
                                .put("range", updatedModel.range))
                        .build()

                repo.receiverRead(idObject, Handler {
                    context.assertTrue(it.succeeded())
                    context.assertTrue(TestModel(it.result().body).getSomeBoolean()!!)
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

            val testModel = TestModel(it.result().body)
            val idObject = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_READ")
                    .withBody(JsonObject()
                            .put("hash", testModel.hash)
                            .put("range", testModel.range))
                    .build()

            repo.receiverRead(idObject, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val record = TestModel(it.result().body)

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
            val idObject = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_READ")
                    .withBody(JsonObject()
                            .put("hash", testModel.hash)
                            .put("range", testModel.range))
                    .build()

            repo.receiverIndex(idObject, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.encode(), GenericItemList::class.java)

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

            val indexEvent = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_INDEX")
                    .withBody(JsonObject()
                            .put("hash", testModel.hash))
                    .withMetadata(JsonObject().put("query", JsonObject(Json.encode(queryPack))))
                    .build()

            repo.receiverIndex(indexEvent, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.encode(), GenericItemList::class.java)

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

            val indexEventTrue = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_INDEX")
                    .withBody(JsonObject()
                            .put("hash", testModel.hash))
                    .withMetadata(JsonObject().put("query", JsonObject(Json.encode(queryPackTrue))))
                    .build()

            repo.receiverIndex(indexEventTrue, Handler {
                context.assertTrue(it.succeeded())
                context.assertNotNull(it.result())

                val list = Json.decodeValue(
                        it.result().body.encode(), GenericItemList::class.java)

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

                val address = it.result().body.getString("address")

                rule.vertx().eventBus().consumer<JsonObject>(address) {
                    val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                    if (statusCode == 204) {
                        context.assertEquals(204, it.body().getJsonObject("metadata").getInteger("statusCode"))

                        val updatedModel = TestModel(it.body().getJsonObject("body"))

                        context.assertNotNull(updatedModel)

                        val idObject = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_READ")
                                .withBody(JsonObject()
                                        .put("hash", updatedModel.hash)
                                        .put("range", updatedModel.range))
                                .build()

                        repo.receiverRead(idObject, Handler {
                            context.assertTrue(it.failed())
                            async.complete()
                        })
                    }
                }

                val deleteEvent = CommandEventBuilder()
                        .withSuccess()
                        .withAction("RECEIVE_DELETE")
                        .withBody(TestModel().toJsonFormat())
                        .build()

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

            val testModel = TestModel(it.result().body)
            val deleteEvent = CommandEventBuilder()
                    .withSuccess()
                    .withAction("RECEIVE_DELETE")
                    .withBody(testModel.toJsonFormat())
                    .build()

            repo.receiverDeleteWithReceipt(deleteEvent, Handler {
                context.assertEquals(204, it.result().metadata.getInteger("statusCode"))

                val updatedModel = TestModel(it.result().body)

                context.assertNotNull(updatedModel)

                val idObject = CommandEventBuilder()
                        .withSuccess()
                        .withAction("RECEIVE_READ")
                        .withBody(JsonObject()
                                .put("hash", updatedModel.hash)
                                .put("range", updatedModel.range))
                        .build()

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
            context.assertEquals(address, it.result().body.getString("address"))
            async.complete()
        })
    }
}