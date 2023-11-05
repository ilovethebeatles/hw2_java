package com.liza;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.HashMap;
import java.util.Map;


public class AdvancedCacheHandler {

    private static final Map<Method, Object> methodCache = new HashMap<>();
    private static final Map<Object, Map<String, Object>> objectState = new HashMap<>();

    public static <T> T cache(T object) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(object.getClass());
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (method.isAnnotationPresent(Cache.class)) {
                    if (methodCache.containsKey(method) && !hasStateChanged(obj)) {
                        return methodCache.get(method);
                    } else {
                        Object result = proxy.invokeSuper(obj, args);
                        methodCache.put(method, result);
                        return result;
                    }
                }
                if (method.isAnnotationPresent(Setter.class)) {
                    methodCache.clear();
                    objectState.put(obj, getCurrentState(obj));
                }
                return proxy.invokeSuper(obj, args);
            }
        });

        return (T) enhancer.create();
    }

    private static boolean hasStateChanged(Object obj) {
        Map<String, Object> currentState = getCurrentState(obj);
        Map<String, Object> lastState = objectState.get(obj);
        if (lastState == null) {
            return false;
        }
        return !currentState.equals(lastState);
    }

    private static Map<String, Object> getCurrentState(Object obj) {
        Map<String, Object> state = new HashMap<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                state.put(field.getName(), field.get(obj));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return state;
    }
}

