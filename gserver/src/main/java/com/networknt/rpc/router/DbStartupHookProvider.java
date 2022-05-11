package com.networknt.rpc.router;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbStartupHookProvider implements StartupHookProvider {
    public static final String DATA_SOURCE = "datasource";
    public static Map<String, DataSource> dbMap = new HashMap<>();

    @Override
    public void onStartup() {
        Map<String, Object> dataSourceMap = (Map<String, Object>) Config.getInstance().getJsonMapConfig(DATA_SOURCE);
        // iterate all db config
        dataSourceMap.forEach((k, v) -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(((Map<String, String>) v).get("jdbcUrl"));
            ds.setUsername(((Map<String, String>) v).get("username"));
            ds.setPassword(((Map<String, String>) v).get("password"));
            Map<String, ?> configParams = (Map<String, ?>) ((Map<String, Object>) v).get("parameters");
            configParams.forEach((p, q) -> ds.addDataSourceProperty(p, String.valueOf(q)));
            dbMap.put(k, ds);
        });
        // test all db
        dbMap.forEach((db, ds) -> {
            try (Connection conn = ds.getConnection()) {
                log.info("datasource={} is ok", db);
            } catch (Exception e) {
                log.warn("fail to connect datasource={}", db, e);
            }
        });
    }
}
