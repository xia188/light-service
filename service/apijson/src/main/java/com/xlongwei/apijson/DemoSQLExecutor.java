package com.xlongwei.apijson;

import java.sql.Connection;

import javax.sql.DataSource;

import apijson.Log;
import apijson.framework.APIJSONSQLExecutor;
import apijson.orm.SQLConfig;

public class DemoSQLExecutor extends APIJSONSQLExecutor {
	public static final String TAG = "DemoSQLExecutor";

	@Override
	public Connection getConnection(SQLConfig config) throws Exception {
		Log.d(TAG, "getConnection  config.getDatasource() = " + config.getDatasource());

		Connection c = connectionMap.get(config.getDatabase());
		if (c == null || c.isClosed()) {
			try {
				DataSource ds = DemoApplication.ds;
				String connectionKey = config.getDatasource() + "-" + config.getDatabase();
				// demo使用的是config.getDatabase()和super.getConnection(config)对应不上
				connectionMap.put(connectionKey, ds == null ? null : ds.getConnection());
			} catch (Exception e) {
				Log.e(TAG, "getConnection   try { "
						+ "DataSource ds = DemoApplication.getApplicationContext().getBean(DataSource.class); .."
						+ "} catch (Exception e) = " + e.getMessage());
			}
		}

		// 必须最后执行 super 方法，因为里面还有事务相关处理。
		// 如果这里是 return c，则会导致 增删改 多个对象时只有第一个会 commit，即只有第一个对象成功插入数据库表
		return super.getConnection(config);
	}
}
