package com.itranswarp.autumn.io;

import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置项读取类.
 *
 * @Author: suwg
 * @Date: 2023/4/27
 */
public class PropertyResolver {

    private static final Logger logger = Logger.getLogger(PropertyResolver.class.getName());
    /**
     * 配置项Map.
     */
    Map<String, String> properties = new HashMap<>();

    /**
     * 存储Class -> Function:.
     */
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    /**
     * 存储所有的配置项,包括环境变量.
     *
     * @param props
     */
    public PropertyResolver(Properties props) {
        //存入环境变量.
        this.properties.putAll(System.getenv());
        //存入Properties.
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (logger.isLoggable(Level.FINE)) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.fine(String.format("PropertyResolver: %s = %s", key, this.properties.get(key)));
            }
        }

        //注册类型转化converter,
        //String类型转换到指定Class类型:.
        converters.put(String.class, s -> s);

        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));

    }


    /**
     * 判断配置项是否存在.
     *
     * @param key
     * @return
     */
    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }


    /**
     * 提供convert接口给到用户，可以自定义converter.
     *
     * @param clazz
     * @param value
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 泛化的getProperty方法.
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    /**
     * 获取配置项的值.
     *
     * @param key
     * @param targetType
     * @param defaultValue
     * @param <T>
     * @return
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }


    /**
     * 配置项：通过key来查询配置项的值.
     */
    @Nullable
    public String getProperty(String key) {

        //解析 ${abc.xyz:defaultValue};
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.getDefaultValue() != null) {
                //带默认值查询.
                return getProperty(keyExpr.getKey(), keyExpr.getDefaultValue());
            } else {
                //不带默认值查询.
                return getRequiredProperty(keyExpr.getKey());
            }
        }

        //普通key查询： eg： "app.title"
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    /**
     * 带默认值的keu查询.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    private String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            return value;
        }
        if (expr.getDefaultValue() != null) {
            return getProperty(expr.getKey(), expr.getDefaultValue());
        } else {
            return getRequiredProperty(expr.getKey());
        }
    }

    /**
     * 获取请求的配置项值.
     *
     * @param key
     * @return
     */
    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }


    /**
     * 解析 ${abc.xyz:defaultValue} 形式的key.
     */
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            // 是否存在defaultValue?
            int n = key.indexOf(":");
            if (n == -1) {
                //没有默认值: ${key}
                String k = notEmpty(key.substring(2, key.length() - 1)); //获取到实际的key
                return new PropertyExpr(k, null);
            } else {
                //有默认值： ${key:default}
                String k = key.substring(2, n);
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    /**
     * keu不为空.
     *
     * @param key
     * @return
     */
    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }

}
