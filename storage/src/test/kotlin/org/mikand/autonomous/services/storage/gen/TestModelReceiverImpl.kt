package org.mikand.autonomous.services.storage.gen

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.mikand.autonomous.services.storage.gen.models.TestModel
import org.mikand.autonomous.services.storage.receivers.dynamodb.DynamoDBReceiverVerticle

class TestModelReceiverImpl(vertx: Vertx, config: JsonObject) : DynamoDBReceiverVerticle<TestModel>(vertx, TestModel::class.java, config)
