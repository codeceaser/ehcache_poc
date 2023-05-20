package com.example.cache;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.example.utils.CommonUtil.*;
import static java.util.stream.Collectors.joining;

//All Operations are having Time Complexity: O(1)
public abstract class CacheRefreshStrategy<K, C> {

    @Autowired
    CacheManager cacheManager;

    public static final Logger LOGGER = LoggerFactory.getLogger(CacheRefreshStrategy.class);

    public abstract String cacheName();
    public abstract List<String> cacheKeyFields();
    public abstract String cacheIdentifierField();

    public abstract C getExistingObjectByIdentifier(Object id);

    public abstract Boolean isEvictionFromExistingCacheRequired(C existingObject);

    public abstract Boolean areKeysDifferent(C existing, C newer);

    //Time Complexity: O(1)
    public final C processEvictionFromExistingCache(C existingObject, C newerObject, Cache cachesPresent) {
        C removedObject = null;
        if (Objects.nonNull(cachesPresent) && isEvictionFromExistingCacheRequired(existingObject)) {
            if (areKeysDifferent(existingObject, newerObject)) {
                removedObject = removeObjectFromCache(existingObject, cachesPresent);
                Object keyForNewerCache = EXTRACT_CACHE_KEY.apply(newerObject, cacheKeyFields());
                Object keyForExistingCache = EXTRACT_CACHE_KEY.apply(existingObject, cacheKeyFields());
                LOGGER.info("Key for Cached Object {} has changed from {} to {}", existingObject, keyForExistingCache, keyForNewerCache);
            }
        }
        return removedObject;
    }

    public static final BiFunction<Object, List<String>, Object> EXTRACT_CACHE_KEY = (cacheObject, cacheKeyFields) -> {
        Object keyForExistingCache = null;
        Collection<Method> gettersForCacheKey = cacheKeyFields.stream().map(cacheKeyField -> fieldToGetterExtractor.apply(cacheObject.getClass(), Sets.newHashSet(cacheKeyField)).get(extractField.apply(cacheObject.getClass(), cacheKeyField))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(cacheKeyFields)) {
            if (cacheKeyFields.size() > 1) {
                keyForExistingCache = gettersForCacheKey.stream().map(getter -> get.apply(cacheObject, getter.getName())).map(String::valueOf).collect(joining("-"));
            } else{
                keyForExistingCache = gettersForCacheKey.stream().map(getter -> get.apply(cacheObject, getter.getName())).findFirst().get();
            }
        }
        return keyForExistingCache;
    };

    //Time Complexity: O(1)
    public final C removeObjectFromCache(C existingObject, Cache cachesPresent){
        C removedObject = null;
        Object keyForExistingCache = EXTRACT_CACHE_KEY.apply(existingObject, cacheKeyFields());

        Cache.ValueWrapper existingCache = cachesPresent.get(keyForExistingCache);
        if (Objects.nonNull(existingCache)) {
            LOGGER.info("Cache for Key {} is found", keyForExistingCache);
            Map cacheMap = (Map) existingCache.get();
            if (Objects.nonNull(cacheMap)) {
                Method getterForCacheObjectIdentifier = fieldToGetterExtractor.apply(existingObject.getClass(), Sets.newHashSet(cacheIdentifierField())).get(extractField.apply(existingObject.getClass(), cacheIdentifierField()));
                Object id = get.apply(existingObject, getterForCacheObjectIdentifier.getName());
                if (cacheMap.containsKey(id)) {
                    LOGGER.info("removing an entry {} from existing cache for key {}", existingObject, keyForExistingCache);
                    removedObject = (C)cacheMap.remove(id);
                }
            }
        }
        return removedObject;
    }

    //Time Complexity: O(1)
    public final C updateCache(C newerObject, Cache cachesPresent) {
        C addedObject = null;
        if (Objects.nonNull(cachesPresent)) {
            //Compare the field that is the key for the Cache and if key has changed then evict from existing Cache
            //Key: name, type

            //refresh the new Cache with new key value
            Object keyForNewerCache = EXTRACT_CACHE_KEY.apply(newerObject, cacheKeyFields());
            Method getterForCacheObjectIdentifier = fieldToGetterExtractor.apply(newerObject.getClass(), Sets.newHashSet(cacheIdentifierField())).get(extractField.apply(newerObject.getClass(), cacheIdentifierField()));
            Object id = get.apply(newerObject, getterForCacheObjectIdentifier.getName());
            Cache.ValueWrapper valueWrapper = cachesPresent.get(keyForNewerCache);
            if (Objects.nonNull(valueWrapper)) {
                Map cacheMap = (Map) valueWrapper.get();
                if (Objects.nonNull(cacheMap)) {
                    if (cacheMap.containsKey(id)) {
                        LOGGER.info("removing an existing entry {} from cache", cacheMap.get(id));
                        cacheMap.remove(id);
                    }
                    LOGGER.info("adding an entry {} into cache", newerObject);
                    addedObject = (C) cacheMap.put(id, newerObject);
                }
            }
        }
        return addedObject;
    }

    //Time Complexity: O(1)
    public final C refreshCache(C existingObject, C newerObject, String isDelete){
        if (StringUtils.isBlank(isDelete)) {
            isDelete = "N";
        }
        C removedObject = null;
        //Input: Cache Name
        Cache caches = cacheManager.getCache(cacheName());
        if (StringUtils.equalsIgnoreCase("N", isDelete)) {
            removedObject = processEvictionFromExistingCache(existingObject, newerObject, caches);
            C addedObject = updateCache(newerObject, caches);
        } else {
            removedObject = removeObjectFromCache(existingObject, caches);
        }
        return removedObject;
    }

}


