package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.xlongwei.petstore.model.Pet;
import com.xlongwei.petstore.utility.Utils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsPetIdPutHandler implements LightHttpHandler {
    static String error = "{\"error\":\"%s\"}";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(200);
        String petId = Utils.getPathParameter(exchange, "petId");
        Pet pet = JsonMapper.fromJson(JsonMapper.toJson(exchange.getAttachment(AttachmentConstants.REQUEST_BODY)),
                Pet.class);
        try (Connection connection = Utils.ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("update pet set name=?,tag=? where id=?")) {
            statement.setString(1, pet.getName());
            if (pet.getTag() == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, pet.getTag());
            }
            statement.setInt(3, Integer.parseInt(petId));
            int update = statement.executeUpdate();
            if (update >= 1) {
                exchange.endExchange();
            } else {
                exchange.getResponseSender().send(String.format(error, "查无数据"));
            }
        }
    }
}
