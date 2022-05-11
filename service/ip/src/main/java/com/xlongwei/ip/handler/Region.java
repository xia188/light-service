
package com.xlongwei.ip.handler;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;
import com.xlongwei.ip.ThrowingFunction;

import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/ip/region/0.0.1")
public class Region implements HybridHandler {
    static Map<String, Object> ipConfig = Config.getInstance().getJsonMapConfig("ip");
    static DbSearcher dbSearcher = null;
    static ThrowingFunction<String, DataBlock> dbSearch = null;

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String ip = HybridUtils.getParam(exchange, "ip");
        boolean addIp = false;
        if (StringUtils.isBlank(ip)) {
            ip = HybridUtils.getParam(exchange, "showapi_userIp");
            if (StringUtils.isBlank(ip)) {
                ip = Region.getIp(exchange);
            }
            addIp = true;
        }
        Map<String, String> map = searchToMap(ip);
        if (addIp) {
            map.put("ip", ip);
        }
        return HybridUtils.toByteBuffer(JsonMapper.toJson(map));
    }

    public static Map<String, String> searchToMap(String ip) {
        Map<String, String> map = new HashMap<>();
        DataBlock dataBlock = search(ip);
        if (dataBlock != null) {
            // 国家，区域，省份，城市，运营商
            String region = dataBlock.getRegion();
            if (StringUtils.isNotBlank(region)) {
                String[] split = StringUtils.split(region, '|');
                int idx = 0;
                map.put("country", zeroToEmpty(split[idx++]));
                map.put("area", zeroToEmpty(split[idx++]));
                map.put("state", zeroToEmpty(split[idx++]));
                map.put("city", zeroToEmpty(split[idx++]));
                map.put("isp", zeroToEmpty(split[idx++]));
                map.put("region", String.join(StringUtils.EMPTY, map.values()));
            }
        }
        return map;
    }

    public static DataBlock search(String ip) {
        if (Util.isIpAddress(ip)) {
            synchronized (dbSearcher) {
                try {
                    return dbSearch.apply(ip);
                } catch (Exception e) {
                    log.warn("fail to search ip: {}, ex: {}", ip, e.getMessage());
                }
            }
        }
        return null;
    }

    /** 获取真实IP地址 */
    public static String getIp(HttpServerExchange exchange) {
        String[] ipHeaders = { "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP", "WL-Proxy-Client-IP", "Proxy-Client-IP",
                "X-Forwarded-For", "X-Real-IP" };
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        for (String ipHeader : ipHeaders) {
            String ipValue = requestHeaders.getFirst(ipHeader);
            if (!StringUtils.isBlank(ipValue) && !"unknown".equalsIgnoreCase(ipValue)) {
                int common = ipValue.indexOf(',');
                if (common > -1) {
                    // clientip,proxy1,proxy2
                    ipValue = ipValue.substring(0, common);
                }
                if (Util.isIpAddress(ipValue) && !"127.0.0.1".equals(ipValue)) {
                    return ipValue;
                }
            }
        }
        return exchange.getSourceAddress().getAddress().getHostAddress();
    }

    private static String zeroToEmpty(String value) {
        return value == null || "0".equals(value) ? StringUtils.EMPTY : value;
    }

    @Override
    public void init() {
        try {
            DbConfig config = new DbConfig();
            String search = (String) ipConfig.get("search");
            String dbFile = (String) ipConfig.get("dbFile");
            dbSearcher = new DbSearcher(config, dbFile);
            log.info("DbSearcher init success, search={} dbFile={}", search, dbFile);
            switch (search) {
                case "memory":
                    dbSearch = dbSearcher::memorySearch;
                case "binary":
                    dbSearch = dbSearcher::binarySearch;
                case "btree":
                default:
                    dbSearch = dbSearcher::btreeSearch;
            }
        } catch (Exception e) {
            log.warn("fail to init DbSearcher: {}", e.getMessage());
        }
    }
}
