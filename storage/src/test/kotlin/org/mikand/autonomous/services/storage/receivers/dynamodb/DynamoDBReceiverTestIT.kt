package org.mikand.autonomous.services.storage.receivers.dynamodb

import com.nannoq.tools.cluster.services.ServiceManager
import com.nannoq.tools.repository.utils.FilterParameter
import com.nannoq.tools.repository.utils.GenericItemList
import com.nannoq.tools.repository.utils.QueryPack
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import java.util.Collections
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.storage.gen.TestModelReceiverImpl
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.receivers.Receiver
import org.mikand.autonomous.services.storage.utils.DynamoDBTestClass

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(VertxExtension::class)
class DynamoDBReceiverTestIT : DynamoDBTestClass() {
    @Suppress("unused")
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Test
    fun receiverCreate(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    service.fetchSubscriptionAddress(Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val address = result.result().body.getString("address")

                        vertx.eventBus().consumer<JsonObject>(address) {
                            val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                            if (statusCode == 201) {
                                context.verify {
                                    assertThat(it.body().getJsonObject("body")).isNotNull

                                    context.completeNow()
                                }
                            }
                        }

                        service.receiverCreate(CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_CREATE")
                                .withBody(TestModel().toJson())
                                .build())
                    })
                })
            })
        }
    }

    @Test
    fun receiverCreateWithReceipt(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { result ->
                    context.verify {
                        assertThat(result.succeeded()).isTrue()
                    }

                    val service = result.result()
                    val receiveEvent = CommandEventBuilder()
                            .withSuccess()
                            .withAction("RECEIVE_CREATE")
                            .withBody(TestModel().toJson())
                            .build()

                    service.receiverCreateWithReceipt(receiveEvent, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                            assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(201)
                            assertThat(it.result().body).isNotNull

                            context.completeNow()
                        }
                    })
                })
            })
        }
    }

    @Test
    fun receiverUpdate(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createItem(verticle, Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val testModel = TestModel(result.result().body)
                        testModel.setSomeBoolean(someBoolean = true)

                        context.verify {
                            assertThat(testModel.getSomeBoolean()!!).isTrue()
                        }

                        service.fetchSubscriptionAddress(Handler { it ->
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                            }

                            val address = it.result().body.getString("address")

                            vertx.eventBus().consumer<JsonObject>(address) {
                                val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                                if (statusCode == 202) {
                                    val updatedModel = TestModel(it.body().getJsonObject("body"))

                                    context.verify {
                                        assertThat(updatedModel).isNotNull
                                        assertThat(updatedModel.getSomeBoolean()!!).isTrue()
                                    }

                                    val idObject = CommandEventBuilder()
                                            .withSuccess()
                                            .withAction("RECEIVE_READ")
                                            .withBody(JsonObject()
                                                    .put("hash", updatedModel.hash)
                                                    .put("range", updatedModel.range))
                                            .build()

                                    service.receiverRead(idObject, Handler {
                                        context.verify {
                                            assertThat(it.succeeded()).isTrue()
                                            assertThat(TestModel(it.result().body).getSomeBoolean()!!).isTrue()

                                            context.completeNow()
                                        }
                                    })
                                }
                            }

                            service.receiverUpdate(CommandEventBuilder()
                                    .withSuccess()
                                    .withAction("RECEIVE_UPDATE")
                                    .withBody(testModel.toJson())
                                    .build())
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverUpdateWithReceipt(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createItem(verticle, Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val testModel = TestModel(result.result().body)

                        testModel.setSomeBoolean(someBoolean = true)

                        val updateEvent = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_UPDATE")
                                .withBody(testModel.toJson())
                                .build()

                        service.receiverUpdateWithReceipt(updateEvent, Handler {
                            context.verify {
                                assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(202)
                            }

                            val updatedModel = TestModel(it.result().body)

                            context.verify {
                                assertThat(updatedModel).isNotNull
                                assertThat(updatedModel.getSomeBoolean()!!).isTrue()
                            }

                            val idObject = CommandEventBuilder()
                                    .withSuccess()
                                    .withAction("RECEIVE_READ")
                                    .withBody(JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range))
                                    .build()

                            service.receiverRead(idObject, Handler {
                                context.verify {
                                    assertThat(it.succeeded()).isTrue()
                                    assertThat(TestModel(it.result().body).getSomeBoolean()!!).isTrue()

                                    context.completeNow()
                                }
                            })
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverRead(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { result ->
                    context.verify {
                        assertThat(result.succeeded()).isTrue()
                    }

                    val service = result.result()

                    createItem(verticle, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        val testModel = TestModel(it.result().body)
                        val idObject = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_READ")
                                .withBody(JsonObject()
                                        .put("hash", testModel.hash)
                                        .put("range", testModel.range))
                                .build()

                        service.receiverRead(idObject, Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                                assertThat(it.result()).isNotNull

                                val record = TestModel(it.result().body)

                                assertThat(record).isNotNull

                                context.completeNow()
                            }
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverIndex(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createXItems(verticle, 20, Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val testModel = result.result()[0]
                        val idObject = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_READ")
                                .withBody(JsonObject()
                                        .put("hash", testModel.hash))
                                .build()

                        service.receiverIndex(idObject, Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                                assertThat(it.result()).isNotNull
                            }

                            val list = Json.decodeValue(
                                    it.result().body.encode(), GenericItemList::class.java)

                            context.verify {
                                assertThat(list).isNotNull
                                assertThat(list.count).isEqualTo(20)
                                assertThat(list.items?.size).isEqualTo(20)
                                assertThat(list.pageToken).isNotNull

                                context.completeNow()
                            }
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverIndexWithQuery(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val checkpoint = context.checkpoint(2)
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) { outerResult ->
            context.verify {
                assertThat(outerResult.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createXItems(verticle, 20, Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val testModel = result.result()[0]
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

                        service.receiverIndex(indexEvent, Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                                assertThat(it.result()).isNotNull
                            }

                            val list = Json.decodeValue(
                                    it.result().body.encode(), GenericItemList::class.java)

                            context.verify {
                                assertThat(list).isNotNull
                                assertThat(list.count).isEqualTo(20)
                                assertThat(list.items?.size).isEqualTo(20)
                                assertThat(list.pageToken).isNotNull

                                checkpoint.flag()
                            }
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

                        service.receiverIndex(indexEventTrue, Handler {
                            context.verify {
                                assertThat(it.succeeded()).isTrue()
                                assertThat(it.result()).isNotNull
                            }

                            val list = Json.decodeValue(
                                    it.result().body.encode(), GenericItemList::class.java)

                            context.verify {
                                assertThat(list).isNotNull
                                assertThat(list.count).isEqualTo(0)
                                assertThat(list.items?.size).isEqualTo(0)
                                assertThat(list.pageToken).isNotNull

                                checkpoint.flag()
                            }
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverDelete(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createItem(verticle, Handler {
                        context.verify {
                            assertThat(it.succeeded()).isTrue()
                        }

                        service.fetchSubscriptionAddress(Handler { result ->
                            context.verify {
                                assertThat(result.succeeded()).isTrue()
                            }

                            val address = result.result().body.getString("address")

                            vertx.eventBus().consumer<JsonObject>(address) {
                                val statusCode = it.body().getJsonObject("metadata").getInteger("statusCode")

                                if (statusCode == 204) {
                                    context.verify {
                                        assertThat(it.body().getJsonObject("metadata").getInteger("statusCode")).isEqualTo(204)
                                    }

                                    val updatedModel = TestModel(it.body().getJsonObject("body"))

                                    context.verify {
                                        assertThat(updatedModel).isNotNull
                                    }

                                    val idObject = CommandEventBuilder()
                                            .withSuccess()
                                            .withAction("RECEIVE_READ")
                                            .withBody(JsonObject()
                                                    .put("hash", updatedModel.hash)
                                                    .put("range", updatedModel.range))
                                            .build()

                                    service.receiverRead(idObject, Handler {
                                        context.verify {
                                            assertThat(it.failed()).isTrue()

                                            context.completeNow()
                                        }
                                    })
                                }
                            }

                            val deleteEvent = CommandEventBuilder()
                                    .withSuccess()
                                    .withAction("RECEIVE_DELETE")
                                    .withBody(TestModel().toJsonFormat())
                                    .build()

                            service.receiverDelete(deleteEvent)
                        })
                    })
                })
            })
        }
    }

    @Test
    fun receiverDeleteWithReceipt(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler { asyncResult ->
                    context.verify {
                        assertThat(asyncResult.succeeded()).isTrue()
                    }

                    val service = asyncResult.result()

                    createItem(verticle, Handler { result ->
                        context.verify {
                            assertThat(result.succeeded()).isTrue()
                        }

                        val testModel = TestModel(result.result().body)
                        val deleteEvent = CommandEventBuilder()
                                .withSuccess()
                                .withAction("RECEIVE_DELETE")
                                .withBody(testModel.toJsonFormat())
                                .build()

                        service.receiverDeleteWithReceipt(deleteEvent, Handler {
                            context.verify {
                                assertThat(it.result().metadata.getInteger("statusCode")).isEqualTo(204)
                            }

                            val updatedModel = TestModel(it.result().body)

                            context.verify {
                                assertThat(updatedModel).isNotNull
                            }

                            val idObject = CommandEventBuilder()
                                    .withSuccess()
                                    .withAction("RECEIVE_READ")
                                    .withBody(JsonObject()
                                            .put("hash", updatedModel.hash)
                                            .put("range", updatedModel.range))
                                    .build()

                            service.receiverRead(idObject, Handler {
                                context.verify {
                                    assertThat(it.failed()).isTrue()

                                    context.completeNow()
                                }
                            })
                        })
                    })
                })
            })
        }
    }

    @Test
    fun fetchSubscriptionAddress(vertx: Vertx, testInfo: TestInfo, context: VertxTestContext) {
        val verticle: TestModelReceiverImpl = contextObjects["${testInfo.testMethod.get().name}-repo"] as TestModelReceiverImpl

        vertx.deployVerticle(verticle) {
            context.verify {
                assertThat(it.succeeded()).isTrue()
            }

            ServiceManager.getInstance().publishService(Receiver::class.java, verticle, Handler {
                ServiceManager.getInstance().consumeService(Receiver::class.java, Handler {
                    context.verify {
                        assertThat(it.succeeded()).isTrue()
                    }

                    val service = it.result()
                    val address = "${Receiver::class.java.name}.${TestModel::class.java.simpleName}"

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
