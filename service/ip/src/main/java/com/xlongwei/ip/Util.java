package com.xlongwei.ip;

import com.networknt.utility.StringUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

public class Util {
    /** 获取真实IP地址 */
    public static String getIp(HttpServerExchange exchange) {
        String[] ipHeaders = { "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP", "WL-Proxy-Client-IP", "Proxy-Client-IP",
                "X-Forwarded-For", "X-Real-IP" };
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        for (String ipHeader : ipHeaders) {
            String ipValue = requestHeaders.getFirst(ipHeader);
            if (!StringUtils.isBlank(ipValue) && !"unknown".equalsIgnoreCase(ipValue)) {
                int common = ipValue.indexOf(',');
                if (common > -1) {
                    // clientip,proxy1,proxy2
                    ipValue = ipValue.substring(0, common);
                }
                if (isIp(ipValue) && !"127.0.0.1".equals(ipValue)) {
                    return ipValue;
                }
            }
        }
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    /** 是否ip地址 */
    public static boolean isIp(String ip) {
        return org.lionsoul.ip2region.Util.isIpAddress(ip);
    }

}
