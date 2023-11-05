package cn.cnic.dataspace.api.datax.admin.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @ explain JAVA reflection tool class
 */
public class ReflectionUtil {

    /**
     * Obtain the value of a private member variable
     */
    public static Object getPrivateField(Object instance, String filedName) throws NoSuchFieldException, IllegalAccessException {
        Field field = instance.getClass().getDeclaredField(filedName);
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * Set the value of private members
     */
    public static void setPrivateField(Object instance, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * Accessing Private Methods
     */
    public static Object invokePrivateMethod(Object instance, String methodName, Class[] classes, String objects) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = instance.getClass().getDeclaredMethod(methodName, classes);
        method.setAccessible(true);
        return method.invoke(instance, objects);
    }
}
