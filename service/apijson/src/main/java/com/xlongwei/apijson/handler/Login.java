
package com.xlongwei.apijson.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpSession;

import org.jose4j.jwt.JwtClaims;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.security.JwtIssuer;
import com.networknt.utility.Constants;
import com.xlongwei.apijson.DemoApplication;
import com.xlongwei.apijson.DemoController;
import com.xlongwei.apijson.DemoParser;
import com.xlongwei.apijson.DemoVerifier;

import apijson.RequestMethod;
import apijson.orm.exception.ConditionErrorException;

@ServiceHandler(id = "xlongwei.com/apijson/login/0.0.1")
public class Login extends Abstract {
    private JSONObject requestConfig = new JSONObject().fluentPut("MUST", "type,phone,password").fluentPut("VERIFY",
            new JSONObject().fluentPut("type&{}", ">=0,<=1").fluentPut("phone~", "PHONE").fluentPut("password~",
                    "^.{6,20}$"));

    @Override
    public String handle(String request, HttpSession session) {
        return login(JSON.parseObject(request), session).toJSONString();
    }

    private JSONObject login(JSONObject request, HttpSession session) {
        try {
            // 校验请求参数
            DemoVerifier.verifyRequest(RequestMethod.POST, "login", requestConfig,
                    request, DemoController.APIJSON_CREATOR);
            int type = request.getIntValue("type");// 登录方式，非必须 0-密码 1-验证码
            long phone = request.getLongValue("phone");
            String password = request.getString("password");// 密码或验证码
            // 校验验证码或密码
            if (type == 1) {

            }
            // 查询用户信息
            long id = 0;
            try (Connection conn = DemoApplication.ds.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "select id from apijson_privacy where phone=?" + (type == 0 ? " and _password=?" : ""))) {
                ps.setLong(1, phone);
                if (type == 0) {
                    ps.setString(2, password);
                }
                ResultSet rs = ps.executeQuery();
                id = rs.next() ? rs.getLong(1) : 0;
            }
            if (id <= 0) {
                return DemoParser.newErrorResult(new ConditionErrorException("登录失败"));
            }
            // 返回jwt令牌
            JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
            claims.setClaim(Constants.USER_ID_STRING, String.valueOf(id));
            String accessToken = JwtIssuer.getJwt(claims);
            return DemoParser.extendSuccessResult(new JSONObject().fluentPut("access_token", "Bearer " + accessToken));
        } catch (Exception e) {
            return DemoParser.newErrorResult(e);
        }
    }
}
