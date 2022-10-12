
package com.xlongwei.apijson.handler;

import com.networknt.rpc.router.ServiceHandler;

import apijson.RequestMethod;

@ServiceHandler(id = "xlongwei.com/apijson/headByTag/0.0.1")
public class HeadByTag extends ByTag {

    @Override
    public RequestMethod method() {
        return RequestMethod.HEAD;
    }

}
