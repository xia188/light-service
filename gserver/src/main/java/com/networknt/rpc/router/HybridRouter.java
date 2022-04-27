package com.networknt.rpc.router;

import org.apache.commons.lang3.StringUtils;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

/**
 * /hybrid/{service}/{action}
 */
public class HybridRouter extends RpcRouter {
        public static final String hybridPath = StringUtils.defaultIfBlank(System.getProperty("rpc-router.hybridPath"),
                        "/hybrid");
        public static final String defaultHost = StringUtils.defaultIfBlank(
                        System.getProperty("rpc-router.defaultHost"),
                        "xlongwei.com");
        public static final String defaultVersion = StringUtils.defaultIfBlank(
                        System.getProperty("rpc-router.defaultVersion"),
                        "0.0.1");

        @Override
        public HttpHandler getHandler() {
                PathHandler httpHandler = (PathHandler) super.getHandler();
                System.out.println("hybridPath = " + hybridPath);
                httpHandler.addPrefixPath(hybridPath, new HybridHandler());
                return httpHandler;
        }
}
