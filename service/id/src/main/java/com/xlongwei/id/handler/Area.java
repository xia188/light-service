
package com.xlongwei.id.handler;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.DbStartupHookProvider;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/id/area/0.0.1")
public class Area implements HybridHandler {
    static DataSource ds = null;
    static int year = 2020;

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String area = HybridUtils.getParam(exchange, "area");
        Map<String, Object> map = new HashMap<>();
        Map<String, String> provinces = new LinkedHashMap<>();
        Map<String, String> cities = new LinkedHashMap<>();
        Map<String, String> counties = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("select code,name from idcard where year=?");
        if (StringUtils.isBlank(area)) {
            sql.append(" and code like '__0000'");
            map.put("provinces", provinces);
        } else if (area.matches("\\d{2}")) {
            sql.append(" and code like '" + area + "__00'");
            map.put("cities", cities);
        } else if (area.matches("\\d{4}")) {
            sql.append(" and code like '"
                    + area.substring(0, 4) + "__'");
            map.put("counties", counties);
        } else if (area.matches("\\d{6}")) {
            sql.append(" and (code like '__0000' or code like '" + area.substring(0, 2) + "__00'" + " or code like '"
                    + area.substring(0, 4) + "__')");
            map.put("provinces", provinces);
            map.put("cities", cities);
            map.put("counties", counties);
        }
        areas(sql.toString()).forEach((code, name) -> {
            if (code.endsWith("0000")) {
                provinces.put(code.substring(0, 2), name);
            } else if (code.endsWith("00")) {
                cities.put(code.substring(0, 4), name);
            } else {
                counties.put(code, name);
            }
        });
        return HybridUtils.toByteBuffer(JsonMapper.toJson(map));
    }

    private Map<String, String> areas(String sql) {
        Map<String, String> areas = new LinkedHashMap<>();
        try (Connection connection = ds.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement(sql)) {
            statement.setInt(1, year);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String code = resultSet.getString("code");
                String name = resultSet.getString("name");
                areas.put(code, name);
            }
        } catch (Exception e) {
            log.warn("fail to query {}", sql, e);
        }
        return areas;
    }

    @Override
    public void init() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig("id");
        ds = DbStartupHookProvider.dbMap.get(config.get("ds"));
        try (Connection connection = ds.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("select max(year) from idcard")) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                year = resultSet.getInt(1);
            }
            log.info("idcard={} is ok", year);
        } catch (Exception e) {
            log.warn("fail to load idcard", e);
        }
    }
}
