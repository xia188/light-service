package com.networknt.rpc.router;

import static com.networknt.rpc.router.JsonHandler.STATUS_HANDLER_NOT_FOUND;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.Handler;
import com.networknt.rpc.security.JwtVerifyHandler;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Util;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HybridHandler extends AbstractRpcHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Methods.POST.equals(exchange.getRequestMethod())
                || Methods.PUT.equals(exchange.getRequestMethod())) {
            String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            if (contentType != null && (contentType.startsWith("multipart/form-data")
                    || contentType.startsWith("application/x-www-form-urlencoded"))) {
                FormParserFactory.Builder builder = FormParserFactory.builder();
                builder.setDefaultCharset(Constants.DEFAULT_CHARACTER);
                FormDataParser parser = builder.build().createParser(exchange);
                FormData formData = null;
                if (parser != null) {
                    exchange.startBlocking();
                    formData = parser.parseBlocking();
                }
                Map<String, Object> bodyMap = new HashMap<>();
                parseFormData(bodyMap, formData);
                parseQueryParameters(bodyMap, exchange);
                hanadleRequest(exchange, bodyMap);
            } else {
                exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
                    Map<String, Object> bodyMap = new HashMap<>();
                    parseBodyMessage(bodyMap, message);
                    parseQueryParameters(bodyMap, exchange);
                    hanadleRequest(exchange, bodyMap);
                }, StandardCharsets.UTF_8);
            }
        } else {
            Map<String, Object> bodyMap = new HashMap<>();
            parseQueryParameters(bodyMap, exchange);
            hanadleRequest(exchange, bodyMap);
        }
    }

    private void parseBodyMessage(Map<String, Object> bodyMap, String message) {
        if ((message = StringUtils.trimToEmpty(message)).isEmpty() == false) {
            int leftP = message.indexOf('{');
            if (leftP == 0) {
                bodyMap.putAll(JsonMapper.string2Map(message));
            } else if (leftP == -1 && message.indexOf('=') > 0) {
                decodeParamMap(bodyMap, message);
            } else {
                bodyMap.put("BODYSTRING", message);
            }
        }
    }

    static List<String> rpcKeys = Arrays.asList("host", "service", "action", "version", "data");

    @SuppressWarnings({ "unchecked" })
    private void hanadleRequest(HttpServerExchange exchange, Map<String, Object> bodyMap) {
        if (log.isInfoEnabled() && !bodyMap.isEmpty()) {
            log.info("bodyMap: {}", JsonMapper.toJson(bodyMap));
        }
        String serviceId = getServiceId(exchange.getRequestURI(), bodyMap);
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if (handler == null) {
            this.handleMissingHandler(exchange, serviceId);
            return;
        }
        // calling jwt scope verification here. token signature and expiration are done
        verifyJwt(JwtVerifyHandler.config, serviceId, exchange);
        // 兼容JsonHandler取data进行验证和处理，处理HybridHandler数据时移除host、version，但保留data
        if (bodyMap.keySet().containsAll(rpcKeys)) {
            bodyMap = (Map<String, Object>) bodyMap.get("data");
        } else {
            bodyMap.remove("host");
            bodyMap.remove("version");
        }
        // calling schema validator here.
        ByteBuffer error = validate(serviceId, bodyMap);
        if (error != null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(error);
            return;
        }
        exchange.putAttachment(AttachmentConstants.REQUEST_BODY, bodyMap);
        // if exchange is not ended, then do the processing.
        ByteBuffer result = handler.handle(exchange, bodyMap);
        if (result != null) {
            if (log.isDebugEnabled()) {
                log.debug(result.toString());
            }
            this.completeExchange(result, exchange);
        } else if (!exchange.isComplete()) {
            exchange.endExchange();
        }
    }

    private void parseQueryParameters(Map<String, Object> bodyMap, HttpServerExchange exchange) {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        for (Entry<String, Deque<String>> entry : params.entrySet()) {
            String param = entry.getKey();
            Deque<String> deque = entry.getValue();
            if (deque.size() > 1) {
                bodyMap.put(param, new LinkedList<>(deque));
            } else {
                bodyMap.put(param, deque.getFirst());
            }
        }
    }

    private void parseFormData(Map<String, Object> bodyMap, FormData formData) {
        if (formData == null) {
            return;
        }
        for (String name : formData) {
            Deque<FormValue> deque = formData.get(name);
            if (deque.size() > 1) {
                List<Object> values = new LinkedList<>();
                for (FormValue formValue : deque) {
                    values.add(formValue.isFileItem() ? formValue : formValue.getValue());
                }
                bodyMap.put(name, values);
            } else {
                FormValue formValue = deque.getFirst();
                bodyMap.put(name, formValue.isFileItem() ? formValue : formValue.getValue());
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    public static ByteBuffer validate(String serviceId, Object object) {
        // get schema from serviceId, remember that the schema is for the bodyMap
        Map<String, Object> serviceMap = (Map<String, Object>) JsonHandler.schema.get(serviceId);
        if (log.isDebugEnabled()) {
            try {
                log.debug("serviceId = " + serviceId + " serviceMap = "
                        + Config.getInstance().getMapper().writeValueAsString(serviceMap));
            } catch (Exception e) {
                log.error("Exception:", e);
            }
        }
        JsonNode jsonNode = Config.getInstance().getMapper().valueToTree(serviceMap.get("schema"));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(jsonNode);
        Set<ValidationMessage> errors = schema.validate(Config.getInstance().getMapper().valueToTree(object));
        ByteBuffer bf = null;
        if (errors.size() > 0) {
            try {
                Status status = new Status(Handler.STATUS_VALIDATION_ERROR,
                        Config.getInstance().getMapper().writeValueAsString(errors));
                log.error("Validation Error:" + status.toString());
                bf = HybridUtils.toByteBuffer(status.toString());
            } catch (JsonProcessingException e) {
                log.error("Exception:", e);
            }
        }
        return bf;
    }

    private String getServiceId(String requestURI, Map<String, Object> bodyMap) {
        requestURI = requestURI.substring(HybridRouter.hybridPath.length());
        String[] split = StringUtils.split(requestURI, '/');
        Object host = bodyMap.get("host");
        Object service = split.length >= 2 ? split[0] : bodyMap.get("service");
        Object action = split.length >= 2 ? split[1] : bodyMap.get("action");
        Object version = bodyMap.get("version");
        return (host == null ? HybridRouter.defaultHost : host) + "/" + service + "/" + action + "/"
                + (version == null ? HybridRouter.defaultVersion : version);
    }

    private void decodeParamMap(Map<String, Object> map, String paramsStr) {
        if (StringUtils.isNotBlank(paramsStr)) {
            String[] params = paramsStr.split("[&]");
            for (String param : params) {
                String[] split = param.split("[=]");
                String name = Util.urlDecode(split[0]);
                String value = Util.urlDecode(split[1]);
                map.put(name, value);
            }
        }
    }

    private void handleMissingHandler(HttpServerExchange exchange, String serviceId) {
        log.error("Handler is not found for serviceId " + serviceId);
        Status status = new Status(STATUS_HANDLER_NOT_FOUND, serviceId);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
    }
}
