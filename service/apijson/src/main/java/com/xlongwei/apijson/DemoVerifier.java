package com.xlongwei.apijson;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwt.JwtClaims;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.security.JwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.utility.Constants;
import com.networknt.utility.HybridUtils;

import apijson.JSONResponse;
import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.demo.model.User;
import apijson.framework.APIJSONConstant;
import apijson.orm.JSONRequest;
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

    private static Cache<String, HttpSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(256)
            .build();

    // 这里可以通过jwt登录验证，缓存DemoSession
    public static HttpSession session() {
        // 判断是否有userId
        HttpServerExchange exchange = HybridUtils.exchange.get();
        if (exchange == null) {
            return null;
        }
        // 如果启用了JwtVerifyHandler（要求所有请求都带jwt，开放接口不启用），直接判断auditInfo即可
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        String userId = auditInfo == null ? null : (String) auditInfo.get(Constants.USER_ID_STRING);
        if (userId == null) {
            // 如果未启用JwtVerifyHandler，则直接解析jwt令牌
            HeaderMap headerMap = exchange.getRequestHeaders();
            String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
            String jwt = JwtVerifier.getJwtFromAuthorization(authorization);
            if (StringUtils.isBlank(jwt)) {
                return null;
            }
            try {
                JwtVerifier jwtVerifier = JwtVerifyHandler.jwtVerifier;
                JwtClaims claims = jwtVerifier.verifyJwt(jwt, false, true);
                userId = Objects.toString(claims.getClaimValue(Constants.USER_ID_STRING));
                auditInfo = new HashMap<>();
                auditInfo.put(Constants.USER_ID_STRING, userId);
                exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
            } catch (Exception e) {
                log.error("JwtException:", e);
                return null;
            }
        }
        // 没有用户信息，无会话：get请求无需会话，gets请求会由apijson框架验证权限
        if (userId == null || "null".equals(userId) || userId.isEmpty()) {
            return null;
        }
        // 从缓存获取会话，可以改造为redis，参考README.md里的session序列化
        HttpSession session = cache.getIfPresent(userId);
        if (session == null) {
            session = new DemoSession();
            cache.put(userId, session);
        }
        // 新会话手动登录
        Long id = (Long) session.getAttribute(APIJSONConstant.VISITOR_ID);
        if (id == null) {
            User user = new User();
            if (StringUtils.isNumeric(userId)) {
                user.setId(Long.parseLong(userId));
            } else {
                user.setName(userId);
            }
            JSONResponse response = new JSONResponse(
                    new DemoParser(RequestMethod.GETS, false).parseResponse(
                            new JSONRequest( // 兼容 MySQL 5.6 及以下等不支持 json 类型的数据库
                                    "User", // User 里在 setContactIdList(List<Long>) 后加 setContactIdList(String) 没用
                                    new apijson.JSONObject( // fastjson 查到一个就不继续了，所以只能加到前面或者只有这一个，但这样反过来不兼容 5.7+
                                            user // 所以就用 @json 来强制转为 JSONArray，保证有效
                                    ).setJson("contactIdList,pictureList").setCombine("")).setFormat(true)));
            user = response.getObject(User.class);
            if (user == null) {
                session.setAttribute(APIJSONConstant.VISITOR_ID, 0);
                return null;
            }
            DemoApplication.apijson.login(session, user, null, null, null);
            id = user.getId();
        }
        // id为正表示登录成功
        return id != null && id > 0 ? session : null;
    }

}
