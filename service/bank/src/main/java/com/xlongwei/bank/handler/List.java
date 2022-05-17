
package com.xlongwei.bank.handler;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.DbStartupHookProvider;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.NioUtils;
import com.networknt.utility.Tuple;

import org.apache.commons.lang3.StringUtils;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/bank/list/0.0.1")
public class List implements HybridHandler {
    static DataSource ds = null;
    static byte[] bankList = null;
    static boolean hasPinyin = false;

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String orderBy = StringUtils.defaultIfBlank(HybridUtils.getParam(exchange, "orderBy"), "bankName");
        if ("bankCode".equals(orderBy)) {
            try (Connection connection = ds.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "select distinct bankCode,bankName from bank_card where length(bankName)>0 order by bankCode")) {
                ResultSet resultSet = statement.executeQuery();
                Map<String, String> map = new LinkedHashMap<>(9868);
                while (resultSet.next()) {
                    String bankCode = resultSet.getString("bankCode");
                    String bankName = resultSet.getString("bankName");
                    map.put(bankCode, bankName);
                }
                return HybridUtils.toByteBuffer(JsonMapper.toJson(map));
            } catch (Exception e) {
                return HybridUtils
                        .toByteBuffer(JsonMapper.toJson(Collections.singletonMap("error", e.getMessage())));
            }
        } else if (hasPinyin && "pinyin".equals(orderBy)) {
            try (Connection connection = ds.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "select distinct bankCode,bankName from bank_card where length(bankName)>0")) {
                ResultSet resultSet = statement.executeQuery();
                java.util.List<Tuple<String, String>> list = new ArrayList<>();
                while (resultSet.next()) {
                    String bankCode = resultSet.getString("bankCode");
                    String bankName = resultSet.getString("bankName");
                    list.add(new Tuple<>(bankCode, bankName));
                }
                list.sort((a, b) -> {
                    String o1 = PinyinHelper.toPinyin(a.second, PinyinStyleEnum.INPUT, StringUtils.EMPTY);
                    String o2 = PinyinHelper.toPinyin(b.second, PinyinStyleEnum.INPUT, StringUtils.EMPTY);
                    return o1.compareTo(o2);
                });
                Map<String, String> map = new LinkedHashMap<>(9868);
                list.forEach(tuple -> {
                    map.put(tuple.first, tuple.second);
                });
                log.info("bankList.json={}", JsonMapper.toJson(map));
            } catch (Exception e) {
                return HybridUtils
                        .toByteBuffer(JsonMapper.toJson(Collections.singletonMap("error", e.getMessage())));
            }
        }
        return ByteBuffer.wrap(bankList);
    }

    @Override
    public void init() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig("bank");
        ds = DbStartupHookProvider.dbMap.get(config.get("ds"));
        try {
            InputStream is = List.class.getResourceAsStream("/bankList.json");
            bankList = NioUtils.toByteArray(is);
            log.info("bankList is ok");
        } catch (Exception e) {
            log.warn("fail to init bankList", e);
        }
        try {
            hasPinyin = PinyinHelper.class != null;
        } catch (Throwable e) {
        }
        log.info("hasPinyin={}", hasPinyin);
    }
}
