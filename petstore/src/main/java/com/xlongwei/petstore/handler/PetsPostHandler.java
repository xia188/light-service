package com.xlongwei.petstore.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.status.HttpStatus;
import com.networknt.utility.StringUtils;
import com.xlongwei.petstore.model.Pet;
import com.xlongwei.petstore.utility.Utils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PetsPostHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        exchange.setStatusCode(HttpStatus.OK.value());
        final Pet pet = JsonMapper.fromJson(JsonMapper.toJson(exchange.getAttachment(AttachmentConstants.REQUEST_BODY)),
                Pet.class);
        try (Connection connection = Utils.ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("insert into pet(name,tag) values(?,?)")) {
            statement.setString(1, pet.getName());
            statement.setString(2, StringUtils.trimToEmpty(pet.getTag()));
            statement.executeUpdate();
            exchange.getResponseSender().send(JsonMapper.toJson(pet));
        }
    }
}
