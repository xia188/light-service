
package com.xlongwei.apijson.handler;

import com.networknt.rpc.router.ServiceHandler;

import apijson.RequestMethod;

@ServiceHandler(id = "xlongwei.com/apijson/getByTag/0.0.1")
public class GetByTag extends ByTag {

    @Override
    public RequestMethod method() {
        return RequestMethod.GET;
    }

}
