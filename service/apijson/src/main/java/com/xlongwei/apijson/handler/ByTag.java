package com.xlongwei.apijson.handler;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.StringUtils;

import com.networknt.rpc.Handler;
import com.networknt.utility.HybridUtils;
import com.xlongwei.apijson.DemoApplication;

import apijson.JSON;
import apijson.RequestMethod;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public abstract class ByTag implements Handler {
    static String byTag = "ByTag/";

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        if (!DemoApplication.apijsonEnabled || !Methods.POST.equals(exchange.getRequestMethod())) {
            return HybridUtils.toByteBuffer(DemoApplication.badRequest);
        } else {
            String requestURI = exchange.getRequestURI();
            int pos = requestURI.indexOf(byTag);
            String tag = pos < 0 ? null : requestURI.substring(pos + byTag.length());
            if (StringUtils.isBlank(tag)) {
                return HybridUtils.toByteBuffer(DemoApplication.badRequest);
            }
            String request = JSON.toJSONString(HybridUtils.getBodyMap(exchange));
            // 这里可以适配一个HttpSession，目前在DemoVerifier做了简单鉴权
            String response = DemoApplication.apijson.parseByTag(method(), tag, null, request, null);
            return HybridUtils.toByteBuffer(response);
        }
    }

    public abstract RequestMethod method();
}
