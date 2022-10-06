package com.xlongwei.apijson;

import apijson.framework.APIJSONSQLConfig;

public class DemoSQLConfig extends APIJSONSQLConfig {

    static {
        DEFAULT_DATABASE = DATABASE_MYSQL;
        DEFAULT_SCHEMA = (String) DemoApplication.config.get("ds");
    }

    // AbstractSQLExecutor:194生成的SQL语句的表名带引号`
    @Override
    public boolean isMySQL() {
        return true;
    }

    // String connectionKey = config.getDatasource() + "-" + config.getDatabase();
    // 如果 DemoSQLExecutor.getConnection 能拿到连接池的有效 Connection，则这里不需要配置 dbVersion,
    // dbUri, dbAccount, dbPassword
    @Override
    public String getDatasource() {
        return DEFAULT_DATABASE;
    }

    @Override
    public String getDatabase() {
        return DEFAULT_SCHEMA;
    }

}
