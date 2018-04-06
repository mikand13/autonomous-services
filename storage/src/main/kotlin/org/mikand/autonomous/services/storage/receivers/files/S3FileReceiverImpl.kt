package org.mikand.autonomous.services.storage.receivers.files

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.nannoq.tools.repository.dynamodb.DynamoDBRepository
import com.nannoq.tools.web.RoutingHelper
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.mikand.autonomous.services.storage.receivers.ReceiveEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveEventType
import org.mikand.autonomous.services.storage.receivers.ReceiveInputEvent
import org.mikand.autonomous.services.storage.receivers.ReceiveStatus
import java.io.File
import java.io.IOException
import java.util.*

class S3FileReceiverImpl(val config: JsonObject = JsonObject()) : FileReceiver, AbstractVerticle() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private lateinit var mapper: DynamoDBMapper
    private lateinit var region: String
    private lateinit var host: String
    private var port: Int? = null
    private lateinit var rootPath: String
    private lateinit var bucketName: String
    private lateinit var subscriptionAddress: String
    private lateinit var server: HttpServer

    override fun start(startFuture: Future<Void>?) {
        region = config.getString("aws_s3_region") ?: "eu-west-1"
        host = config.getString("aws_s3_file_receiver_host") ?: "localhost"
        port = config.getInteger("aws_s3_file_receiver_port") ?: 5443
        rootPath = config.getString("aws_s3_file_receiver_rootPath") ?: "/uploads"
        bucketName = config.getString("aws_s3_file_receiver_bucketName") ?: "test-bucket"

        mapper = DynamoDBRepository.getS3DynamoDbMapper()
        subscriptionAddress = S3FileReceiverImpl::class.java.name

        initializeHttpServer(startFuture?.completer())
    }

    private fun initializeHttpServer(completer: Handler<AsyncResult<Void>>?) {
        val options = HttpServerOptions().setTcpKeepAlive(true)
        val router = createRouter()

        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(port!!, {
                    if (it.succeeded()) {
                        server = it.result()

                        completer?.handle(Future.succeededFuture())
                    } else {
                        completer?.handle(Future.failedFuture(it.cause()))
                    }
                })
    }

    private fun createRouter(): Router {
        val router = Router.router(vertx)

        RoutingHelper.routeWithBodyAndLogger({
            router.put("$rootPath/:token")
        }, {
            it.get().handler({ ctx ->
                ctx.request().isExpectMultipart = true
                ctx.next()
            }).handler(this::uploadHandler)
        })

        return router
    }

    private fun uploadHandler(routingContext: RoutingContext) {
        val fileUploads = routingContext.fileUploads()

        if (fileUploads.isEmpty()) {
            noFilesResponse(routingContext)
        } else if (fileUploads.size > 1) {
            tooManyFilesResponse(routingContext)
        } else {
            handleFileUpload(routingContext, fileUploads.iterator().next())
        }
    }

    private fun noFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.response().end(JsonObject().put("error", "No files received!").encode())
    }

    private fun tooManyFilesResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 400
        routingContext.response().end(JsonObject().put("error", "Only one file per token!").encode())
    }

    override fun stop(stopFuture: Future<Void>?) {
        server.close(stopFuture?.completer())
    }

    override fun fileReceiverInitializeCreate(receiveInputEvent: ReceiveInputEvent,
                                              resultHandler: Handler<AsyncResult<ReceiveEvent>>): S3FileReceiverImpl {
        val token = UUID.randomUUID().toString()
        val uploadJson = JsonObject().put("uploadUrl", "$host:$port:$rootPath/$token")
        val receiveStatus = ReceiveStatus(202, statusObject = uploadJson)
        val receiveEvent = ReceiveEvent(ReceiveEventType.DATA.name, "FILE_UPLOAD_URL", receiveStatus)

        resultHandler.handle(Future.succeededFuture(receiveEvent))

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

    private fun handleFileUpload(routingContext: RoutingContext, fileUpload: FileUpload) {
        val tempFileName = fileUpload.uploadedFileName()
        val S3Link = mapper.createS3Link(bucketName, tempFileName)

        vertx.executeBlocking<Boolean>({
            val javaFile = File(tempFileName)
            val putObjectResult = S3Link.uploadFrom(javaFile)

            if (putObjectResult != null) {
                it.complete()
            } else {
                it.fail(AmazonS3Exception("Unable to upload file!"))
            }
        }, false, { res -> uploadResult(routingContext, res) })
    }

    private fun uploadResult(routingContext: RoutingContext, result: AsyncResult<Boolean>) {
        if (result.succeeded()) {
            routingContext.response().statusCode = 202
            routingContext.response().end()
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
        routingContext.response().end(JsonObject().put("error", "Unable to process file!").encode())
    }

    private fun unableToReadFileResponse(routingContext: RoutingContext) {
        routingContext.response().statusCode = 422
        routingContext.response().end(JsonObject().put("error", "Unable to process file!").encode())
    }

    private fun handleFileDeletion(key: String, resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        vertx.executeBlocking<Boolean>({
            mapper.s3ClientCache.getClient(region).deleteObject(bucketName, key)

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
        val receiveEvent = ReceiveEvent(ReceiveEventType.DATA.name, "FILE_DELETION", receiveStatus)

        resultHandler.handle(Future.succeededFuture(receiveEvent))
    }

    private fun failedDeleteResponse(res: AsyncResult<Boolean>, resultHandler: Handler<AsyncResult<ReceiveEvent>>) {
        resultHandler.handle(Future.failedFuture(res.cause()))
    }

    override fun fetchSubscriptionAddress(addressHandler: Handler<AsyncResult<ReceiveEvent>>): FileReceiver {
        addressHandler.handle(Future.succeededFuture(ReceiveEvent(ReceiveEventType.DATA.name, "ADDRESS",
                ReceiveStatus(200, statusObject = JsonObject().put("address", subscriptionAddress)))))

        return this
    }
}