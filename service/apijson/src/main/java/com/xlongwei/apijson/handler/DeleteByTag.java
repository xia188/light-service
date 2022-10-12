
package com.xlongwei.apijson.handler;

import com.networknt.rpc.router.ServiceHandler;

import apijson.RequestMethod;

@ServiceHandler(id = "xlongwei.com/apijson/deleteByTag/0.0.1")
public class DeleteByTag extends ByTag {

    @Override
    public RequestMethod method() {
        return RequestMethod.DELETE;
    }

}
