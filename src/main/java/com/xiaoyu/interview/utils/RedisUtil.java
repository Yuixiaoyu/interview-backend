package com.xiaoyu.interview.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final RedissonClient redissonClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public RedisUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /* ================== 通用存取 ================== */

    public <T> void set(String key, T value, int expireSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            RBucket<String> bucket = redissonClient.getBucket(key);
            if (expireSeconds > 0) {
                bucket.set(jsonValue, expireSeconds, TimeUnit.SECONDS);
            } else {
                bucket.set(jsonValue);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象序列化失败", e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, clazz);
        } catch (IOException e) {
            throw new RuntimeException("对象反序列化失败", e);
        }
    }

    public <T> T get(String key, TypeReference<T> typeRef) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("对象反序列化失败", e);
        }
    }

    /* ================== List / Set / Map ================== */
    // Redisson 自带分布式集合 API，不需要 JSON 转换，直接存取

    public <T> void setList(String key, List<T> list, int expireSeconds) {
        redissonClient.getBucket(key).set(list, expireSeconds, TimeUnit.SECONDS);
    }

    public <T> List<T> getList(String key) {
        return (List<T>) redissonClient.getBucket(key).get();
    }

    public <T> void setSet(String key, Set<T> set, int expireSeconds) {
        redissonClient.getBucket(key).set(set, expireSeconds, TimeUnit.SECONDS);
    }

    public <T> Set<T> getSet(String key) {
        return (Set<T>) redissonClient.getBucket(key).get();
    }

    public <K, V> void setMap(String key, Map<K, V> map, int expireSeconds) {
        redissonClient.getBucket(key).set(map, expireSeconds, TimeUnit.SECONDS);
    }

    public <K, V> Map<K, V> getMap(String key) {
        return (Map<K, V>) redissonClient.getBucket(key).get();
    }

    /* ================== 通用方法 ================== */

    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public String getType(String key) {
        Object val = redissonClient.getBucket(key).get();
        return val == null ? "none" : val.getClass().getSimpleName();
    }
}
