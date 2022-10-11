package com.xlongwei.apijson;

import apijson.RequestMethod;

public class DemoParser extends apijson.demo.DemoParser {
    public DemoParser() {
        super();
    }

    public DemoParser(RequestMethod method) {
        super(method);
    }

    public DemoParser(RequestMethod method, boolean needVerify) {
        super(method, needVerify);
    }

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
