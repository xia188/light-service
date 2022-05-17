
package com.xlongwei.mobile.handler;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import com.networknt.utility.HybridUtils;

import org.apache.commons.lang3.StringUtils;

import io.undertow.server.HttpServerExchange;
import me.ihxq.projects.pna.Attribution;
import me.ihxq.projects.pna.PhoneNumberInfo;
import me.ihxq.projects.pna.PhoneNumberLookup;
import me.ihxq.projects.pna.algorithm.AnotherBinarySearchAlgorithmImpl;
import me.ihxq.projects.pna.algorithm.ProspectBinarySearchAlgorithmImpl;

@ServiceHandler(id = "xlongwei.com/mobile/region/0.0.1")
public class Region implements HybridHandler {
    static PhoneNumberLookup phoneNumberLookup = null;
    static boolean defaultCharsetUtf8 = false;

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        String mobile = HybridUtils.getParam(exchange, "mobile");
        Map<String, Object> map = new LinkedHashMap<>();
        int type = getMobileType(mobile);
        map.put("type", type);
        map.put("valid", type >= 0 && mobile.length() == 11);
        PhoneNumberInfo info = phoneNumberLookup.lookup(mobile).orElse(null);
        if (info != null) {
            Attribution attribution = info.getAttribution();
            String province = convert(attribution.getProvince());
            String city = convert(attribution.getCity());
            String isp = info.getIsp().getCnName();
            StringBuilder region = new StringBuilder(province);
            if (!province.equals(city)) {
                region.append(city);
            }
            region.append(isp);
            map.put("province", province);
            map.put("city", city);
            map.put("zipCode", attribution.getZipCode());
            map.put("areaCode", attribution.getAreaCode());
            map.put("isp", isp);
            map.put("region", region.toString());
        }
        return HybridUtils.toByteBuffer(JsonMapper.toJson(map));
    }

    /**
     * 手机号类型：-1-非法 0-未知 1-移动 2-联通 3-电信
     */
    public static int getMobileType(String string) {
        int mobileLength = 11;
        int mobilePrefixLength = 3;
        if (string == null || string.length() < mobilePrefixLength || string.length() > mobileLength) {
            return -1;
        }
        if (string.length() < mobileLength) {
            string = StringUtils.rightPad(string, mobileLength, '1');
        }
        if (!mobilePattern.matcher(string).matches()) {
            return -1;
        }
        if (mobile1.matcher(string).matches()) {
            return 1;
        }
        if (mobile2.matcher(string).matches()) {
            return 2;
        }
        if (mobile3.matcher(string).matches()) {
            return 3;
        }
        return 0;
    }

    private String convert(String string) {
        return defaultCharsetUtf8 ? string : new String(string.getBytes(), StandardCharsets.UTF_8);
    }

    @Override
    public void init() {
        // PhoneNumberLookup使用默认编码加载phone.dat，实际应该是UTF_8编码，windows下系统默认GBK导致乱码
        defaultCharsetUtf8 = StandardCharsets.UTF_8.equals(Charset.defaultCharset());
        Map<String, Object> config = Config.getInstance().getJsonMapConfig("mobile");
        String algorithm = (String) config.get("algorithm");
        switch (algorithm) {
            case "another":
                phoneNumberLookup = new PhoneNumberLookup(new AnotherBinarySearchAlgorithmImpl());
                break;
            case "prospect":
                phoneNumberLookup = new PhoneNumberLookup(new ProspectBinarySearchAlgorithmImpl());
                break;
            case "binary":
            default:
                phoneNumberLookup = new PhoneNumberLookup();
                break;
        }
    }

    private static String mobilePatternString = "^1(3[0-9]|4[5-9]|5[0-35-9]|6[2567]|7[0-9]|8[0-9]|9[1389])[0-9]{8}$";
    private static Pattern mobilePattern = Pattern.compile(mobilePatternString);
    private static Pattern mobile1 = Pattern
            .compile("^1(3[5-9]|4[78]|5[0-27-9]|65|7[28]|8[2-478]|98)\\d{8}$|^(134[0-8]|170[356])\\d{7}$");
    private static Pattern mobile2 = Pattern
            .compile("^1(3[012]|4[56]|5[56]|6[67]|7[156]|8[56])\\d{8}$|^170[47-9]\\d{7}$");
    private static Pattern mobile3 = Pattern
            .compile("^1(33|49|53|62|7[347]|8[019]|9[139])\\d{8}$|^(1349|170[0-2])\\d{7}$");

}
