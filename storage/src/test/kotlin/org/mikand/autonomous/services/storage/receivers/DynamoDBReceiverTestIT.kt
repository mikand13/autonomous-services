package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.cluster.services.ServiceManager
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
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.utils.DynamoDBTestClass
import java.util.*

@RunWith(VertxUnitRunner::class)
class DynamoDBReceiverTestIT : DynamoDBTestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun receiverCreate(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    service.fetchSubscriptionAddress(Handler {
                        context.assertTrue(it.succeeded())

                        val address = it.result().body.statusObject.getString("address")

                        rule.vertx().eventBus().consumer<JsonObject>(address) {
                            val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                            if (statusCode == 201) {
                                context.assertNotNull(it.body().getJsonObject("body").getJsonObject("statusObject"))
                                async.complete()
                            }
                        }


                        service.receiverCreate(ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_CREATE",
                                ReceiveStatus(201, statusObject = TestModel().toJson())))
                    })
                }
            }
        }))
    }

    @Test
    fun receiverCreateWithReceipt(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()
                    val receiveEvent = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_CREATE",
                            ReceiveStatus(201, statusObject = TestModel().toJson()))

                    service.receiverCreateWithReceipt(receiveEvent, Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(201, it.result().body.statusCode)
                        context.assertNotNull(it.result().body.statusObject)
                        async.complete()
                    })
                }
            }
        }))
    }

    @Test
    fun receiverUpdate(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createItem(verticle, Handler {
                        context.assertTrue(it.succeeded())

                        val testModel = TestModel(it.result().body.statusObject)
                        testModel.setSomeBoolean(someBoolean = true)

                        context.assertTrue(testModel.getSomeBoolean()!!)

                        service.fetchSubscriptionAddress(Handler {
                            context.assertTrue(it.succeeded())

                            val address = it.result().body.statusObject.getString("address")

                            rule.vertx().eventBus().consumer<JsonObject>(address) {
                                val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                                if (statusCode == 202) {
                                    val updatedModel = TestModel(it.body().getJsonObject("body")
                                            .getJsonObject("statusObject"))

                                    context.assertNotNull(updatedModel)
                                    context.assertTrue(updatedModel.getSomeBoolean()!!)

                                    val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                            ReceiveStatus(200, statusObject = JsonObject()
                                                    .put("hash", updatedModel.hash)
                                                    .put("range", updatedModel.range)))

                                    service.receiverRead(idObject, Handler {
                                        context.assertTrue(it.succeeded())
                                        context.assertTrue(TestModel(it.result().body.statusObject).getSomeBoolean()!!)
                                        async.complete()
                                    })
                                }
                            }

                            service.receiverUpdate(ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_UPDATE",
                                    ReceiveStatus(202, statusObject = testModel.toJsonFormat())))
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun receiverUpdateWithReceipt(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createItem(verticle, Handler {
                        context.assertTrue(it.succeeded())

                        val testModel = TestModel(it.result().body.statusObject)
                        testModel.setSomeBoolean(someBoolean = true)
                        val updateEvent = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_UPDATE",
                                ReceiveStatus(202, statusObject = testModel.toJsonFormat()))

                        service.receiverUpdateWithReceipt(updateEvent, Handler {
                            context.assertEquals(202, it.result().body.statusCode)

                            val updatedModel = TestModel(it.result().body.statusObject)

                            context.assertNotNull(updatedModel)
                            context.assertTrue(updatedModel.getSomeBoolean()!!)

                            val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                    ReceiveStatus(200, statusObject = JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range)))

                            service.receiverRead(idObject, Handler {
                                context.assertTrue(it.succeeded())
                                context.assertTrue(TestModel(it.result().body.statusObject).getSomeBoolean()!!)
                                async.complete()
                            })
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun receiverRead(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createItem(verticle, Handler {
                        context.assertTrue(it.succeeded())

                        val testModel = TestModel(it.result().body.statusObject)
                        val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                ReceiveStatus(200, statusObject = JsonObject()
                                        .put("hash", testModel.hash)
                                        .put("range", testModel.range)))

                        service.receiverRead(idObject, Handler {
                            context.assertTrue(it.succeeded())
                            context.assertNotNull(it.result())

                            val record = TestModel(it.result().body.statusObject)

                            context.assertNotNull(record)

                            async.complete()
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun receiverIndex(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createXItems(verticle, 20, Handler {
                        context.assertTrue(it.succeeded())

                        val testModel = it.result()[0]
                        val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                ReceiveStatus(200, statusObject = JsonObject()
                                        .put("hash", testModel.hash)))

                        service.receiverIndex(idObject, Handler {
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
            }
        }))
    }

    @Test
    fun receiverIndexWithQuery(context: TestContext) {
        val async = context.async()
        val asyncTwo = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createXItems(verticle, 20, Handler {
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

                        val indexEvent = ReceiveEvent(type = ReceiveEventType.COMMAND.name, action = "RECEIVE_INDEX",
                                body = ReceiveStatus(200, statusObject = JsonObject()
                                        .put("hash", testModel.hash)),
                                metaData = JsonObject().put("query", JsonObject(Json.encode(queryPack))))

                        service.receiverIndex(indexEvent, Handler {
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

                        val indexEventTrue = ReceiveEvent(type = ReceiveEventType.COMMAND.name, action = "RECEIVE_INDEX",
                                body = ReceiveStatus(200, statusObject = JsonObject()
                                        .put("hash", testModel.hash)),
                                metaData = JsonObject().put("query", JsonObject(Json.encode(queryPackTrue))))

                        service.receiverIndex(indexEventTrue, Handler {
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
            }
        }))

    }

    @Test
    fun receiverDelete(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createItem(verticle, Handler {
                        context.assertTrue(it.succeeded())

                        service.fetchSubscriptionAddress(Handler {
                            context.assertTrue(it.succeeded())

                            val address = it.result().body.statusObject.getString("address")

                            rule.vertx().eventBus().consumer<JsonObject>(address) {
                                val statusCode = it.body().getJsonObject("body").getInteger("statusCode")

                                if (statusCode == 204) {
                                    context.assertEquals(204, it.body().getJsonObject("body")
                                            .getInteger("statusCode"))

                                    val updatedModel = TestModel(it.body().getJsonObject("body")
                                            .getJsonObject("statusObject"))

                                    context.assertNotNull(updatedModel)

                                    val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                            ReceiveStatus(200, statusObject = JsonObject()
                                                    .put("hash", updatedModel.hash)
                                                    .put("range", updatedModel.range)))

                                    service.receiverRead(idObject, Handler {
                                        context.assertTrue(it.failed())
                                        async.complete()
                                    })
                                }
                            }

                            service.receiverDelete(ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_DELETE",
                                    ReceiveStatus(204, statusObject = TestModel().toJson())))
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun receiverDeleteWithReceipt(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()

                    createItem(verticle, Handler {
                        context.assertTrue(it.succeeded())

                        val testModel = TestModel(it.result().body.statusObject)
                        val deleteEvent = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_DELETE",
                                ReceiveStatus(204, statusObject = testModel.toJsonFormat()))

                        service.receiverDeleteWithReceipt(deleteEvent, Handler {
                            context.assertEquals(204, it.result().body.statusCode)

                            val updatedModel = TestModel(it.result().body.statusObject)

                            context.assertNotNull(updatedModel)

                            val idObject = ReceiveEvent(ReceiveEventType.COMMAND.name, "RECEIVE_READ",
                                    ReceiveStatus(200, statusObject = JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range)))

                            service.receiverRead(idObject, Handler {
                                context.assertTrue(it.failed())
                                async.complete()
                            })
                        })
                    })
                }
            }
        }))
    }

    @Test
    fun fetchSubscriptionAddress(context: TestContext) {
        val async = context.async()
        val verticle: TestModelReceiverImpl = context.get("${name.methodName}-repo")
        val vertx = rule.vertx()

        vertx.deployVerticle(verticle, context.asyncAssertSuccess({
            ServiceManager.getInstance().publishService(Receiver::class.java, verticle) {
                ServiceManager.getInstance().consumeService(Receiver::class.java) {
                    context.assertTrue(it.succeeded())

                    val service = it.result()
                    val address = "${Receiver::class.java.name}.${TestModel::class.java.simpleName}"

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