package com.dbclient.jdbc.server.util;

public class TypeChecker {

    // 基本包装类型的类
    private static final Class<?>[] WRAPPER_TYPES = {
            Boolean.class, Byte.class, Character.class, Double.class,
            Float.class, Integer.class, Long.class, Short.class, String.class
    };

    public static boolean isPrimary(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() || isWrapperType(clazz);
    }

    private static boolean isWrapperType(Class<?> clazz) {
        for (Class<?> wrapper : WRAPPER_TYPES) {
            if (wrapper.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isPrimary(5)); // true, Integer
        System.out.println(isPrimary('c')); // true, Character
        System.out.println(isPrimary(5.0)); // true, Double
        System.out.println(isPrimary("Hello")); // false, String
    }
}