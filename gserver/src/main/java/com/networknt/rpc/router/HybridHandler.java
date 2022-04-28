package com.networknt.rpc.router;

import static com.networknt.rpc.router.JsonHandler.STATUS_HANDLER_NOT_FOUND;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import com.networknt.utility.Tuple;
import com.networknt.utility.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

public class HybridHandler extends AbstractRpcHandler {
    static Logger log = LoggerFactory.getLogger(HybridHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        parseBody(exchange);
        String serviceId = getServiceId(exchange);
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if (handler == null) {
            this.handleMissingHandler(exchange, serviceId);
            return;
        }
        // calling jwt scope verification here. token signature and expiration are done
        verifyJwt(JwtVerifyHandler.config, serviceId, exchange);
        Object data = HybridUtils.getBodyMap(exchange);
        // calling schema validator here.
        ByteBuffer error = validate(serviceId, data);
        if (error != null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(error);
            return;
        }
        // if exchange is not ended, then do the processing.
        ByteBuffer result = handler.handle(exchange, data);
        if (result != null) {
            if (log.isDebugEnabled()) {
                log.debug(result.toString());
            }
            this.completeExchange(result, exchange);
        } else if (!exchange.isComplete()) {
            exchange.endExchange();
        }
    }

    @SuppressWarnings({ "unchecked" })
    public static ByteBuffer validate(String serviceId, Object object) {
        // get schema from serviceId, remember that the schema is for the data object only.
        // the input object is the data attribute of the request body.
        Map<String, Object> serviceMap = (Map<String, Object>)JsonHandler.schema.get(serviceId);
        if(log.isDebugEnabled()) {
            try {
                log.debug("serviceId = " + serviceId  + " serviceMap = " + Config.getInstance().getMapper().writeValueAsString(serviceMap));
            } catch (Exception e) {
                log.error("Exception:", e);
            }
        }
        JsonNode jsonNode = Config.getInstance().getMapper().valueToTree(serviceMap.get("schema"));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(jsonNode);
        Set<ValidationMessage> errors = schema.validate(Config.getInstance().getMapper().valueToTree(object));
        ByteBuffer bf = null;
        if(errors.size() > 0) {
            try {
                Status status = new Status(Handler.STATUS_VALIDATION_ERROR, Config.getInstance().getMapper().writeValueAsString(errors));
                log.error("Validation Error:" + status.toString());
                bf = HybridUtils.toByteBuffer(status.toString());
            } catch (JsonProcessingException e) {
                log.error("Exception:", e);
            }
        }
        return bf;
    }

    public static String getServiceId(HttpServerExchange exchange) {
        String requestURI = exchange.getRequestURI();
        requestURI = requestURI.substring(HybridRouter.hybridPath.length());
        String[] split = StringUtils.split(requestURI, '/');
        String host = HybridUtils.getParam(exchange, "host");
        String service = split.length >= 2 ? split[0] : HybridUtils.getParam(exchange, "service");
        String action = split.length >= 2 ? split[1] : HybridUtils.getParam(exchange, "action");
        String version = HybridUtils.getParam(exchange, "version");
        return (StringUtils.isBlank(host) ? HybridRouter.defaultHost : host) + "/" + service + "/" + action + "/"
                + (StringUtils.isBlank(version) ? HybridRouter.defaultVersion : version);
    }

    // 解析body为Map<String, 可通过HybridUtils获取参数
    // Object>，Object可能是String、List<String>、FileItem、List<FileItem>
    public static boolean parseBody(HttpServerExchange exchange) {
        Map<String, Object> body = new HashMap<>(4);
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        for (Entry<String, Deque<String>> entry : params.entrySet()) {
            String param = entry.getKey();
            Deque<String> deque = entry.getValue();
            if (deque.size() > 1) {
                body.put(param, new LinkedList<>(deque));
            } else {
                body.put(param, deque.getFirst());
            }
        }
        boolean allowBody = Methods.POST.equals(exchange.getRequestMethod())
                || Methods.PUT.equals(exchange.getRequestMethod()), isForm = false;
        if (allowBody) {
            List<Tuple<String, Object>> others = new LinkedList<>();
            String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            try {
                exchange.startBlocking();// 参考BodyHandler
                isForm = StringUtils.isNotBlank(contentType) && (contentType.startsWith("multipart/form-data")
                        || contentType.startsWith("application/x-www-form-urlencoded"));
                if (isForm) {
                    FormParserFactory.Builder builder = FormParserFactory.builder();
                    builder.setDefaultCharset(Constants.DEFAULT_CHARACTER);
                    FormParserFactory formParserFactory = builder.build();
                    // MultiPartParserDefinition#93，exchange.addExchangeCompleteListener，在请求结束时关闭parser并删除临时文件
                    FormDataParser parser = formParserFactory.createParser(exchange);
                    if (parser != null) {
                        FormData formData = parser.parseBlocking();
                        for (String name : formData) {
                            Deque<FormValue> deque = formData.get(name);
                            if (deque.size() > 1) {
                                List<Object> strings = new LinkedList<>();
                                List<Object> list = new LinkedList<>();
                                for (FormValue formValue : deque) {
                                    strings.add(
                                            formValue.isFileItem() ? formValue.getFileName() : formValue.getValue());
                                    list.add(formValue.isFileItem() ? formValue : formValue.getValue());
                                }
                                body.put(name, strings);
                                others.add(new Tuple<>(name, list));
                            } else {
                                FormValue formValue = deque.getFirst();
                                body.put(name, formValue.isFileItem() ? formValue.getFileName() : formValue.getValue());
                                others.add(
                                        new Tuple<>(name, formValue.isFileItem() ? formValue : formValue.getValue()));
                            }
                        }
                    } else {
                        isForm = false;
                    }
                }
                if (isForm == false) {
                    InputStream inputStream = exchange.getInputStream();
                    String unparsedRequestBody = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                    if ((unparsedRequestBody = StringUtils.trimToEmpty(unparsedRequestBody)).isEmpty() == false) {
                        int leftP = unparsedRequestBody.indexOf('{');
                        if (leftP == 0) {
                            body.putAll(JsonMapper.string2Map(unparsedRequestBody));
                        } else if (leftP == -1 && unparsedRequestBody.indexOf('=') > 0) {
                            body.putAll(decodeParamMap(unparsedRequestBody,
                                    StandardCharsets.UTF_8));
                        } else {
                            others.add(new Tuple<>("BODYSTRING", unparsedRequestBody));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("fail to parse body", e);
            }
            if (!body.isEmpty() && log.isDebugEnabled()) {
                log.debug("body: {}", JsonMapper.toJson(body));
            }
            others.forEach(tuple -> body.put(tuple.first, tuple.second));
        }
        exchange.putAttachment(AttachmentConstants.REQUEST_BODY, body);
        return isForm;
    }

    public static Map<String, String> decodeParamMap(String paramsStr, Charset charset) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isNotBlank(paramsStr)) {
            String[] params = paramsStr.split("[&]");
            for (String param : params) {
                String[] split = param.split("[=]");
                String name = Util.urlDecode(split[0]);
                String value = Util.urlDecode(split[1]);
                map.put(name, value);
            }
        }
        return map;
    }

    private void handleMissingHandler(HttpServerExchange exchange, String serviceId) {
        log.error("Handler is not found for serviceId " + serviceId);
        Status status = new Status(STATUS_HANDLER_NOT_FOUND, serviceId);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
    }
}
