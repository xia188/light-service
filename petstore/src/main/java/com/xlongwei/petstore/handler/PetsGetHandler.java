package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.sql.DataSource;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.service.SingletonServiceFactory;
import com.xlongwei.petstore.model.Pet;

import org.apache.commons.lang3.math.NumberUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsGetHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        Deque<String> limitParam = exchange.getQueryParameters().get("limit");
        int limit = NumberUtils.toInt(limitParam == null ? "100" : limitParam.getFirst(), 100);
        try (Connection connection = SingletonServiceFactory.getBean(DataSource.class).getConnection();
                PreparedStatement statement = connection.prepareCall("select * from pet limit ?")) {
            statement.setInt(1, limit);
            ResultSet resultSet = statement.executeQuery();
            List<Pet> pets = new ArrayList<>();
            while (resultSet.next()) {
                Pet pet = new Pet();
                pet.setId(resultSet.getLong("id"));
                pet.setName(resultSet.getString("name"));
                pet.setTag(resultSet.getString("tag"));
                pets.add(pet);
            }
            exchange.getResponseSender().send(JsonMapper.toJson(pets));
        }
    }
}
