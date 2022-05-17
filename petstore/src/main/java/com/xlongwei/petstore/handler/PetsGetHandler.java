package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.xlongwei.petstore.model.Pet;
import com.xlongwei.petstore.utility.Utils;

import org.apache.commons.lang3.StringUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsGetHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String limit = StringUtils.defaultIfBlank(Utils.getQueryParameter(exchange, "limit"), "100");
        String maxId = Utils.getQueryParameter(exchange, "maxId");
        if (maxId != null && !maxId.matches("\\d+")) {
            maxId = null;
        }
        try (Connection connection = Utils.ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        maxId == null ? "select * from pet order by id desc limit ?"
                                : "select * from pet where id<? order by id desc limit ?")) {
            if (maxId == null) {
                statement.setInt(1, Integer.parseInt(limit));
            } else {
                statement.setInt(1, Integer.parseInt(maxId));
                statement.setInt(2, Integer.parseInt(limit));
            }
            ResultSet resultSet = statement.executeQuery();
            List<Pet> pets = new ArrayList<>();
            while (resultSet.next()) {
                Pet pet = new Pet();
                pet.setId(resultSet.getLong("id"));
                pet.setName(resultSet.getString("name"));
                pet.setTag(resultSet.getString("tag"));
                pets.add(pet);
            }
            if (!pets.isEmpty()) {
                exchange.getResponseHeaders().add(new HttpString("x-next"),
                        "/v1/pets?maxId=" + pets.get(pets.size() - 1).getId() + "&limit=" + limit);
            }
            exchange.getResponseSender().send(JsonMapper.toJson(pets));
        }
    }
}
