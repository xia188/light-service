
package com.xlongwei.apijson.handler;

import com.networknt.rpc.router.ServiceHandler;

import apijson.RequestMethod;

@ServiceHandler(id = "xlongwei.com/apijson/headsByTag/0.0.1")
public class HeadsByTag extends ByTag {

    @Override
    public RequestMethod method() {
        return RequestMethod.HEADS;
    }

}
