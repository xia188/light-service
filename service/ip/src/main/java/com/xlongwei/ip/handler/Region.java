
package com.xlongwei.ip.handler;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;
import com.xlongwei.ip.Util;

import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/ip/region/0.0.1")
public class Region implements Handler {
    static Map<String, Object> ipConfig = Config.getInstance().getJsonMapConfig("ip");
    static DbSearcher dbSearcher = null;

    static {
        try {
            DbConfig config = new DbConfig();
            String dbFile = (String) ipConfig.get("dbFile");
            dbSearcher = new DbSearcher(config, dbFile);
            log.info("DbSearcher init success");
        } catch (Exception e) {
            log.warn("fail to init DbSearcher: {}", e.getMessage());
        }
    }

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String ip = HybridUtils.getParam(exchange, "ip");
        boolean addIp = false;
        if (StringUtils.isBlank(ip)) {
            ip = HybridUtils.getParam(exchange, "showapi_userIp");
            if (StringUtils.isBlank(ip)) {
                ip = Util.getIp(exchange);
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
        DataBlock dataBlock = search(ip);
        if (dataBlock != null) {
            // 国家，区域，省份，城市，运营商
            String region = dataBlock.getRegion();
            if (StringUtils.isNotBlank(region)) {
                Map<String, String> map = new LinkedHashMap<>(8);
                String[] split = region.split("[|]", 5);
                int idx = 0;
                map.put("country", zeroToEmpty(split[idx++]));
                map.put("area", zeroToEmpty(split[idx++]));
                map.put("state", zeroToEmpty(split[idx++]));
                map.put("city", zeroToEmpty(split[idx++]));
                map.put("isp", zeroToEmpty(split[idx++]));
                map.put("region", String.join(StringUtils.EMPTY, map.values()));
                return map;
            }
        }
        return Collections.emptyMap();
    }

    public static DataBlock search(String ip) {
        if (Util.isIp(ip)) {
            synchronized (dbSearcher) {
                try {
                    switch ((String) ipConfig.get("search")) {
                        case "memory":
                            return dbSearcher.memorySearch(ip);
                        case "btree":
                            return dbSearcher.btreeSearch(ip);
                        case "binary":
                            return dbSearcher.binarySearch(ip);
                    }
                } catch (Exception e) {
                    log.warn("fail to search ip: {}, ex: {}", ip, e.getMessage());
                }
            }
        }
        return null;
    }

    private static String zeroToEmpty(String value) {
        return value == null || "0".equals(value) ? StringUtils.EMPTY : value;
    }
}
