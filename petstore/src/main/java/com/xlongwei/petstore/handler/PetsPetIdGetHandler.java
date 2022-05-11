package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.service.SingletonServiceFactory;
import com.xlongwei.petstore.model.Error;
import com.xlongwei.petstore.model.Pet;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsPetIdGetHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String petId = exchange.getPathParameters().get("petId").getFirst();
        try (Connection connection = SingletonServiceFactory.getBean(DataSource.class).getConnection();
                PreparedStatement statement = connection.prepareCall("select * from pet where id=?")) {
            statement.setLong(1, Long.valueOf(petId));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Pet pet = new Pet();
                pet.setId(resultSet.getLong("id"));
                pet.setName(resultSet.getString("name"));
                pet.setTag(resultSet.getString("tag"));
                exchange.getResponseSender().send(JsonMapper.toJson(pet));
            } else {
                Error error = new Error();
                error.setCode(500);
                error.setMessage("pet not found");
                exchange.getResponseSender().send(JsonMapper.toJson(error));
            }
        }
    }
}
