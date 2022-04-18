package com.dbclient.jdbc.server.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public abstract class JSON {
    private static final ObjectMapper mapper;
    private static final ObjectMapper withEmptyMapper;
    private static final SimpleModule dateModule;

    private JSON() {
    }

    static {
        dateModule = new SimpleModule();
        //without empty
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        buldCommonMapper(mapper);
        //within empty
        withEmptyMapper = new ObjectMapper();
        withEmptyMapper.setSerializationInclusion(Include.ALWAYS);
        buldCommonMapper(withEmptyMapper);
    }

    /**
     * 设置mappepr的通用属性
     */
    private static void buldCommonMapper(ObjectMapper mapper) {
        mapper.registerModule(dateModule);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 将对象转换成json
     *
     * @param originalObject 要转换的对象
     */
    public static String toJSON(Object originalObject) {

        if (originalObject == null) return null;
        if (originalObject instanceof String) return String.valueOf(originalObject);

        String json = null;
        try {
            json = mapper.writeValueAsString(originalObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    /**
     * 将对象转换成json
     *
     * @param originalObject 要转换的对象
     */
    public static byte[] toJsonByte(Object originalObject) {

        if (originalObject == null) return null;

        byte[] json = null;
        try {
            json = mapper.writeValueAsBytes(originalObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }


    /**
     * 将对象转换成json,并包含空属性
     *
     * @param originalObject 要转换的对象
     */
    public static String toJsonWithEmpty(Object originalObject) {

        if (originalObject == null) return null;
        String json = null;
        try {
            json = withEmptyMapper.writeValueAsString(originalObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    /**
     * 进行类型转换
     */
    public static <T> T convert(Object origin, Class<T> targetClass) {
        return mapper.convertValue(origin, targetClass);
    }

    /**
     * 将list转为另一个list
     *
     * @param originList  原始List
     * @param targetClass 目标类型
     * @return 目标类型List
     */
    public static <O extends Collection<?>, T> List<T> convertList(O originList, Class<T> targetClass) {
        JavaType javaType = mapper.getTypeFactory().constructParametricType(originList.getClass(), targetClass);
        return mapper.convertValue(originList, javaType);
    }


    /**
     * 根据子key获取子json
     *
     * @param json json字符串
     * @param key  json字符串子key
     * @return 子json
     */
    public static String get(String json, String key) {

        if (json == null || "".equals(json)) return null;

        String value;
        try {
            value = mapper.readValue(json, JsonNode.class).get(key).textValue();
        } catch (IOException e) {
            e.printStackTrace();
            value = null;
        }

        return value;
    }

    /**
     * 将json转成List
     *
     * @param json      json字符串
     * @param valueType list泛型
     */
    public static <T> List<T> parseList(String json, Class<T> valueType) {

        return (List<T>) parseCollection(json, List.class, valueType);
    }

    /**
     * 将json转成List
     *
     * @param json      json字符串
     * @param valueType list泛型
     */
    public static <T, E extends Collection> Collection<T> parseCollection(String json, Class<E> collectionClass, Class<T> valueType) {

        if (json == null || "".equals(json) || valueType == null) return null;

        JavaType javaType = mapper.getTypeFactory().constructParametricType(collectionClass, valueType);

        Collection<T> objectList;
        try {
            objectList = mapper.readValue(json, javaType);
        } catch (Exception e) {
            e.printStackTrace();
            objectList = null;
        }

        return objectList;
    }

    /**
     * 将json转成指定的类对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T parse(Object obj, Class<T> type) {

        if (obj == null || "".equals(obj) || type == null) return null;
        if (type == String.class) return (T) obj;

        T result;
        try {
            result = mapper.readValue(toJSON(obj), type);
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    public static SimpleModule getDateModule() {
        return dateModule;
    }
}
