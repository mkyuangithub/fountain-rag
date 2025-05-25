package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.locale.bean.I18nEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserLocaleService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String REDIS_LOCALE_PREFIX = "fountain:locale:";
    private static final long CACHE_SECONDS = 60;

    public void setUserLocale(String userName, String locale) {
        String key = REDIS_LOCALE_PREFIX + userName;
        redisTemplate.opsForValue().set(key, locale);
    }

    public String getCurrentLocale(String userName) {
        String key = REDIS_LOCALE_PREFIX + userName;
        String locale = (String)redisTemplate.opsForValue().get(key);
        return StringUtils.isEmpty(locale) ? "zh" : locale;
    }

    public String getI18nValue(String userName, String key) {
        String locale = getCurrentLocale(userName);
        String cacheKey = "fountain:i18n:" + key + ":" + locale;

        // 先从Redis获取
        String value = (String)redisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        // Redis没有，从MongoDB获取
        Query query = new Query(Criteria.where("key").is(key));
        I18nEntity i18n = mongoTemplate.findOne(query, I18nEntity.class);

        if (i18n == null) {
            return key; // 如果找不到对应的翻译，返回key本身
        }

        // 根据locale获取对应的值
        value = locale.equals("en") ? i18n.getEnValue() : i18n.getZhValue();
        if (StringUtils.isEmpty(value)) {
            value = i18n.getZhValue(); // 如果对应语言的值为空，默认返回中文
        }

        // 存入Redis缓存
        redisTemplate.opsForValue().set(cacheKey, value, CACHE_SECONDS, TimeUnit.SECONDS);

        return value;
    }
}
