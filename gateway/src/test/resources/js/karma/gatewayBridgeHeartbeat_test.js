/*
 * MIT License
 *
 * Copyright (c) 2017 Anders Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

const EventBus = require("vertx3-eventbus-client");
const HeartBeatService = require("./extractedProxies/heartbeat_service-proxy.js");

describe('NotNull', function() {
    const eb = new EventBus("http://localhost:5443/eventbus");

    it('Is not Null', function() {
        eb.should.not.equal(null);
        HeartbeatService.should.not.equal(null);
    });

    eb.close();
});

describe('HeartBeatService should return true', function() {
    const eb = new EventBus("http://localhost:5443/eventbus");
    const HeartbeatService = new HeartBeatService("GatewayHeartbeat");

    it('HeartBeatService should return true!', function() {
        this.timeout(5000);

        eb.onopen = function() {
            HeartbeatService.ping(function (error, result) {
                expect(error).to.equal('undefined');
                expect(result).to.equal(true);

                done();

                eb.close();
            })
        };
    });
});