package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;

import javax.sql.DataSource;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.service.SingletonServiceFactory;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsPetIdDeleteHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String petId = exchange.getPathParameters().get("petId").getFirst();
        try (Connection connection = SingletonServiceFactory.getBean(DataSource.class).getConnection();
                PreparedStatement statement = connection.prepareCall("delete from pet where id=?")) {
            statement.setLong(1, Long.valueOf(petId));
            int executeUpdate = statement.executeUpdate();
            exchange.getResponseSender()
                    .send(JsonMapper.toJson(Collections.singletonMap("delete", executeUpdate >= 1)));
        }
    }
}
