
package com.xlongwei.gservice.handler;

import java.io.InputStream;
import java.nio.ByteBuffer;

import com.networknt.config.JsonMapper;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData.FormValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/gservice/echo/0.0.1")
public class Echo implements Handler {
    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String json = JsonMapper.toJson(input);
        log.info("bodyMap: {}", json);
        FormValue file = HybridUtils.getFile(exchange, "file");
        if (file != null) {
            try (InputStream inputStream = file.getFileItem().getInputStream()) {
                byte[] buffer = new byte[1024];
                int length, total = 0;
                while ((length = inputStream.read(buffer)) != -1) {
                    total += length;
                }
                log.info("fileName={} fileSize={} totalRead={} path={}", file.getFileName(),
                        file.getFileItem().getFileSize(),
                        total, file.getFileItem().getFile());
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        return HybridUtils.toByteBuffer(json);
    }
}
