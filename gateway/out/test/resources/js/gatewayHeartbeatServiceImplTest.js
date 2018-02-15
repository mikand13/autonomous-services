exports.vertxStartAsync = function(startFuture) {
  var ServiceManager = Java.type('com.nannoq.tools.cluster.services.ServiceManager');
  var JHeartbeatService = Java.type('com.nannoq.tools.cluster.services.HeartbeatService');

  ServiceManager.getInstance().consumeService(JHeartbeatService.class, "GatewayHeartbeat", function (res) {
    if (res.succeeded()) {
      res.result().ping(function (pingRes) {
        if (pingRes.succeeded() && pingRes.result()) {
          startFuture.complete();
        } else {
          startFuture.fail(res.cause());
        }
      })
    } else {
      startFuture.fail(res.cause());
    }
  });
};

exports.vertxStopAsync = function(stopFuture) {
  stopFuture.complete();
};