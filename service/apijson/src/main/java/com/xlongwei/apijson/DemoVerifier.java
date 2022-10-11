package com.xlongwei.apijson;

import java.util.HashMap;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import com.networknt.exception.ExpiredTokenException;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.security.JwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.utility.Constants;
import com.networknt.utility.HybridUtils;

import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.orm.SQLConfig;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class DemoVerifier extends apijson.demo.DemoVerifier {

    static {
        COMPILE_MAP.put("PHONE", StringUtil.PATTERN_PHONE);
        COMPILE_MAP.put("EMAIL", StringUtil.PATTERN_EMAIL);
        COMPILE_MAP.put("ID_CARD", StringUtil.PATTERN_ID_CARD);
    }

    // 这里可以自定义登录验证，避开HttpSession
    @Override
    public void verifyLogin() throws Exception {
        HttpServerExchange exchange = HybridUtils.exchange.get();
        if (exchange != null) {
            // 如果启用了JwtVerifyHandler，直接判断auditInfo即可
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            String clientId = auditInfo == null ? null : (String) auditInfo.get(Constants.CLIENT_ID_STRING);
            String userId = auditInfo == null ? null : (String) auditInfo.get(Constants.USER_ID_STRING);
            if (clientId != null || userId != null) {
                return;
            }
            // 如果未启用JwtVerifyHandler，则直接解析jwt令牌
            HeaderMap headerMap = exchange.getRequestHeaders();
            String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
            String jwt = JwtVerifier.getJwtFromAuthorization(authorization);
            if (jwt != null) {
                try {
                    JwtVerifier jwtVerifier = JwtVerifyHandler.jwtVerifier;
                    JwtClaims claims = jwtVerifier.verifyJwt(jwt, false, true);
                    auditInfo = new HashMap<>();
                    auditInfo.put(Constants.ENDPOINT_STRING, exchange.getRequestURI());
                    auditInfo.put(Constants.CLIENT_ID_STRING, claims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                    auditInfo.put(Constants.USER_ID_STRING, claims.getStringClaimValue(Constants.USER_ID_STRING));
                    auditInfo.put(Constants.SCOPE_STRING,
                            claims.getStringListClaimValue(Constants.SCOPE_STRING).toString().replaceAll("\\s+", ""));
                    auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                    exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                    return;
                } catch (InvalidJwtException e) {
                    log.error("InvalidJwtException:", e);
                } catch (ExpiredTokenException e) {
                    log.error("ExpiredTokenException", e);
                }
            }
        }
        super.verifyLogin();
    }

    @Override
    public void verifyRole(SQLConfig config, String table, RequestMethod method, String role) throws Exception {
        HttpServerExchange exchange = HybridUtils.exchange.get();
        if (exchange != null) {
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            String userId = auditInfo == null ? null : (String) auditInfo.get(Constants.USER_ID_STRING);
            if (userId != null) {
                return;
            }
        }
        super.verifyRole(config, table, method, role);
    }

}
