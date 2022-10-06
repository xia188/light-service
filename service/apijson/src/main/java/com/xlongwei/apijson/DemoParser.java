package com.xlongwei.apijson;

public class DemoParser extends apijson.demo.DemoParser {
    // 可重写来设置最大查询数量
    @Override
    public int getMaxQueryCount() {
        return 1000;
    }

    @Override
    public int getMaxQueryPage() {
        return 10000;
    }
}
