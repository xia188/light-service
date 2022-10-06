package com.xlongwei.apijson;

import java.util.Map;

import javax.sql.DataSource;

import com.networknt.config.Config;
import com.networknt.rpc.router.DbStartupHookProvider;

import apijson.Log;
import apijson.framework.APIJSONApplication;
import apijson.framework.APIJSONCreator;
import apijson.orm.FunctionParser;
import apijson.orm.Parser;
import apijson.orm.SQLConfig;
import apijson.orm.SQLExecutor;
import apijson.orm.Verifier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoApplication {
    public static final Map<String, Object> config = Config.getInstance().getJsonMapConfig("apijson");
    public static final boolean apijsonEnabled = (Boolean) config.get("enabled");
    public static final DemoController apijson = new DemoController();
    public static final DataSource ds = DbStartupHookProvider.dbMap.get(config.get("ds"));
    public static final String badRequest = "{\"error\":\"bad request\"}";

    public static void start() {
        log.info("apijson.enabled={}", apijsonEnabled);
        if (apijsonEnabled) {
            APIJSONApplication.DEFAULT_APIJSON_CREATOR = new APIJSONCreator<Long>() {
                @Override
                public Parser<Long> createParser() {
                    return new DemoParser();
                }

                @Override
                public FunctionParser createFunctionParser() {
                    return new DemoFunctionParser();
                }

                @Override
                public Verifier<Long> createVerifier() {
                    return new DemoVerifier();
                }

                @Override
                public SQLConfig createSQLConfig() {
                    return new DemoSQLConfig();
                }

                @Override
                public SQLExecutor createSQLExecutor() {
                    return new DemoSQLExecutor();
                }
            };
            Log.DEBUG = (Boolean) config.get("debug");
            log.info("apijson.debug={}", Log.DEBUG);
            try {
                APIJSONApplication.init(false);
            } catch (Exception e) {
                log.warn("fail to start apijson: {}", e.getMessage());
            }
        }
    }
}
