package com.xlongwei.apijson.handler;

import java.nio.ByteBuffer;

import javax.servlet.http.HttpSession;

import com.networknt.rpc.Handler;
import com.networknt.utility.HybridUtils;
import com.xlongwei.apijson.DemoApplication;
import com.xlongwei.apijson.DemoVerifier;

import apijson.JSON;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public abstract class Abstract implements Handler {

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        if (!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
            return HybridUtils.toByteBuffer(DemoApplication.badRequest);
        } else {
            String request = JSON.toJSONString(HybridUtils.getBodyMap(exchange));
            // 这里可以适配一个HttpSession，目前在DemoVerifier做了简单鉴权
            String response = handle(request, DemoVerifier.session());
            return HybridUtils.toByteBuffer(response);
        }
    }

    public abstract String handle(String request, HttpSession session);
}
