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
import com.nannoq.tools.web.responsehandlers.ResponseLogHandler.Companion.BODY_CONTENT_TAG
import io.vertx.core.*
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.serviceproxy.ServiceException
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpHeaders
import org.mikand.autonomous.services.core.communication.Collector
import org.mikand.autonomous.services.core.events.CommandEventBuilder
import org.mikand.autonomous.services.core.events.CommandEventImpl
import org.mikand.autonomous.services.core.events.DataEventBuilder
import org.mikand.autonomous.services.core.events.DataEventImpl
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.collections.HashMap

open class S3FileReceiverImpl(private val config: JsonObject = JsonObject()) :
        FileReceiver, Collector<String>, AbstractVerticle() {
    private val itMap = HashMap<String, Handler<AsyncResult<String>>>()

    override val collectorMap: HashMap<String, Handler<AsyncResult<String>>>
        get() = itMap

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

    private val tokenMap: HashSet<String> = HashSet()

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

        initializeCollector()

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
            when {
                it.succeeded() -> initializeHttpServer(startFuture)
                else -> startFuture?.fail(it.cause())
            }
        })
    }

    private fun initializeHttpServer(startFuture: Future<Void>?) {
        val options = HttpServerOptions().setTcpKeepAlive(true)
        val router = createRouter()

        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(port!!, {
                    when {
                        it.succeeded() -> {
                            server = it.result()

                            startFuture?.complete()
                        }
                        else -> startFuture?.fail(it.cause())
                    }
                })
    }

    private fun createRouter(): Router {
        val router = Router.router(vertx)

        RoutingHelper.routeWithBodyAndLogger(Supplier {
            router.post("$rootPath/:token")
        }, Consumer {
            it.get().handler(this::uploadHandler)
        })

        RoutingHelper.routeWithLogger(Supplier {
            router.get("$rootPath/:token")
        }, Consumer {
            it.get().handler(this::downloadHandler)
        })

        return router
    }

    private fun uploadHandler(routingContext: RoutingContext) {
        val token = routingContext.pathParam("token")

        when (token) {
            null -> {
                routingContext.response().statusCode = 401
                routingContext.next()
            }
            else -> {
                val mappedToken = tokenMap.contains(token)

                when {
                    mappedToken -> hasAnyoneCollectedIt(token, Handler {
                        when {
                            it.succeeded() -> doUpload(routingContext, token)
                            else -> {
                                routingContext.response().statusCode = 404
                                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "Token not found!").encode())
                                routingContext.next()
                            }
                        }
                    })
                    else -> doUpload(routingContext, token)
                }
            }
        }
    }

    private fun doUpload(routingContext: RoutingContext, token: String) {
        val fileUploads = routingContext.fileUploads()

        when {
            fileUploads.isEmpty() -> noFilesResponse(routingContext)
            fileUploads.size > 1 -> tooManyFilesResponse(routingContext)
            else -> handleFileUpload(routingContext, fileUploads.iterator().next(), token)
        }
    }

    private fun downloadHandler(routingContext: RoutingContext) {
        val token = routingContext.pathParam("token")

        when (token) {
            null -> {
                routingContext.response().statusCode = 404
                routingContext.next()
            }
            else -> vertx.executeBlocking<Boolean>({
                it.complete(client.doesObjectExist(bucketName, token))
            }, false, {
                when {
                    it.succeeded() -> {
                        val fiveMinutesFromNow = Date.from(Instant.now().plus(5, ChronoUnit.MINUTES))

                        vertx.executeBlocking<String>({
                            it.complete(client.generatePresignedUrl(bucketName, token, fiveMinutesFromNow).toString())
                        }, false, {
                            when {
                                it.succeeded() -> {
                                    routingContext.response().statusCode = 302
                                    routingContext.response().putHeader(HttpHeaders.LOCATION, it.result())
                                    routingContext.next()
                                }
                                else -> {
                                    routingContext.response().statusCode = 404
                                    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                    routingContext.put(BODY_CONTENT_TAG, JsonObject().put("error", "File not found!").encode())
                                    routingContext.next()
                                }
                            }
                        })
                    }
                    else -> {
                        routingContext.response().statusCode = 404
                        routingContext.next()
                    }
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

    override fun fileReceiverInitializeRead(receiveInputEvent: CommandEventImpl,
                                            resultHandler: Handler<AsyncResult<DataEventImpl>>): FileReceiver {
        val objectKey = receiveInputEvent.body.getString("key")

        vertx.executeBlocking<String>({
            when {
                client.doesObjectExist(bucketName, objectKey) ->
                    it.complete(client.getObjectMetadata(bucketName, objectKey).userMetadata["extension"])
                else -> it.fail(IllegalArgumentException())
            }
        }, false, {
            when {
                it.succeeded() -> {
                    val alternateDownloadHost = finalConfig.getString("custom_download_host") ?: "localhost"
                    val dev = finalConfig.getBoolean("dev") ?: false
                    val downloadHost = if (dev) alternateDownloadHost else host
                    val url = "$protocol://$downloadHost:$port$rootPath/$objectKey"

                    resultHandler.handle(Future.succeededFuture(DataEventBuilder()
                            .withSuccess()
                            .withAction("${objectKey}_DOWNLOAD_URL")
                            .withMetadata(JsonObject().put("statusCode", 200))
                            .withBody(JsonObject()
                                    .put("downloadUrl", url)
                                    .put("extension", it.result()))
                            .build()))
                }
                else -> fileDoesNotExistResponse(it.map(false), resultHandler, "FILE_UPLOAD_URL")
            }
        })

        return this
    }

    override fun fileReceiverInitializeCreate(receiveInputEvent: CommandEventImpl,
                                              resultHandler: Handler<AsyncResult<DataEventImpl>>): S3FileReceiverImpl {
        val token = UUID.randomUUID().toString()
        val uploadJson = JsonObject().put("uploadUrl", "$protocol://$host:$port$rootPath/$token")
        val eventBuilder = DataEventBuilder()

        tokenMap.add(token)

        eventBuilder
                .withSuccess()
                .withAction("FILE_UPLOAD_URL")
                .withMetadata(JsonObject().put("statusCode", 202))
                .withBody(uploadJson)

        resultHandler.handle(Future.succeededFuture(eventBuilder.build()))

        return this
    }

    override fun fileReceiverDelete(receiveInputEvent: CommandEventImpl): S3FileReceiverImpl {
        fileReceiverDeleteWithReceipt(receiveInputEvent, Handler {})

        return this
    }

    override fun fileReceiverDeleteWithReceipt(receiveInputEvent: CommandEventImpl,
                                               resultHandler: Handler<AsyncResult<DataEventImpl>>): S3FileReceiverImpl {
        val objectKey = receiveInputEvent.body.getString("key")

        vertx.executeBlocking<Boolean>({
            it.complete(client.doesObjectExist(bucketName, objectKey))
        }, false, {
            when {
                it.succeeded() -> handleFileDeletion(objectKey, resultHandler)
                else -> fileDoesNotExistResponse(it, resultHandler, "FILE_DELETION")
            }
        })

        return this
    }

    private fun fileDoesNotExistResponse(it: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<DataEventImpl>>,
                                         failureRoot: String) {
        when {
            it.cause() != null -> {
                logger.error("Error creating URL!", it.cause())

                val eventBuilder = DataEventBuilder()
                        .withFailure()
                        .withAction("FILE_UPLOAD_URL_FAILURE")
                        .withMetadata(JsonObject().put("statusCode", 500))

                resultHandler.handle(ServiceException.fail(500, "", eventBuilder.build().toJson()))
            }
            else -> {
                val eventBuilder = CommandEventBuilder()
                        .withFailure()
                        .withAction("${failureRoot}_FAILURE")
                        .withMetadata(JsonObject().put("statusCode", 404))
                        .withBody(JsonObject().put("error", "File does not exist!"))

                resultHandler.handle(ServiceException.fail(404, "", eventBuilder.build().toJson()))
            }
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

                when {
                    putObjectResult != null -> fut.complete(UploadResult(finalName, extension))
                    else -> fut.fail(AmazonS3Exception("Unable to upload file!"))
                }
            }
        }, false, { res -> uploadResult(routingContext, res, token) })
    }

    private fun uploadResult(routingContext: RoutingContext, result: AsyncResult<UploadResult>, token: String) {
        when {
            result.succeeded() -> {
                tokenMap.remove(token)

                try {
                    val upload = result.result()
                    val extension = upload.extension
                    val key = upload.fileName
                    val output = JsonObject()
                            .put("key", key)
                            .put("extension", extension)
                            .put("fullName", "$key.${upload.extension}")
                    val outputEvent = DataEventBuilder()
                            .withSuccess()
                            .withAction("${extension.toUpperCase()}_UPLOADED")
                            .withMetadata(JsonObject().put("statusCode", 200))
                            .withBody(output)
                            .build()

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
                } catch (e: Throwable) {
                    tokenMap.add(token)
                }
            }
            else -> {
                val cause = result.cause()

                logger.error("Failed upload!", cause)

                when {
                    result.cause() is AmazonS3Exception -> uploadFailedResponse(routingContext)
                    result.cause() is IOException -> unableToReadFileResponse(routingContext)
                }
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

    private fun handleFileDeletion(key: String, resultHandler: Handler<AsyncResult<DataEventImpl>>) {
        vertx.executeBlocking<Boolean>({
            client.deleteObject(bucketName, key)

            it.complete()
        }, false, { res -> deletionResult(res, resultHandler) })
    }

    private fun deletionResult(res: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<DataEventImpl>>) {
        when {
            res.succeeded() -> successFullDeleteResponse(resultHandler)
            else -> failedDeleteResponse(res, resultHandler)
        }
    }

    private fun successFullDeleteResponse(resultHandler: Handler<AsyncResult<DataEventImpl>>) {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("FILE_DELETION")
                .withMetadata(JsonObject().put("statusCode", 204))
                .build()

        resultHandler.handle(Future.succeededFuture(outputEvent))
    }

    private fun failedDeleteResponse(res: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<DataEventImpl>>) {
        logger.error("Failed deletion", res.cause())

        val outputEvent = DataEventBuilder()
                .withFailure()
                .withAction("FILE_DELETION_FAILURE")
                .withMetadata(JsonObject().put("statusCode", 500))
                .build()

        resultHandler.handle(ServiceException.fail(500, "", outputEvent.toJson()))
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<DataEventImpl>>): FileReceiver {
        val outputEvent = DataEventBuilder()
                .withSuccess()
                .withAction("ADDRESS")
                .withMetadata(JsonObject().put("statusCode", 200))
                .withBody(JsonObject().put("address", subscriptionAddress))
                .build()

        addressHandler.handle(Future.succeededFuture(outputEvent))

        return this
    }

    override fun iHaveIt(key: String, resultHandler: Handler<AsyncResult<String>>) {
        val token = tokenMap.remove(key)

        when {
            token -> resultHandler.handle(Future.succeededFuture(key))
            else -> resultHandler.handle(ServiceException.fail(404, "Not fonund..."))
        }
    }

    override fun getVertx(): Vertx {
        return super<AbstractVerticle>.getVertx()
    }
}