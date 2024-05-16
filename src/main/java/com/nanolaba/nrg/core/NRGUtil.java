package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;

import java.util.Properties;

public class NRGUtil {

    public static void mergeProperty(Object key, Object value, Properties properties) {
        if (properties.containsKey(key)) {
            LOG.warn("A duplicate property '{}' declaration was detected", key);
        }
        properties.setProperty(key.toString(), String.valueOf(value));
    }
}
