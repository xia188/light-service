package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.xlongwei.petstore.utility.Utils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsPetIdDeleteHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String petId = Utils.getPathParameter(exchange, "petId");
        try (Connection connection = Utils.ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("delete from pet where id=?")) {
            statement.setLong(1, Long.valueOf(petId));
            int executeUpdate = statement.executeUpdate();
            exchange.getResponseSender()
                    .send(JsonMapper.toJson(Collections.singletonMap("delete", executeUpdate >= 1)));
        }
    }
}
