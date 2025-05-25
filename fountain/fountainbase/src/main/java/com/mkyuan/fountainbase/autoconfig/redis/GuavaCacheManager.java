package com.mkyuan.fountainbase.autoconfig.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class GuavaCacheManager {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private LoadingCache<String, Optional<Object>> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .softValues()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Optional<Object>>() {
                @Override
                public Optional<Object> load(String key) throws Exception {
                    return Optional.ofNullable(redisTemplate.opsForValue().get(key));
                }
            });

    private LoadingCache<String, Optional<Object>> longTermCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .softValues()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<Object>>() {
                @Override
                public Optional<Object> load(String key) throws Exception {
                    return Optional.ofNullable(redisTemplate.opsForValue().get(key));
                }
            });

    public LoadingCache<String, Optional<Object>> getCache() {
        return cache;
    }

    public LoadingCache<String, Optional<Object>> getLongTermCache() {
        return longTermCache;
    }

    public void invalidateCache(String key) {
        longTermCache.invalidate(key);
        redisTemplate.delete(key);
    }
}
