package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NRGUtil {

    public static void mergeProperty(Object key, Object value, Properties properties) {
        if (properties.containsKey(key)) {
            LOG.warn("A duplicate property '{}' declaration was detected", key);
        }
        properties.setProperty(key.toString(), String.valueOf(value));
    }

    public static Map<String, String> parseParametersLine(String parameters) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isNotEmpty(parameters)) {
            Pattern pattern = Pattern.compile("(\\S+) *= *['\"]([^\"]+)['\"]");
            Matcher matcher = pattern.matcher(parameters);
            while (matcher.find()) {
                map.put(matcher.group(1), matcher.group(2));
            }
        }
        return map;
    }

    public static String unwrapParameterValue(String wrapped) {
        wrapped = StringUtils.trimToEmpty(wrapped);
        return wrapped.length() > 1 ? StringUtils.unwrap(wrapped, wrapped.charAt(0)) : "";
    }
}
