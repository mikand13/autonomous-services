package org.mikand.autonomous.services.storage.receivers.files

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
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
import io.vertx.serviceproxy.ServiceException
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpHeaders
import org.mikand.autonomous.services.storage.receivers.ReceiveEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType.DATA
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType.DATA_FAILURE
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveStatus
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

open class S3FileReceiverImpl(private val config: JsonObject = JsonObject()) : FileReceiver, AbstractVerticle() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var finalConfig: JsonObject
    private lateinit var client: AmazonS3
    private lateinit var region: String
    private lateinit var protocol: String
    private lateinit var host: String
    private var port: Int? = null
    private lateinit var rootPath: String
    private lateinit var bucketName: String
    private lateinit var s3Endpoint: String
    private lateinit var server: HttpServer

    private lateinit var tokenMap: AsyncMap<String, JsonObject>

    private var subscriptionAddress: String? =
            config.getString("subscriptionAddress") ?: "${S3FileReceiverImpl::class.java.name}.data"

    override fun start(startFuture: Future<Void>?) {
        finalConfig = config.mergeIn(config())
        protocol = if (finalConfig.getBoolean("ssl")) "https" else "http"
        region = finalConfig.getString("aws_s3_region") ?: "eu-west-1"
        host = finalConfig.getString("aws_s3_file_receiver_host") ?: "localhost"
        port = finalConfig.getInteger("aws_s3_file_receiver_port") ?: 5443
        rootPath = finalConfig.getString("aws_s3_file_receiver_rootPath") ?: "/uploads"
        bucketName = finalConfig.getString("aws_s3_file_receiver_bucketName") ?: "test-autonoumous-services-bucket"
        s3Endpoint = finalConfig.getString("aws_s3_endPoint") ?: "s3.$region.amazonaws.com"
        subscriptionAddress = finalConfig.getString("subscriptionAddress") ?: subscriptionAddress

        logger.info("Connecting to Region: $region, Endpoint: $s3Endpoint, using Bucket: $bucketName")

        val dynamoDBId = finalConfig.getString("aws_s3_iam_id")
        val dynamoDBKey = finalConfig.getString("aws_s3_iam_key")

        vertx.executeBlocking<Boolean>({
            val creds = if (dynamoDBId == null || dynamoDBKey == null) AnonymousAWSCredentials() else
                BasicAWSCredentials(dynamoDBId, dynamoDBKey)
            val statCreds = AWSStaticCredentialsProvider(creds)
            val endpoint = EndpointConfiguration(s3Endpoint, region)

            val dev = finalConfig.getBoolean("dev") ?: false
            val test = finalConfig.getBoolean("test") ?: false

            client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(endpoint)
                    .withCredentials(statCreds)
                    .withPathStyleAccessEnabled(dev || test)
                    .build()

            try {
                if (!client.doesBucketExist(bucketName)) {
                    try {
                        client.createBucket(bucketName)
                    } catch (e: AmazonS3Exception) {
                        if (e.errorCode != "BucketAlreadyExists") {
                            logger.error("Error in initializing bucket!", e)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Unable to access S3 to Verify Bucket!", e)
            }

            it.complete()
        }, false, {
            if (it.succeeded()) {
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
            } else {
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
            router.post("$rootPath/:token")
        }, {
            it.get().handler(this::uploadHandler)
        })

        RoutingHelper.routeWithLogger({
            router.get("$rootPath/:token")
        }, {
            it.get().handler(this::downloadHandler)
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
                    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Token not found!").encode())
                    routingContext.next()
                }
            })
        }
    }

    private fun downloadHandler(routingContext: RoutingContext) {
        val token = routingContext.pathParam("token")

        if (token == null) {
            routingContext.response().statusCode = 404
            routingContext.next()
        } else {
            vertx.executeBlocking<Boolean>({
                it.complete(client.doesObjectExist(bucketName, token))
            }, false, {
                if (it.succeeded()) {
                    val fiveMinutesFromNow = Date.from(Instant.now().plus(5, ChronoUnit.MINUTES))

                    vertx.executeBlocking<String>({
                        it.complete(client.generatePresignedUrl(bucketName, token, fiveMinutesFromNow).toString())
                    }, false, {
                        if (it.succeeded()) {
                            routingContext.response().statusCode = 302
                            routingContext.response().putHeader(HttpHeaders.LOCATION, it.result())
                            routingContext.next()
                        } else {
                            routingContext.response().statusCode = 404
                            routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "File not found!").encode())
                            routingContext.next()
                        }
                    })
                } else {
                    routingContext.response().statusCode = 404
                    routingContext.next()
                }
            })
        }
    }

    private fun noFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "No files received!").encode())
        routingContext.next()
    }

    private fun tooManyFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Only one file per token!").encode())
        routingContext.next()
    }

    override fun stop(stopFuture: Future<Void>?) {
        server.close(stopFuture?.completer())
    }

    override fun fileReceiverInitializeRead(receiveInputEvent: ReceiveInputEvent,
                                            resultHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver {
        val objectKey = receiveInputEvent.body.getString("key")

        vertx.executeBlocking<String>({
            if (client.doesObjectExist(bucketName, objectKey)) {
                it.complete(client.getObjectMetadata(bucketName, objectKey).userMetadata["extension"])
            } else {
                it.fail(IllegalArgumentException())
            }
        }, false, {
            if (it.succeeded()) {
                val alternateDownloadHost = finalConfig.getString("custom_download_host") ?: "localhost"
                val dev = finalConfig.getBoolean("dev") ?: false
                val downloadHost = if (dev) alternateDownloadHost else host
                val url = "$protocol://$downloadHost:$port$rootPath/$objectKey"

                resultHandler.handle(Future.succeededFuture(ReceiveEvent(DATA.name, "${objectKey}_DOWNLOAD_URL",
                        ReceiveStatus(200, statusObject = JsonObject()
                                .put("downloadUrl", url)
                                .put("extension", it.result())))))
            } else {
                fileDoesNotExistResponse(it.map(false), resultHandler, "FILE_UPLOAD_URL")
            }
        })

        return this
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

                resultHandler.handle(ServiceException.fail(500, "", JsonObject(Json.encode(receiveEvent))))
            }
        })


        return this
    }

    override fun fileReceiverDelete(receiveInputEvent: ReceiveInputEvent): S3FileReceiverImpl {
        fileReceiverDeleteWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun fileReceiverDeleteWithReceipt(receiveInputEvent: ReceiveInputEvent,
                                               resultHandler: Handler<AsyncResult<ReceiveEvent>>): S3FileReceiverImpl {
        val objectKey = receiveInputEvent.body.getString("key")

        vertx.executeBlocking<Boolean>({
            it.complete(client.doesObjectExist(bucketName, objectKey))
        }, false, {
            if (it.succeeded()) {
                handleFileDeletion(objectKey, resultHandler)
            } else {
                fileDoesNotExistResponse(it, resultHandler, "FILE_DELETION")
            }
        })

        return this
    }

    private fun fileDoesNotExistResponse(it: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<ReceiveEvent>>,
                                         failureRoot: String) {
        if (it.cause() != null) {
            logger.error("Error creating URL!", it.cause())

            val receiveStatus = ReceiveStatus(500,
                    statusObject = JsonObject())
            val receiveEvent = ReceiveEvent(DATA_FAILURE.name, "${failureRoot}_FAILURE", receiveStatus)

            resultHandler.handle(ServiceException.fail(500, "", JsonObject(Json.encode(receiveEvent))))
        } else {
            val receiveStatus = ReceiveStatus(404,
                    statusObject = JsonObject().put("error", "File does not exist!"))
            val receiveEvent = ReceiveEvent(DATA_FAILURE.name, "${failureRoot}_FAILURE", receiveStatus)

            resultHandler.handle(ServiceException.fail(404, "", JsonObject(Json.encode(receiveEvent))))
        }
    }

    private class UploadResult(val fileName: String, val extension: String)

    private fun handleFileUpload(routingContext: RoutingContext, fileUpload: FileUpload, token: String) {
        val tempFileName = fileUpload.uploadedFileName()

        vertx.executeBlocking<UploadResult>({ fut ->
            val javaFile = File(tempFileName)
            val extension = FilenameUtils.getExtension(fileUpload.fileName())
            val finalName = tempFileName.removePrefix("file-uploads/")
            val metaData = ObjectMetadata()
            metaData.userMetadata["extension"] = extension
            metaData.contentLength = javaFile.length()

            javaFile.inputStream().use {
                val putObjectResult = client.putObject(bucketName, finalName, it, metaData)

                if (putObjectResult != null) {
                    fut.complete(UploadResult(finalName, extension))
                } else {
                    fut.fail(AmazonS3Exception("Unable to upload file!"))
                }
            }
        }, false, { res -> uploadResult(routingContext, res, token) })
    }

    private fun uploadResult(routingContext: RoutingContext, result: AsyncResult<UploadResult>, token: String) {
        if (result.succeeded()) {
            tokenMap.remove(token, {
                val upload = result.result()
                val extension = upload.extension
                val key = upload.fileName
                val output = JsonObject()
                        .put("key", key)
                        .put("extension", extension)
                        .put("fullName", "$key.${upload.extension}")
                val outputEvent = ReceiveEvent(DATA.name, "${extension.toUpperCase()}_UPLOADED",
                        ReceiveStatus(200, statusObject = output))
                val encode = Json.encode(outputEvent)

                routingContext.response().statusCode = 202
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                routingContext.put(BODY_CONTENT_TAG, encode)
                routingContext.next()

                vertx.eventBus().publish(subscriptionAddress, JsonObject(encode))

                vertx.fileSystem().delete("file-uploads/$key", {
                    if (it.failed()) {
                        logger.error("Failed deletion of temporary file upload!", it.cause())
                    }
                })
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
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Unable to process file!").encode())
        routingContext.next()
    }

    private fun unableToReadFileResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 422
        routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
        logger.error("Failed deletion", res.cause())

        val receiveStatus = ReceiveStatus(500, statusObject = JsonObject())
        val receiveEvent = ReceiveEvent(DATA_FAILURE.name, "FILE_DELETION_FAILURE", receiveStatus)

        resultHandler.handle(ServiceException.fail(500, "", JsonObject(Json.encode(receiveEvent))))
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver {
        addressHandler.handle(Future.succeededFuture(ReceiveEvent(DATA.name, "ADDRESS",
                ReceiveStatus(200, statusObject = JsonObject().put("address", subscriptionAddress)))))

        return this
    }
}