@logger = Java::IoVertxCoreLogging::LoggerFactory.get_logger('RubyVerticle')
@service_manager = Java::ComNannoqToolsClusterServices::ServiceManager.getInstance
@heartbeat_service_class = Java::ComNannoqToolsClusterServices::HeartbeatService.java_class

def vertx_start_async start_future
  @service_manager.consumeService(@heartbeat_service_class, "GatewayHeartbeat") do |res|
    if res.succeeded
      res.result.ping do |ping_res|
        if ping_res.succeeded and ping_res.result
          start_future.complete
        else
          start_future.fail("Failed to launch ruby verticle!")
        end
      end
    else
      start_future.fail(res.cause)
    end
  end
end

def vertx_stop_async stop_future
  stop_future.complete
end