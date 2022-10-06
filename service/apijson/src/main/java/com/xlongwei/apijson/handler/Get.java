
package com.xlongwei.apijson.handler;

import java.nio.ByteBuffer;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.xlongwei.apijson.DemoApplication;

import apijson.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

@ServiceHandler(id = "xlongwei.com/apijson/get/0.0.1")
public class Get implements HybridHandler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        if (!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
            return HybridUtils.toByteBuffer(DemoApplication.badRequest);
        } else {
            String request = JSON.toJSONString(HybridUtils.getBodyMap(exchange));
            String response = DemoApplication.apijson.get(request, null);
            return HybridUtils.toByteBuffer(response);
        }
    }

    @Override
    public void init() {
        DemoApplication.start();
    }
}
