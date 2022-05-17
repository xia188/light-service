
package com.xlongwei.bank.handler;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.DbStartupHookProvider;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;
import com.xlongwei.bank.BankUtil;

import org.apache.commons.lang3.BooleanUtils;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/bank/card/0.0.1")
public class Card implements HybridHandler {
    static DataSource ds = null;
    static boolean cardBin = false;
    static String queryEqualCardBin = "select cardBin,issuerCode as bankId,issuerName as bankName,cardName,cardDigits,cardType,bankCode,bankName as bankName2 from bank_card where cardBin=?";
    static String queryLikeCardBin = "select cardBin,issuerCode as bankId,issuerName as bankName,cardName,cardDigits,cardType,bankCode,bankName as bankName2 from bank_card where instr(?,cardBin)=1 order by length(cardBin) desc limit 1";

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String bankCardNumber = HybridUtils.getParam(exchange, "bankCardNumber");
        if (StringUtils.isNotBlank(bankCardNumber) && bankCardNumber.matches("\\d{2,}")) {
            String queryParam = null, querySql = null;
            if (cardBin) {
                String cardBin = BankUtil.cardBin(bankCardNumber);
                if (StringUtils.isNotBlank(cardBin)) {
                    queryParam = cardBin;
                    querySql = queryEqualCardBin;
                }
            } else {
                queryParam = bankCardNumber;
                querySql = queryLikeCardBin;
            }
            if (queryParam != null) {
                try (Connection connection = ds.getConnection();
                        PreparedStatement statement = connection.prepareStatement(querySql)) {
                    statement.setString(1, bankCardNumber);
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        Map<String, String> map = new HashMap<>(16);
                        map.put("valid", Boolean.toString(BankUtil.isBankCardNumber(bankCardNumber)));
                        map.put("cardBin", resultSet.getString("cardBin"));
                        map.put("bankId", resultSet.getString("bankId"));
                        map.put("bankName", resultSet.getString("bankName"));
                        map.put("cardName", resultSet.getString("cardName"));
                        map.put("cardDigits", resultSet.getString("cardDigits"));
                        map.put("cardType", resultSet.getString("cardType"));
                        map.put("bankCode", resultSet.getString("bankCode"));
                        map.put("bankName2", resultSet.getString("bankName2"));
                        return HybridUtils.toByteBuffer(JsonMapper.toJson(map));
                    }
                } catch (Exception e) {
                    return HybridUtils
                            .toByteBuffer(JsonMapper.toJson(Collections.singletonMap("error", e.getMessage())));
                }
            }
        }
        return HybridUtils
                .toByteBuffer(JsonMapper.toJson(Collections.singletonMap("error", "查无数据")));
    }

    @Override
    public void init() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig("bank");
        ds = DbStartupHookProvider.dbMap.get(config.get("ds"));
        cardBin = BooleanUtils.toBooleanObject(config.get("cardBin").toString());
        if (cardBin) {
            try (Connection connection = ds.getConnection();
                    PreparedStatement statement = connection.prepareStatement("select cardBin from bank_card");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BankUtil.addBin(resultSet.getString("cardBin"));
                }
                resultSet.last();
                log.info("bank_card={} is ok", resultSet.getRow());
            } catch (Exception e) {
                log.warn("fail to load bank_card", e);
            }
        } else {
            log.info("bank_card={} is ok", false);
        }
    }
}
