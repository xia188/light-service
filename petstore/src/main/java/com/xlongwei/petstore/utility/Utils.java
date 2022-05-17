package com.xlongwei.petstore.utility;

import java.util.Deque;

import javax.sql.DataSource;

import com.networknt.service.SingletonServiceFactory;

import io.undertow.server.HttpServerExchange;

public class Utils {
    public static DataSource ds = SingletonServiceFactory.getBean(DataSource.class);

    /** 获取请求参数 */
    public static String getQueryParameter(HttpServerExchange exchange, String name) {
        Deque<String> deque = exchange.getQueryParameters().get(name);
        return deque == null ? null : deque.getFirst();
    }

    /** 获取路径参数 */
    public static String getPathParameter(HttpServerExchange exchange, String name) {
        Deque<String> deque = exchange.getPathParameters().get(name);
        return deque == null ? null : deque.getFirst();
    }
}
