
package com.xlongwei.gservice.handler;

import java.nio.ByteBuffer;

import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.NioUtils;

import io.undertow.server.HttpServerExchange;

@ServiceHandler(id = "xlongwei.com/gservice/hello/0.0.1")
public class Hello implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        return NioUtils.toByteBuffer("{\"message\":\"Hello World!\"}");
    }
}
