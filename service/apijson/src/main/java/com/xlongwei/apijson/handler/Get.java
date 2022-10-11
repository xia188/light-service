
package com.xlongwei.apijson.handler;

import javax.servlet.http.HttpSession;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;

@ServiceHandler(id = "xlongwei.com/apijson/get/0.0.1")
public class Get extends Abstract implements HybridHandler {
    @Override
    public String handle(String request, HttpSession session) {
        return DemoApplication.apijson.get(request, session);
    }
    
    @Override
    public void init() {
        DemoApplication.start();
    }
}
