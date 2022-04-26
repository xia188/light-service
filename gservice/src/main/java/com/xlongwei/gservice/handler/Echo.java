
package com.xlongwei.gservice.handler;

import java.nio.ByteBuffer;

import com.networknt.config.JsonMapper;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.NioUtils;

import io.undertow.server.HttpServerExchange;

@ServiceHandler(id = "xlongwei.com/gservice/echo/0.0.1")
public class Echo implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        return NioUtils.toByteBuffer(JsonMapper.toJson(input));
    }
}
