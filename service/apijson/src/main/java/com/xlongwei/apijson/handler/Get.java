
package com.xlongwei.apijson.handler;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;

@ServiceHandler(id = "xlongwei.com/apijson/get/0.0.1")
public class Get extends Crud implements HybridHandler {
    @Override
    public void init() {
        DemoApplication.start();
    }
}
