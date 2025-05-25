package com.mkyuan.fountainbase.autoconfig.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Resource;

@Configuration
public class RedisSentinelConfig {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Value("${spring.redis.nodes}")
    private String nodes;
    @Value("${spring.redis.max-redirects:3}")
    private Integer maxRedirects;
    @Value("${spring.redis.password}")
    private String password;
    @Value("${spring.redis.database:0}")
    private Integer database;
    @Value("${spring.redis.timeout}")
    private int timeout;

    @Value("${spring.redis.sentinel.nodes}")
    private String sentinel;

    @Value("${spring.redis.jedis.pool.max-active:50}")
    private Integer maxActive;
    @Value("${spring.redis.jedis.pool.max-idle:50}")
    private Integer maxIdle;
    @Value("${spring.redis.jedis.pool.max-wait:-1}")
    private Long maxWait;
    @Value("${spring.redis.jedis.pool.min-idle:0}")
    private Integer minIdle;
    @Value("${spring.redis.sentinel.master}")
    private String master;
    @Value("${spring.redis.switchFlag}")
    private String switchFlag;
    @Value("${spring.redis.jedis.pool.shutdown-timeout}")
    private Integer shutdown;

    @Value("${spring.redis.jedis.pool.timeBetweenEvictionRunsMillis}")
    private long timeBetweenEvictionRunsMillis;

    @Resource
    private RedissonProperties redissonProperties;

    public String getSwitchFlag() {
        return switchFlag;
    }

    /**
     * 连接池配置信息
     *
     * @return
     */


    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        return config;
    }

    /**
     * 配置 Redis Cluster 信息
     */

    @Bean
    // @ConditionalOnMissingBean
    public JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory factory = null;
        try {
            String[] split = nodes.split(",");
            Set<HostAndPort> nodes = new LinkedHashSet<>();
            for (int i = 0; i < split.length; i++) {
                try {
                    String[] split1 = split[i].split(":");
                    nodes.add(new HostAndPort(split1[0], Integer.parseInt(split1[1])));
                } catch (Exception e) {
                    logger.error(">>>>>>出现配置错误!请确认: " + e.getMessage(), e);
                    throw new RuntimeException(String.format("出现配置错误!请确认node=[%s]是否正确", nodes));
                }
            }

            // 如果是哨兵的模式
            if (!StringUtils.isEmpty(sentinel)) {
                logger.info(">>>>>>Redis use SentinelConfiguration");
                RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
                String[] sentinelArray = sentinel.split(",");
                for (String s : sentinelArray) {
                    try {
                        String[] split1 = s.split(":");
                        redisSentinelConfiguration.addSentinel(new RedisNode(split1[0], Integer.parseInt(split1[1])));
                    } catch (Exception e) {
                        logger.error(">>>>>>出现配置错误!请确认: " + e.getMessage(), e);
                        throw new RuntimeException(String.format("出现配置错误!请确认node=[%s]是否正确", sentinelArray));
                    }
                }
                redisSentinelConfiguration.setMaster(master);
                // logger.info(">>>>>>redis 密码-> {}", password);
                redisSentinelConfiguration.setPassword(password);
                factory = new JedisConnectionFactory(redisSentinelConfiguration, jedisPoolConfig());
            }
            // 如果是单个节点 用Standalone模式
            else {
                if (nodes.size() < 2) {
                    logger.info(">>>>>>Redis use RedisStandaloneConfiguration");
                    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();

                    // 遍历获取第一个节点的配置
                    for (HostAndPort n : nodes) {
                        redisStandaloneConfiguration.setPort(n.getPort());
                        redisStandaloneConfiguration.setHostName(n.getHost());
                        break; // 只需要第一个节点
                    }

                    // 设置密码
                    if (!StringUtils.isEmpty(password)) {
                        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
                    }

                    // 创建工厂并设置配置
                    factory = new JedisConnectionFactory(redisStandaloneConfiguration);
                    factory.setPoolConfig(jedisPoolConfig());
                } else {
                    // cluster
                }
            }
// 确保在最后调用afterPropertiesSet
            if (factory != null) {
                factory.afterPropertiesSet();
            }
        } catch (Exception e) {
            logger.error(">>>>>>init redis config error->{}", e.getMessage(), e);
        }
        return factory;
    }

    private RedisTemplate<String, Object> initRedisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        try {
            template.setConnectionFactory(jedisConnectionFactory);

            Jackson2JsonRedisSerializer jacksonSerial = new Jackson2JsonRedisSerializer<>(Object.class);
            GenericJackson2JsonRedisSerializer gser = new GenericJackson2JsonRedisSerializer();

            ObjectMapper om = new ObjectMapper();
            // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
            om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

            jacksonSerial.setObjectMapper(om);

            StringRedisSerializer stringSerial = new StringRedisSerializer();
            template.setKeySerializer(stringSerial);
            template.setValueSerializer(gser);
            // template.setValueSerializer(stringSerial);
            // template.setValueSerializer(jacksonSerial);
            template.setHashKeySerializer(stringSerial);
            template.setHashValueSerializer(jacksonSerial);
            // template.setHashKeySerializer(gser);
            template.afterPropertiesSet();

            return template;
        } catch (Exception e) {
            logger.error(">>>>>>init redisTemplate error: " + e.getMessage(), e);
        }
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> template = null;
        boolean connected = false;
        try {
            // 创建Redis缓存操作助手RedisTemplate对象
            while (!connected) {
                logger.info(">>>>>>准备连接redis");
                template = initRedisTemplate(jedisConnectionFactory);
                if (template != null) {
                    connected = true;
                }
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            logger.error(">>>>>>init redisTemplate error : " + e.getMessage(), e);
        }
        return template;
    }

    /**
     * 哨兵模式自动装配
     *
     * @return
     */
    @Bean
    RedissonClient redissonSentinel() {
        logger.info(">>>>>>redisson address size->" + redissonProperties.getSentinelAddresses().length);

        Config config = new Config();
        try {
            // 检查sentinel地址数量
            String[] sentinelAddresses = redissonProperties.getSentinelAddresses();
            logger.info(">>>>>>redisson address size->" + sentinelAddresses.length);

            if (sentinelAddresses.length == 1) {
                // Standalone模式
                String address = sentinelAddresses[0].replace("redis://", "");
                String[] hostAndPort = address.split(":");

                config.useSingleServer()
                        .setAddress("redis://" + address)
                        .setTimeout(redissonProperties.getTimeout())
                        .setConnectionPoolSize(redissonProperties.getMasterConnectionPoolSize())
                        .setConnectionMinimumIdleSize(redissonProperties.getConnectionMinimumIdleSize());

                if (StringUtils.isNotBlank(redissonProperties.getPassword())) {
                    config.useSingleServer().setPassword(redissonProperties.getPassword());
                }
            } else {
                // 哨兵模式
                SentinelServersConfig serverConfig = config.useSentinelServers()
                        .addSentinelAddress(sentinelAddresses)
                        .setMasterName(redissonProperties.getMasterName())
                        .setTimeout(redissonProperties.getTimeout())
                        .setMasterConnectionPoolSize(redissonProperties.getMasterConnectionPoolSize())
                        .setMasterConnectionMinimumIdleSize(redissonProperties.getConnectionMinimumIdleSize())
                        .setSubscriptionsPerConnection(redissonProperties.getSubscriptionsPerConnection())
                        .setSubscriptionConnectionPoolSize(redissonProperties.getSubscriptionConnectionPoolSize())
                        .setSlaveConnectionMinimumIdleSize(redissonProperties.getConnectionMinimumIdleSize())
                        .setSlaveConnectionPoolSize(redissonProperties.getSlaveConnectionPoolSize());

                if (StringUtils.isNotBlank(redissonProperties.getPassword())) {
                    serverConfig.setPassword(redissonProperties.getPassword());
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>init redisson error->{}", e.getMessage(), e);
        }
        return Redisson.create(config);
    }

}
