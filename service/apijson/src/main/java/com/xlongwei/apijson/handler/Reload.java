package com.xlongwei.apijson.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.networknt.rpc.router.ServiceHandler;
import com.xlongwei.apijson.DemoApplication;
import com.xlongwei.apijson.DemoController;
import com.xlongwei.apijson.DemoParser;
import com.xlongwei.apijson.DemoVerifier;

import apijson.RequestMethod;
import apijson.orm.exception.ConditionErrorException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/apijson/reload/0.0.1")
public class Reload extends Abstract {

    JSONObject requestConfig = new JSONObject()
            .fluentPut("MUST", "type,phone,verify")
            .fluentPut("VERIFY",
                    new JSONObject()
                            .fluentPut("type{}", new JSONArray()
                                    .fluentAdd("ALL").fluentAdd("ACCESS").fluentAdd("FUNCTION").fluentAdd("REQUEST"))
                            .fluentPut("phone~", "PHONE")
                            .fluentPut("verify~", "\\d{4}"));

    long verifyMillis = TimeUnit.SECONDS.toMillis(NumberUtils.toLong(DemoApplication.config.get("verify").toString()));

    @Override
    public String handle(String request, HttpSession session) {
        return reload(JSON.parseObject(request), session).toJSONString();
    }

    private JSONObject reload(JSONObject request, HttpSession session) {
        try {
            // 校验请求参数
            DemoVerifier.verifyRequest(RequestMethod.POST, "reload", requestConfig,
                    request, DemoController.APIJSON_CREATOR);
            // 校验验证码
            long phone = request.getLongValue("phone");
            int verify = request.getIntValue("verify");
            boolean verifySuccess = false;
            Timestamp verifyTime = null, gateTime = new Timestamp(System.currentTimeMillis() - verifyMillis);
            try (Connection conn = DemoApplication.ds.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement(
                                    "select date from verify where type=? and phone=? and verify=? and date>? ")) {
                ps.setInt(1, 4);
                ps.setLong(2, phone);
                ps.setInt(3, verify);
                ps.setTimestamp(4, gateTime);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    verifySuccess = true;
                    verifyTime = rs.getTimestamp(1);
                    // 校验通过时，清除过期验证码；也可以使用定时任务，或定时存储过程，或手动清理
                    ps.execute("delete from verify where date<'"
                            + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(gateTime) + "'");
                }
                rs.close();
            }
            String type = request.getString("type");
            log.info("type={} reload={} verify={} date={}", type, verifySuccess, verify,
                    (verifyTime == null ? StringUtils.EMPTY
                            : DateFormatUtils.ISO_8601_EXTENDED_TIME_FORMAT.format(verifyTime)));
            if (verifySuccess) {
                return DemoParser.extendSuccessResult(DemoApplication.apijson.reload(type));
            } else {
                return DemoParser.newErrorResult(new ConditionErrorException("手机号或验证码错误"));
            }
        } catch (Exception e) {
            return DemoParser.newErrorResult(e);
        }
    }
}
