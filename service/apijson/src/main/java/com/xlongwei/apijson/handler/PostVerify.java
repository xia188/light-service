package com.xlongwei.apijson.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;
import com.xlongwei.apijson.DemoController;
import com.xlongwei.apijson.DemoParser;
import com.xlongwei.apijson.DemoVerifier;

import apijson.RequestMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/apijson/postVerify/0.0.1")
public class PostVerify extends Abstract {

    private JSONObject requestConfig = new JSONObject().fluentPut("MUST", "type,phone").fluentPut("VERIFY",
            new JSONObject().fluentPut("type&{}", ">=0,<=4").fluentPut("phone~", "PHONE"));

    @Override
    public String handle(String request, HttpSession session) {
        return postVerify(JSON.parseObject(request), session).toJSONString();
    }

    private JSONObject postVerify(JSONObject request, HttpSession session) {
        try {
            // 校验请求参数
            DemoVerifier.verifyRequest(RequestMethod.POST, "postVerify", requestConfig,
                    request, DemoController.APIJSON_CREATOR);
            int type = request.getIntValue("type");
            long phone = request.getLongValue("phone");
            int verify = RandomUtils.nextInt(1000, 9999);
            log.info("type={} phone={} verify={}", type, phone, verify);
            // 直接插入数据，效率更高；方案1：删除，插入；方案2：查询，插入或更新
            try (Connection conn = DemoApplication.ds.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("insert into verify(type,phone,verify,date) values(?,?,?,?)")) {
                ps.setInt(1, type);
                ps.setLong(2, phone);
                ps.setInt(3, verify);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
            return DemoParser.extendSuccessResult(new JSONObject().fluentPut("verify", verify));
        } catch (Exception e) {
            return DemoParser.newErrorResult(e);
        }
    }

}
