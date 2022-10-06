package com.xlongwei.apijson;

import java.util.Comparator;
import java.util.Objects;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import apijson.NotNull;

public class DemoFunctionParser extends apijson.demo.DemoFunctionParser {
    /**
     * 支持拼音排序：可以参考bank项目引入houbb依赖，此处暂未引入拼音依赖
     * 
     * <pre>
     * 示例：{"Province[]":{"Province":{"@column":"code,name"}},"sort()":"pinyinSort(Province[],name)"}
     * 请求：{"Province[]":[{"code":"11","name":"北京市"},{"code":"51","name":"四川省"},{"code":"50","name":"重庆市"}],"sort":true,"ok":true,"code":200,"msg":"success"}
     * 响应：{"Province[]":[{"code":"11","name":"北京市"},{"code":"50","name":"重庆市"},{"code":"51","name":"四川省"}],"sort":true,"ok":true,"code":200,"msg":"success"}
     */
    public boolean pinyinSort(@NotNull JSONObject current, @NotNull String object, @NotNull String key) {
        Object obj = current.get(object);
        if (obj instanceof JSONArray) {
            JSONArray array = current.getJSONArray(object);
            array.sort(new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    if (Objects.equals(o1, o2)) {
                        return 0;
                    }
                    if (o1 instanceof JSONObject && o2 instanceof JSONObject) {
                        JSONObject json1 = (JSONObject) o1, json2 = (JSONObject) o2;
                        String value1 = json1.getString(key), value2 = json2.getString(key);
                        // return PinyinUtil.ZH_COMPARATOR.compare(value1, value2);
                        return value1==null ? -1 : value1.compareTo(value2);
                    }
                    return 0;
                }
            });
            return true;
        } else {
            return false;
        }
    }
}
