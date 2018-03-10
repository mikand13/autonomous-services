package org.mikand.autonomous.services.storage.receivers

import com.nannoq.tools.cluster.services.ServiceManager
import com.nannoq.tools.repository.utils.FilterParameter
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

                        rule.vertx().eventBus().consumer<JsonObject>(it.result()) {
                            val statusCode = it.body().getInteger("statusCode")

                            if (statusCode == 201) {
                                context.assertNotNull(it.body().getJsonObject("statusObject"))
                                async.complete()
                            }
                        }

                        service.receiverCreate(TestModel().toJson())
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

                    service.receiverCreateWithReceipt(TestModel().toJson(), Handler {
                        context.assertTrue(it.succeeded())
                        context.assertEquals(201, it.result().statusCode)
                        context.assertNotNull(it.result().statusObject)
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

                        val testModel = TestModel(it.result().statusObject)
                        testModel.setSomeBoolean(someBoolean = true)

                        context.assertTrue(testModel.getSomeBoolean()!!)

                        service.fetchSubscriptionAddress(Handler {
                            context.assertTrue(it.succeeded())

                            rule.vertx().eventBus().consumer<JsonObject>(it.result()) {
                                val statusCode = it.body().getInteger("statusCode")

                                if (statusCode == 202) {
                                    val updatedModel = TestModel(it.body().getJsonObject("statusObject"))

                                    context.assertNotNull(updatedModel)
                                    context.assertTrue(updatedModel.getSomeBoolean()!!)

                                    val idObject = JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range)

                                    service.receiverRead(idObject, Handler {
                                        context.assertTrue(it.succeeded())
                                        context.assertTrue(TestModel(it.result()).getSomeBoolean()!!)
                                        async.complete()
                                    })
                                }
                            }

                            service.receiverUpdate(testModel.toJsonFormat())
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

                        val testModel = TestModel(it.result().statusObject)
                        testModel.setSomeBoolean(someBoolean = true)

                        service.receiverUpdateWithReceipt(testModel.toJsonFormat(), Handler {
                            context.assertEquals(202, it.result().statusCode)

                            val updatedModel = TestModel(it.result().statusObject)

                            context.assertNotNull(updatedModel)
                            context.assertTrue(updatedModel.getSomeBoolean()!!)

                            val idObject = JsonObject()
                                    .put("hash", updatedModel.hash)
                                    .put("range", updatedModel.range)

                            service.receiverRead(idObject, Handler {
                                context.assertTrue(it.succeeded())
                                context.assertTrue(TestModel(it.result()).getSomeBoolean()!!)
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

                        val testModel = TestModel(it.result().statusObject)
                        val idObject = JsonObject()
                                .put("hash", testModel.hash)
                                .put("range", testModel.range)

                        service.receiverRead(idObject, Handler {
                            context.assertTrue(it.succeeded())
                            context.assertNotNull(it.result())

                            val record = TestModel(it.result())

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
                        val idObject = JsonObject()
                                .put("hash", testModel.hash)

                        service.receiverIndex(idObject, Handler {
                            context.assertTrue(it.succeeded())
                            context.assertNotNull(it.result())

                            val list = it.result()

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
                        val idObject = JsonObject()
                                .put("hash", testModel.hash)
                        val queryPack = QueryPack.builder()
                                .withCustomRoute("testRoute")
                                .withProjections(arrayOf())
                                .withFilterParameters(mapOf(Pair("someBoolean", Collections.singletonList(FilterParameter.builder()
                                        .withEq(false)
                                        .withField("someBoolean")
                                        .build()))))
                                .build()

                        service.receiverIndexWithQuery(idObject, JsonObject(Json.encode(queryPack)), Handler {
                            context.assertTrue(it.succeeded())
                            context.assertNotNull(it.result())

                            val list = it.result()

                            context.assertNotNull(list)
                            context.assertEquals(20, list.count)
                            context.assertEquals(20, list.items.size)
                            context.assertNotNull(list.pageToken)

                            async.complete()
                        })

                        val queryPackTrue = QueryPack.builder()
                                .withCustomRoute("testRouteTrue")
                                .withProjections(arrayOf())
                                .withFilterParameters(mapOf(Pair("someBoolean", Collections.singletonList(FilterParameter.builder()
                                        .withEq(true)
                                        .withField("someBoolean")
                                        .build()))))
                                .build()

                        service.receiverIndexWithQuery(idObject, JsonObject(Json.encode(queryPackTrue)), Handler {
                            context.assertTrue(it.succeeded())
                            context.assertNotNull(it.result())

                            val list = it.result()

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

                            rule.vertx().eventBus().consumer<JsonObject>(it.result()) {
                                val statusCode = it.body().getInteger("statusCode")

                                if (statusCode == 204) {
                                    context.assertEquals(204, it.body().getInteger("statusCode"))

                                    val updatedModel = TestModel(it.body().getJsonObject("statusObject"))

                                    context.assertNotNull(updatedModel)

                                    val idObject = JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range)

                                    service.receiverRead(idObject, Handler {
                                        context.assertTrue(it.failed())
                                        async.complete()
                                    })
                                }
                            }

                            service.receiverDelete(TestModel().toJson())
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

                        val testModel = TestModel(it.result().statusObject)

                        service.receiverDeleteWithReceipt(testModel.toJsonFormat(), Handler {
                            context.assertEquals(204, it.result().statusCode)

                            val updatedModel = TestModel(it.result().statusObject)

                            context.assertNotNull(updatedModel)

                            val idObject = JsonObject()
                                    .put("hash", updatedModel.hash)
                                    .put("range", updatedModel.range)

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
                        context.assertEquals(address, it.result(), it.result())
                        async.complete()
                    })
                }
            }
        }))
    }
}