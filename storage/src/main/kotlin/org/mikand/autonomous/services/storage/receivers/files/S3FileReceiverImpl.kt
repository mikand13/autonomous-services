package org.mikand.autonomous.services.storage.receivers.files

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.nannoq.tools.web.RoutingHelper
import com.nannoq.tools.web.responsehandlers.ResponseLogHandler.BODY_CONTENT_TAG
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.mikand.autonomous.services.storage.receivers.ReceiveEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType.DATA
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType.DATA_FAILURE
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveStatus
import java.io.File
import java.io.IOException
import java.util.*



class S3FileReceiverImpl(private val config: JsonObject = JsonObject()) : FileReceiver, AbstractVerticle() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var client: AmazonS3
    private lateinit var region: String
    private lateinit var protocol: String
    private lateinit var host: String
    private var port: Int? = null
    private lateinit var rootPath: String
    private lateinit var bucketName: String
    private lateinit var s3Endpoint: String
    private lateinit var subscriptionAddress: String
    private lateinit var server: HttpServer

    private lateinit var tokenMap: AsyncMap<String, JsonObject>

    override fun start(startFuture: Future<Void>?) {
        protocol = if (config.getBoolean("ssl")) "https" else "http"
        region = config.getString("aws_s3_region") ?: "eu-west-1"
        host = config.getString("aws_s3_file_receiver_host") ?: "localhost"
        port = config.getInteger("aws_s3_file_receiver_port") ?: 5443
        rootPath = config.getString("aws_s3_file_receiver_rootPath") ?: "/uploads"
        bucketName = config.getString("aws_s3_file_receiver_bucketName") ?: "test-bucket"
        s3Endpoint = config.getString("aws_s3_endPoint") ?: "s3.$region.amazonaws.com"

        val dynamoDBId = config.getString("aws_s3_iam_id")
        val dynamoDBKey = config.getString("aws_s3_iam_key")

        val creds = if (dynamoDBId == null || dynamoDBKey == null) AnonymousAWSCredentials() else
            BasicAWSCredentials(dynamoDBId, dynamoDBKey)
        val statCreds = AWSStaticCredentialsProvider(creds)
        val endpoint = EndpointConfiguration(s3Endpoint, region)

        client = AmazonS3ClientBuilder
              .standard()
              .withPathStyleAccessEnabled(true)
              .withEndpointConfiguration(endpoint)
              .withCredentials(statCreds)
              .build()

        subscriptionAddress = S3FileReceiverImpl::class.java.name

        val mapName = S3FileReceiverImpl::class.java.simpleName

        vertx.sharedData().getAsyncMap<String, JsonObject>(mapName, {
            if (it.succeeded()) {
                tokenMap = it.result()
                initializeHttpServer(startFuture)
            } else {
                logger.error("Unable to initialize tokenMap", it.cause())

                startFuture?.fail(it.cause())
            }
        })
    }

    private fun initializeHttpServer(startFuture: Future<Void>?) {
        val options = HttpServerOptions().setTcpKeepAlive(true)
        val router = createRouter()

        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(port!!, {
                    if (it.succeeded()) {
                        server = it.result()

                        startFuture?.complete()
                    } else {
                        startFuture?.fail(it.cause())
                    }
                })
    }

    private fun createRouter(): Router {
        val router = Router.router(vertx)

        RoutingHelper.routeWithBodyAndLogger({
            router.put("$rootPath/:token")
        }, {
            it.get().handler(this::uploadHandler)
        })

        return router
    }

    private fun uploadHandler(routingContext: RoutingContext) {
        val token = routingContext.pathParam("token")

        if (token == null) {
            routingContext.response().statusCode = 401
            routingContext.next()
        } else {
            tokenMap.get(token, {
                if (it.succeeded()) {
                    val fileUploads = routingContext.fileUploads()

                    when {
                        fileUploads.isEmpty() -> noFilesResponse(routingContext)
                        fileUploads.size > 1 -> tooManyFilesResponse(routingContext)
                        else -> handleFileUpload(routingContext, fileUploads.iterator().next(), token)
                    }
                } else {
                    routingContext.response().statusCode = 404
                    routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Token not found!").encode())
                    routingContext.next()
                }
            })
        }
    }

    private fun noFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "No files received!").encode())
        routingContext.next()
    }

    private fun tooManyFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Only one file per token!").encode())
        routingContext.next()
    }

    override fun stop(stopFuture: Future<Void>?) {
        server.close(stopFuture?.completer())
    }

    override fun fileReceiverInitializeCreate(receiveInputEvent: ReceiveInputEvent,
                                              resultHandler: Handler<AsyncResult<ReceiveEvent>>): S3FileReceiverImpl {
        val token = UUID.randomUUID().toString()
        val uploadJson = JsonObject().put("uploadUrl", "$protocol://$host:$port$rootPath/$token")

        tokenMap.put(token, receiveInputEvent.body, {
            if (it.succeeded()) {
                val receiveStatus = ReceiveStatus(202, statusObject = uploadJson)
                val receiveEvent = ReceiveEvent(DATA.name, "FILE_UPLOAD_URL", receiveStatus)

                resultHandler.handle(Future.succeededFuture(receiveEvent))
            } else {
                val receiveStatus = ReceiveStatus(500, statusObject = JsonObject())
                val receiveEvent = ReceiveEvent(DATA_FAILURE.name, "FILE_UPLOAD_URL_FAILURE", receiveStatus)

                logger.error("Error creating URL!", it.cause())

                resultHandler.handle(Future.failedFuture(Json.encodePrettily(receiveEvent)))
            }
        })


        return this
    }

    override fun fileReceiverInitializeDelete(receiveInputEvent: ReceiveInputEvent): S3FileReceiverImpl {
        fileReceiverInitializeDeleteWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun fileReceiverInitializeDeleteWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                                         resultHandler: Handler<AsyncResult<ReceiveEvent>>): S3FileReceiverImpl {
        val objectKey = receiveInputEvent.body.getString("key")

        handleFileDeletion(objectKey, resultHandler)

        return this
    }

    private fun handleFileUpload(routingContext: RoutingContext, fileUpload: FileUpload, token: String) {
        val tempFileName = fileUpload.uploadedFileName()

        vertx.executeBlocking<String>({
            val javaFile = File(tempFileName)
            val putObjectResult = client.putObject(bucketName, tempFileName, javaFile)

            if (putObjectResult != null) {
                it.complete(tempFileName)
            } else {
                it.fail(AmazonS3Exception("Unable to upload file!"))
            }
        }, false, { res -> uploadResult(routingContext, res, token) })
    }

    private fun uploadResult(routingContext: RoutingContext, result: AsyncResult<String>, token: String) {
        if (result.succeeded()) {
            tokenMap.remove(token, {
                routingContext.response().statusCode = 202
                routingContext.put(BODY_CONTENT_TAG, JsonObject().put("key", result.result()))
                routingContext.next()
            })
        } else {
            val cause = result.cause()

            logger.error("Failed upload!", cause)

            if (result.cause() is AmazonS3Exception) {
                uploadFailedResponse(routingContext)
            } else if (result.cause() is IOException) {
                unableToReadFileResponse(routingContext)
            }
        }
    }

    private fun uploadFailedResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 422
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Unable to process file!").encode())
        routingContext.next()
    }

    private fun unableToReadFileResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 422
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Unable to process file!").encode())
        routingContext.next()
    }

    private fun handleFileDeletion(key: String, resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        vertx.executeBlocking<Boolean>({
            client.deleteObject(bucketName, key)

            it.complete()
        }, false, { res -> deletionResult(res, resultHandler) })
    }

    private fun deletionResult(res: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        if (res.succeeded()) {
            successFullDeleteResponse(resultHandler)
        } else {
            failedDeleteResponse(res, resultHandler)
        }
    }

    private fun successFullDeleteResponse(resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        val receiveStatus = ReceiveStatus(204, statusObject = JsonObject())
        val receiveEvent = ReceiveEvent(DATA.name, "FILE_DELETION", receiveStatus)

        resultHandler.handle(Future.succeededFuture(receiveEvent))
    }

    private fun failedDeleteResponse(res: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        resultHandler.handle(Future.failedFuture(res.cause()))
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver {
        addressHandler.handle(Future.succeededFuture(ReceiveEvent(DATA.name, "ADDRESS",
                ReceiveStatus(200, statusObject = JsonObject().put("address", subscriptionAddress)))))

        return this
    }
}