package com.mkyuan.fountainbase.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceUtil {

    @Autowired
    private RedisTemplate redisTemplate;
    private final long workerId;
    private static final long MAX_VALUE = 99999999999999L;

    public SequenceUtil(@Value("${fountain.sequence.worker-id:1}") long workerId) {
        this.workerId = workerId;
    }

    public long nextId(String sequenceName) {
        String timeKey = "id:timestamp:" + sequenceName;
        String seqKey = "id:sequence:" + sequenceName;

        // 使用Redis的原子操作获取时间戳和序列号
        Long timestamp = redisTemplate.opsForValue().increment(timeKey);
        Long sequence = redisTemplate.opsForValue().increment(seqKey) % 1024; // 限制序列号范围

        // 每次时间戳变化时重置序列号
        if (sequence == 0) {
            redisTemplate.opsForValue().set(seqKey, "0");
        }

        // 组合ID
        long id = ((timestamp & 0xFFFFFFFFL) << 15) | // 时间戳部分
                (workerId << 10) |                   // 工作机器ID部分
                sequence;                            // 序列号部分

        // 确保不超过14位
        id = id % MAX_VALUE;
        return id == 0 ? 1 : id;
    }

    /**
     * 获取递增的序列号，确保每次调用都比之前的值大1
     *
     * @param sequenceKey 序列号的键名
     * @param initialValue 如果序列不存在，使用的初始值
     * @return 递增后的序列号
     */
    public long getIncrementalSequence(String sequenceKey, long initialValue) {
        String redisKey = "id:sequence:incremental:" + sequenceKey;

        // 检查键是否存在
        Boolean hasKey = redisTemplate.hasKey(redisKey);

        if (Boolean.FALSE.equals(hasKey)) {
            Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(redisKey, Long.valueOf(initialValue - 1));

            if (Boolean.FALSE.equals(setSuccess)) {
                // 将返回值转换为long
                Long result = redisTemplate.opsForValue().increment(redisKey);
                return result != null ? result : initialValue;
            }
        }

        // 将返回值转换为long
        Long result = redisTemplate.opsForValue().increment(redisKey);
        return result != null ? result : initialValue;
    }
}
