
package com.xlongwei.apijson.handler;

import javax.servlet.http.HttpSession;

import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;

@ServiceHandler(id = "xlongwei.com/apijson/delete/0.0.1")
public class Delete extends Abstract {
    @Override
    public String handle(String request, HttpSession session) {
        return DemoApplication.apijson.delete(request, session);
    }
}
