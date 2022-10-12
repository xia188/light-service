
package com.xlongwei.apijson.handler;

import com.networknt.rpc.router.ServiceHandler;

import apijson.RequestMethod;

@ServiceHandler(id = "xlongwei.com/apijson/getsByTag/0.0.1")
public class GetsByTag extends ByTag {

    @Override
    public RequestMethod method() {
        return RequestMethod.GETS;
    }

}
