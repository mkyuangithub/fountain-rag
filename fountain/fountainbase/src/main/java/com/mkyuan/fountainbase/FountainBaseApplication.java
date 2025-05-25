package com.mkyuan.fountainbase;

import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;

@SpringBootApplication

@ComponentScan(basePackages = "com.mkyuan")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class, RedissonAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class})
@EnableDiscoveryClient
public class FountainBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(FountainBaseApplication.class);
    }
}