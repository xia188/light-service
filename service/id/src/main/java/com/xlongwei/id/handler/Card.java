
package com.xlongwei.id.handler;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.DbStartupHookProvider;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;
import com.networknt.utility.StringUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceHandler(id = "xlongwei.com/id/card/0.0.1")
public class Card implements HybridHandler {
    static DataSource ds = null;
    static FastDateFormat ymd = FastDateFormat.getInstance("yyyyMMdd");

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String idNumber = HybridUtils.getParam(exchange, "idNumber");
        Map<String, Object> params = new HashMap<>();
        params.put("valid", isIdNumber(idNumber));
        if (StringUtils.isNotBlank(idNumber)) {
            if (idNumber.length() > 15 && idNumber.length() != 18) {
                params.put("error", "身份证长度有误");
            } else {
                String area = idNumber.substring(0, Math.min(6, idNumber.length()));
                List<String> areas = areas(area);
                if (area.length() == 6) {
                    params.put("area", area);
                }
                if (!areas.isEmpty()) {
                    params.put("areas", String.join(StringUtils.EMPTY, areas));
                }
                boolean old = idNumber.length() == 15 && !"19".equals(idNumber.substring(6, 8));
                if ((old && idNumber.length() >= 12) || (!old && idNumber.length() >= 14)) {// 出生日期
                    String birth = old ? "19" + idNumber.substring(6, 12) : idNumber.substring(6, 14);
                    params.put("birth", birth);
                    params.put("year", birth.substring(0, 4));
                }
                if (params.containsKey("birth") && (old || idNumber.length() == 18)) {// 序号+性别
                    String serial = old ? idNumber.substring(12, 15)
                            : (idNumber.length() == 18 ? idNumber.substring(14, 17) : null);
                    if (serial != null && serial.matches("\\d+")) {
                        boolean male = Integer.parseInt(serial) % 2 == 1;
                        params.put("male", Boolean.toString(male));
                        params.put("sex", male ? "男" : "女");
                    }
                }
            }
        }
        return HybridUtils.toByteBuffer(JsonMapper.toJson(params));
    }

    /** 解析六位行政区划码 */
    public static List<String> areas(String area) {
        List<String> list = new ArrayList<>(3);
        if (area != null && area.length() >= 2) {
            String area1 = area.substring(0, 2) + "0000";
            String area2 = area.length() < 4 ? null : area.substring(0, 4) + "00";
            String area3 = area.length() < 6 ? null : area.substring(0, 6);
            try (Connection connection = ds.getConnection();
                    PreparedStatement statement = connection
                            .prepareStatement("select name from idcard where code in (?,?,?)")) {
                statement.setString(1, area1);
                statement.setString(2, area2);
                statement.setString(3, area3);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    list.add(resultSet.getString("name"));
                }
                log.info("bank_card={} is ok", resultSet.getRow());
            } catch (Exception e) {
                log.warn("fail to load bank_card", e);
            }
        }
        return list;
    }

    /**
     * 校验身份证号码是否合法
     * <li>idNumber=6位地址码+8位生日+3位序号+1位校验（15位身份证号年份19无校验位）
     * <li>地址码按GB/T2260规定到县
     * <li>生日年份在1900至今年之间
     * <li>校验位Y=sum(Ai*Wi)，Y=Vi[Y % 11]，Y=idNumber[17]
     */
    public static boolean isIdNumber(String idNumber) {
        int idLength1 = 15, idLength2 = 18, idLength = idNumber == null ? 0 : idNumber.length();
        boolean idType1 = idLength == idLength1, idType2 = idLength == idLength2;
        if (idNumber == null || (!idType1 && !idType2)) {
            return false;
        }

        final char[] cs = idNumber.toUpperCase().toCharArray();
        // 校验位数 Y = sum(Ai*Wi) Wi: 7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4 2
        int power = 0;
        for (int i = 0; i < cs.length; i++) {
            if (i == cs.length - 1 && cs[i] == 'X') {
                break;// 最后一位可以 是X或x
            }
            if (cs[i] < '0' || cs[i] > '9') {
                return false;
            }
            if (i < cs.length - 1) {
                power += (cs[i] - '0') * wi[i];
            }
        }

        // 校验区位码
        String area = idNumber.substring(0, 2);
        if (ArrayUtils.indexOf(areas, area) == -1) {
            return false;
        }

        // 校验年份
        String year = idType1 ? "19" + idNumber.substring(6, 8) : idNumber.substring(6, 10);

        final int iyear = Integer.parseInt(year);
        int low = 1900;
        if (iyear < low || iyear > Calendar.getInstance().get(Calendar.YEAR)) {
            // 1900年的PASS，超过今年的PASS
            return false;
        }

        // 校验月份
        int monthHigh = 12;
        String month = idType1 ? idNumber.substring(8, 10) : idNumber.substring(10, 12);
        final int imonth = Integer.parseInt(month);
        if (imonth < 1 || imonth > monthHigh) {
            return false;
        }

        // 校验天数
        String day = idType1 ? idNumber.substring(10, 12) : idNumber.substring(12, 14);
        final int iday = Integer.parseInt(day);
        int dayHigh = 31;
        if (iday < 1 || iday > dayHigh) {
            return false;
        }

        // 校验"校验码"
        if (idType1) {
            return true;
        }
        // Y = mod(S, 11)
        // Y: 0 1 2 3 4 5 6 7 8 9 10 校验码: 1 0 X 9 8 7 6 5 4 3 2
        return cs[cs.length - 1] == vi[power % 11];
    }

    private static String[] areas = { "11", "12", "13", "14", "15", "21", "22", "23", "31", "32", "33", "34", "35",
            "36", "37", "41", "42", "43", "44", "45", "46", "50", "51", "52", "53", "54", "61", "62", "63", "64", "65",
            "71", "81", "82", "91" };
    private static int[] vi = { '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2' };
    private static int[] wi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };

    @Override
    public void init() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig("id");
        ds = DbStartupHookProvider.dbMap.get(config.get("ds"));
    }
}
