package com.networknt.rpc.router;

import static com.networknt.rpc.router.JsonHandler.STATUS_HANDLER_NOT_FOUND;

import java.io.ByteArrayOutputStream;
import java.net.URI;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.ContentType;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.rpc.Handler;
import com.networknt.rpc.security.JwtVerifyHandler;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.server.Server;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.NioUtils;
import com.networknt.utility.StringUtils;
import com.networknt.utility.Util;

import org.xnio.OptionMap;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
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
        if (Methods.POST.equals(exchange.getRequestMethod())// create
                || Methods.PUT.equals(exchange.getRequestMethod())// update all
                || Methods.PATCH.equals(exchange.getRequestMethod())) {// update partial
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
                handleRequest(exchange, bodyMap);
            } else {
                exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
                    Map<String, Object> bodyMap = new HashMap<>();
                    parseBodyMessage(bodyMap, message);
                    parseQueryParameters(bodyMap, exchange);
                    handleRequest(exchange, bodyMap);
                }, StandardCharsets.UTF_8);
            }
        } else {
            Map<String, Object> bodyMap = new HashMap<>();
            parseQueryParameters(bodyMap, exchange);
            handleRequest(exchange, bodyMap);
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

    static List<String> rpcKeys = Arrays.asList("host", "service", "action", "version");

    @SuppressWarnings({ "unchecked" })
    private void handleRequest(HttpServerExchange exchange, Map<String, Object> bodyMap) {
        if (log.isInfoEnabled() && !bodyMap.isEmpty()) {
            log.info("bodyMap: {}", JsonMapper.toJson(bodyMap));
        }
        String serviceId = getServiceId(exchange.getRequestURI(), bodyMap);
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if (handler == null) {
            if (RpcStartupHookProvider.config.isRegisterService()) {
                Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
                String protocol = Server.getServerConfig().isEnableHttp() ? "http" : "https";
                String registerServiceId = serviceId.replace('/', '.');// host:path:port
                String tag = Server.getServerConfig().getEnvironment();
                String serviceToUrl = cluster.serviceToUrl(protocol, registerServiceId, tag, null);
                if (serviceToUrl != null) {
                    this.handleRegistryHandler(exchange, serviceToUrl, bodyMap);
                    return;
                }
            }
            this.handleMissingHandler(exchange, serviceId);
            return;
        }
        // calling jwt scope verification here. token signature and expiration are done
        verifyJwt(JwtVerifyHandler.config, serviceId, exchange);
        // 兼容JsonHandler取data进行验证和处理，处理HybridHandler数据时移除host、version，但保留data
        if (bodyMap.keySet().containsAll(rpcKeys)) {
            rpcKeys.forEach(bodyMap::remove);
            Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");
            if (data != null) {
                bodyMap.entrySet().forEach(entry -> {
                    if (!"data".equals(entry.getKey())) {
                        data.put(entry.getKey(), entry.getValue());
                    }
                });
                bodyMap = data;
            }
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

    static boolean isHttp2 = Server.getServerConfig().isEnableHttp2()
            && Server.getServerConfig().isEnableHttps();
    static OptionMap optionMap = isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)
            : OptionMap.EMPTY;

    private void handleRegistryHandler(HttpServerExchange exchange, String serviceToUrl, Map<String, Object> bodyMap) {
        Http2Client client = Http2Client.getInstance();
        ClientConnection connection = null;
        try {
            URI uri = new URI(serviceToUrl);
            connection = client.borrowConnection(uri, Http2Client.WORKER, client.getDefaultXnioSsl(),
                    Http2Client.BUFFER_POOL, optionMap).get();

            AtomicReference<ClientResponse> reference = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(exchange.getRequestURI());
            // client.propagateHeaders(request, exchange); cId可用于日志输出，在微服务之间关联请求日志
            propagateHeaders(request, exchange);
            // 缺少此行时报400错误
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            String json = JsonMapper.toJson(bodyMap);
            boolean hasFileUpload = bodyMap.values().stream().filter(value -> value instanceof FormValue).findAny()
                    .isPresent();
            log.info("hasFileUpload={} post={}", hasFileUpload, json);
            if (hasFileUpload) {
                String boundary = "gserver_fileupload";
                // 参考body模块BodyHandlerTest#testPostFormMultipart，以及hutool的MultipartBody，文件会读入内存可能影响性能
                request.getRequestHeaders().put(Headers.CONTENT_TYPE,
                        "multipart/form-data; boundary=" + boundary);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writeBodyMap(baos, bodyMap, boundary);
                connection.sendRequest(request,
                        client.byteBufferClientCallback(reference, latch, ByteBuffer.wrap(baos.toByteArray())));
            } else {
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
                // client.createClientCallback使用Charset.defaultCharset()，这里使用ByteBuffer传字节
                connection.sendRequest(request, client.byteBufferClientCallback(reference, latch,
                        ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8))));
            }

            latch.await();
            ClientResponse clientResponse = reference.get();
            exchange.setStatusCode(clientResponse.getResponseCode());
            String response = null; // clientResponse.getAttachment(Http2Client.RESPONSE_BODY);
            ByteBuffer buffer = clientResponse.getAttachment(Http2Client.BUFFER_BODY);
            response = buffer == null ? null : new String(buffer.array(), StandardCharsets.UTF_8);
            log.info("status={} response={}", clientResponse.getResponseCode(), response);
            if (response != null) {
                exchange.getResponseSender().send(response);
            } else {
                exchange.endExchange();
            }
        } catch (Exception e) {
            log.error("Exception:", e);
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send(e.getMessage());
        } finally {
            client.returnConnection(connection);
        }
    }

    private void propagateHeaders(ClientRequest request, HttpServerExchange exchange) {
        String tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String cid = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        if (tid != null) {
            request.getRequestHeaders().put(HttpStringConstants.TRACEABILITY_ID, tid);
        }
        if (token != null) {
            request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
        }
        if (cid != null) {
            request.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, cid);
        }
    }

    private void writeBodyMap(ByteArrayOutputStream baos, Map<String, Object> bodyMap, String boundary)
            throws Exception {
        for (Entry<String, Object> entry : bodyMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                writeBodyMapString(baos, entry.getKey(), (String) value, boundary);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object obj : list) {
                    if (obj instanceof String) {
                        writeBodyMapString(baos, entry.getKey(), (String) obj, boundary);
                    } else if (obj instanceof FormValue) {
                        writeBodyMapFile(baos, entry.getKey(), (FormValue) obj, boundary);
                    }
                }
            } else if (value instanceof FormValue) {
                writeBodyMapFile(baos, entry.getKey(), (FormValue) entry.getValue(), boundary);
            }
        }
        // 文件上传结束标记--boundary--\r\n
        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    }

    static byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private void writeBodyMapFile(ByteArrayOutputStream baos, String name, FormValue file, String boundary)
            throws Exception {
        StringBuilder sb = new StringBuilder("--" + boundary + "\r\n");
        sb.append("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + file.getFileName() + "\"\r\n");
        // Content-Type可选，hutool能根据扩展名匹配正确类型，默认二进制或不传也能成功
        // sb.append("Content-Type: application/octet-stream\r\n");
        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF);
        baos.write(NioUtils.toByteArray(file.getFileItem().getInputStream()));
        baos.write(CRLF);
    }

    private void writeBodyMapString(ByteArrayOutputStream baos, String name, String value, String boundary)
            throws Exception {
        StringBuilder sb = new StringBuilder("--" + boundary + "\r\n");
        sb.append("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        baos.write(CRLF);
        baos.write(value.getBytes());
        baos.write(CRLF);
    }

    private void parseQueryParameters(Map<String, Object> bodyMap, HttpServerExchange exchange) {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        if (params.containsKey("cmd")) {
            String cmd = params.remove("cmd").getFirst();
            parseBodyMessage(bodyMap, cmd);
        }
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
            String[] params = StringUtils.split(paramsStr, '&');
            for (String param : params) {
                String[] split = StringUtils.split(param, '=');
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
