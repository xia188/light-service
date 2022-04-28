
package com.xlongwei.gservice.handler;

import java.nio.ByteBuffer;

import com.networknt.config.JsonMapper;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/gservice/echo/0.0.1")
public class Echo implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String json = JsonMapper.toJson(input);
        log.info("bodyMap: {}", json);
        return HybridUtils.toByteBuffer(json);
    }
}
