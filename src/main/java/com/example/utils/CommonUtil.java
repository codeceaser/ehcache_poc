package com.example.utils;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.beans.Expression;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class CommonUtil {

    public static final Map<Class, Map<String, Field>> CLASS_FIELD_MAP = Maps.newHashMap();

    public static final BiFunction<Class, String, Field> extractField = (clazz, fieldName) -> {
      Field f = null;

        if (!CLASS_FIELD_MAP.containsKey(clazz)) {
            CLASS_FIELD_MAP.put(clazz, Maps.newHashMap());
        }
        Map<String, Field> stringFieldMap = CLASS_FIELD_MAP.get(clazz);
        if (stringFieldMap.containsKey(fieldName)) {
            f = stringFieldMap.get(fieldName);
        } else {
            try {
                f = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                f = ClassUtils.getAllSuperclasses(clazz).stream().map(cls -> {
                    try {
                        final Field declaredField = cls.getDeclaredField(fieldName);
                        return declaredField;
                    } catch (NoSuchFieldException ex) {
                    }
                    return null;
                }).filter(Objects::nonNull).findFirst().orElseGet(() -> null);
            }
            if (Objects.nonNull(f)) {
                f.setAccessible(true);
                stringFieldMap.put(fieldName, f);
            }
        }
      return f;
    };

    public static final BiFunction<Class, String, Object> extractValue = (clazz, fieldName) -> {
        final Field field = extractField.apply(clazz, fieldName);
        if (Objects.nonNull(field)) {
            try{
                return field.get(clazz);
            }catch(IllegalAccessException e){

            }
        }
        return null;
    };

    public static final BiFunction<Object, String, Object> get = (source, getter) -> {
      Object result = null;
        try {
            if (StringUtils.isBlank(getter)) {
                return result;
            }
            Expression expr = new Expression(source, getter, null);
            expr.execute();
            result = expr.getValue();
        } catch (Exception e) {

        }
        return result;
    };

    public static final BiFunction<Class, Collection<String>, Map<Field, Method>> fieldToGetterExtractor = (clazz, methodFields) -> {
        Map<Field, Method> fieldGetterMap = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(methodFields)) {
            try {
                Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors()).filter(propertyDescriptor -> methodFields.contains(propertyDescriptor.getName())).forEach(propertyDescriptor -> {
                    fieldGetterMap.put(extractField.apply(clazz, propertyDescriptor.getName()), propertyDescriptor.getReadMethod());
                });
            } catch (IntrospectionException e) {
            }
        }
        return fieldGetterMap;
    };
}
