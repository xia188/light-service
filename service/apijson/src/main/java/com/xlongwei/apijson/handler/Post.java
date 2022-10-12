
package com.xlongwei.apijson.handler;

import javax.servlet.http.HttpSession;

import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;

@ServiceHandler(id = "xlongwei.com/apijson/post/0.0.1")
public class Post extends Abstract {
    @Override
    public String handle(String request, HttpSession session) {
        return DemoApplication.apijson.post(request, session);
    }
}
