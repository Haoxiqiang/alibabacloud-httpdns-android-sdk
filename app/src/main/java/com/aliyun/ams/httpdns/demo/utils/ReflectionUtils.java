package com.aliyun.ams.httpdns.demo.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by liyazhou on 17/3/28.
 */

public class ReflectionUtils {
    private static final String CLASS_HttpDnsConfig = "com.alibaba.sdk.android.httpdns.HttpDnsConfig";

    private static final String CLASS_HostManager = "com.alibaba.sdk.android.httpdns.HostManager";

    public static Field getField(Class clazz, String field) throws NoSuchFieldException {
        return clazz.getDeclaredField(field);
    }

    public static Method getMethod(Class clazz, String method, Class<?>... parameterTypes) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(method, parameterTypes);
    }

    public static Object getObject(Field field, Object object) throws IllegalAccessException {
        return field.get(object);
    }

    public static void setObject(Field field, Object object, Object value) throws IllegalAccessException {
        field.set(object, value);
    }

    public static void setServerIps(String[] serverIpArray) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Field field = Class.forName(CLASS_HttpDnsConfig).getDeclaredField("SERVER_IPS");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(Class.forName(CLASS_HttpDnsConfig), serverIpArray);
    }

    public static void setServerPort(int port) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Field field = Class.forName(CLASS_HttpDnsConfig).getDeclaredField("SERVER_PORT");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(Class.forName(CLASS_HttpDnsConfig), "" + port);
    }
}
