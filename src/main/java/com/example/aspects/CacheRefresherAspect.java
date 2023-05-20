package com.example.aspects;

import com.example.annotations.RefreshCache;
import com.example.cache.CacheRefreshStrategy;
import com.example.components.ApplicationContextProvider;
import com.example.components.Cacheable;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Aspect
public class CacheRefresherAspect {

    public static final Logger LOGGER = LoggerFactory.getLogger(CacheRefresherAspect.class);

    @Pointcut("@annotation(com.example.annotations.RefreshCache)")
    public void refreshCacheAnnotateMethods(){}

    @Pointcut("within(com.example..services..*)")
    public void withinServiceLayer(){}

    public static final BiFunction<Class, MethodSignature, RefreshCache> EXTRACT_REFRESH_CACHE_METADATA = (aClass, signature) -> {
        final Method method = signature.getMethod();
        RefreshCache refreshCache = null;
        String methodName = signature.getName();
        try {
            refreshCache = aClass.getDeclaredMethod(methodName, method.getParameterTypes()).getAnnotation(RefreshCache.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("RefreshCache Configuration Error, Method {} is missing the annotation RefreshCache", methodName);
        }
        return refreshCache;
    };

    @Around(value = "withinServiceLayer() && refreshCacheAnnotateMethods()")
    public Object refreshCaches(ProceedingJoinPoint joinPoint) {
        Object savedObject = null;
        Class<?> aClass = joinPoint.getTarget().getClass();
        String className = aClass.getSimpleName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        LOGGER.info("Intercepting [{}#{}] for Refreshing the Cache", className, methodName);
        Object existingObject = null;
        RefreshCache refreshCache = EXTRACT_REFRESH_CACHE_METADATA.apply(aClass, signature);
        if (Objects.nonNull(refreshCache)) {
            Object id = null;
            if (StringUtils.equalsIgnoreCase("N", refreshCache.isDelete())) {
                Cacheable objectToBeSaved = Optional.ofNullable(joinPoint.getArgs()).map(args -> Lists.newArrayList(args)).orElseGet(Lists::newArrayList).stream().filter(arg -> Cacheable.class.isAssignableFrom(arg.getClass())).map(arg -> (Cacheable)arg).findFirst().orElseGet(() -> null);
                if (Objects.nonNull(objectToBeSaved) && Objects.nonNull(objectToBeSaved.getId())) {
                    id = objectToBeSaved.getId();
                } else {
                    LOGGER.error("No Cacheable Object is being Saved or it does not have the id");
                }
            } else {
                id = Optional.ofNullable(joinPoint.getArgs()).map(args -> Lists.newArrayList(args)).orElseGet(Lists::newArrayList).stream().findFirst().orElseGet(() -> null);
            }
            if (Objects.nonNull(id)) {
                for (String cacheName : refreshCache.cacheNames()) {
                    CacheRefreshStrategy cacheRefreshStrategy = (CacheRefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(CacheRefreshStrategy.class, cacheName);
                    if (Objects.isNull(existingObject) && Objects.nonNull(id)) {
                        existingObject = cacheRefreshStrategy.getExistingObjectByIdentifier(id);
                        break;
                    }
                }
            }
            if (Objects.nonNull(existingObject)) {
                LOGGER.info("Existing Object {} is found", existingObject);
            } else {
                LOGGER.info("No Existing Object is found with Id {}", id);
            }
        } else {
            LOGGER.error("No Refresh Cache Annotation Found");
        }

        try {
            savedObject = joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        final Object existing = existingObject;
        final Object newer = savedObject;

        if (Objects.nonNull(refreshCache)) {
            Stream<CompletableFuture<Object>> futureStream = Arrays.stream(refreshCache.cacheNames()).map(cacheName -> supplyAsync(() -> {
                LOGGER.info("Refreshing the Cache : {}", cacheName);
                CacheRefreshStrategy cacheRefreshStrategy = (CacheRefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(CacheRefreshStrategy.class, cacheName);
                Object replacedObject = cacheRefreshStrategy.refreshCache(existing, newer, refreshCache.isDelete());
                LOGGER.info("Object {} replaced {} in Cache {}", newer, existing, cacheName);
                return replacedObject;
            })).map((future) -> future.whenComplete((evictedObject, exception) -> {
                if (Objects.nonNull(exception)) {
                    LOGGER.error("Error while refreshing cache ", exception);
                } else {
                    LOGGER.info("As a result of Cache Refresh, object {} was evicted", evictedObject);
                }
            }));
            futureStream.collect(Collectors.toList());
            /*for (String cacheName : refreshCache.cacheNames()) {
                LOGGER.info("Refreshing the Cache : {}", cacheName);
                CacheRefreshStrategy cacheRefreshStrategy = (CacheRefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(CacheRefreshStrategy.class, cacheName);
                replacedObject = cacheRefreshStrategy.refreshCache(existingObject, savedObject, refreshCache.isDelete());
                LOGGER.info("Object {} replaced {} in Cache {}", savedObject, existingObject, cacheName);
            }*/
        }

        return savedObject;
    }

}
