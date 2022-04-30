package com.networknt.rpc.router;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;

import org.apache.commons.lang3.StringUtils;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.FormData.FormValue;

/**
 * /hybrid/{service}/{action}?host=&version=
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

        public static SimpleModule simpleModule = new SimpleModule();
        static {
                simpleModule.addSerializer(FormValue.class, new FormValueSerializer());
                JsonMapper.objectMapper.registerModule(simpleModule);
                Config.getInstance().getMapper().registerModule(simpleModule);
        }

        public static class FormValueSerializer extends StdSerializer<FormValue> {

                protected FormValueSerializer() {
                        super(FormValue.class);
                }

                @Override
                public void serialize(FormValue value, JsonGenerator gen, SerializerProvider provider)
                                throws IOException {
                        if (value.isFileItem()) {
                                gen.writeStartObject();
                                gen.writeStringField("fileName", value.getFileName());
                                gen.writeNumberField("fileSize", value.getFileItem().getFileSize());
                                gen.writeEndObject();
                        } else {
                                gen.writeString(value.getValue());
                        }
                }

        }
}
