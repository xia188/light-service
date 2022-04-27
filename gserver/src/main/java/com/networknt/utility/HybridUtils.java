package com.networknt.utility;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.networknt.httpstring.AttachmentConstants;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Headers;

public class HybridUtils {
    private HybridUtils() {
    }

    /** 判断请求是否表单提交 */
    public static boolean isForm(HttpServerExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        boolean isForm = StringUtils.isNotBlank(contentType) && (contentType.startsWith("multipart/form-data")
                || contentType.startsWith("application/x-www-form-urlencoded"));
        return isForm;
    }

    /** 获取请求参数 */
    public static String getParam(HttpServerExchange exchange, String name) {
        return getObject(exchange, name, String.class);
    }

    /** FormValue包含fileName+FileItem */
    public static FormValue getFile(HttpServerExchange exchange, String name) {
        return getObject(exchange, name, FormValue.class);
    }

    /** 获取正文字符串 */
    public static String getBodyString(HttpServerExchange exchange) {
        return getObject(exchange, "BODYSTRING", String.class);
    }

    /** 获取正文和参数Map */
    @SuppressWarnings({ "unchecked" })
    public static Map<String, Object> getBodyMap(HttpServerExchange exchange) {
        return (Map<String, Object>) exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
    }

    /**
     * @param name  参数名
     * @param clazz 支持String、FormValue、List
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T getObject(HttpServerExchange exchange, String name, Class<T> clazz) {
        Map<String, Object> body = getBodyMap(exchange);
        if (body != null) {
            Object obj = body.get(name);
            if (obj != null) {
                Class<? extends Object> clz = obj.getClass();
                if (clazz == clz || clazz.isAssignableFrom(clz)) {
                    return (T) obj;
                } else if (String.class == clazz) {
                    return (T) obj.toString();
                } else if (List.class.isAssignableFrom(clz)) {
                    obj = ((List) obj).get(0);
                    clz = obj.getClass();
                    if (clazz == clz || clazz.isAssignableFrom(clz)) {
                        return (T) obj;
                    }
                }
            }
        }
        return null;
    }

    public static ByteBuffer toByteBuffer(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }
}
