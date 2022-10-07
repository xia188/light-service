
package com.xlongwei.apijson.handler;

import java.nio.ByteBuffer;

import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.xlongwei.apijson.DemoApplication;

import apijson.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

@ServiceHandler(id = "xlongwei.com/apijson/heads/0.0.1")
public class Heads implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        if (!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
            return HybridUtils.toByteBuffer(DemoApplication.badRequest);
        } else {
            String request = JSON.toJSONString(HybridUtils.getBodyMap(exchange));
            String response = DemoApplication.apijson.heads(request, null);
            return HybridUtils.toByteBuffer(response);
        }
    }
}
