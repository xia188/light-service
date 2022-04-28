
package com.xlongwei.gservice.handler;

import java.nio.ByteBuffer;

import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;

import io.undertow.server.HttpServerExchange;

@ServiceHandler(id = "xlongwei.com/gservice/hello/0.0.1")
public class Hello implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        // NioUtils处理中文有问题，String.length()不是字节长度
        return HybridUtils.toByteBuffer("{\"message\":\"Hello World!\"}");
    }
}
