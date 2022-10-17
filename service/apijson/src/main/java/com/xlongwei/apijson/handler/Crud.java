package com.xlongwei.apijson.handler;

import javax.servlet.http.HttpSession;

import com.networknt.rpc.router.HybridRouter;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;
import com.xlongwei.apijson.DemoApplication;

import io.undertow.server.HttpServerExchange;

public class Crud extends Abstract {

    @Override
    public String handle(String request, HttpSession session) {
        HttpServerExchange exchange = HybridUtils.exchange.get();
        String requestURI = exchange.getRequestURI();
        requestURI = requestURI.substring(HybridRouter.hybridPath.length());
        String[] split = StringUtils.split(requestURI, '/');
        String tag = split.length > 2 ? split[2] : null;
        String method = split[1];
        return StringUtils.isBlank(tag) ? DemoApplication.apijson.crud(method, request, session)
                : DemoApplication.apijson.crudByTag(method, tag, null, request, session);
    }

}
