package com.mkyuan.fountainbase.autoconfig.mongodb;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoTransactionConfig {

    /*
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        TransactionOptions transactionOptions = TransactionOptions.builder()
                                                                  .readPreference(ReadPreference.primary())
                                                                  .readConcern(ReadConcern.LOCAL)
                                                                  .writeConcern(WriteConcern.MAJORITY.withWTimeout(30000, TimeUnit.MILLISECONDS))
                                                                  // 使用 Duration 来设置最大提交时间
                                                                  .maxCommitTime(30L, TimeUnit.SECONDS)
                                                                  .build();

        return new MongoTransactionManager(dbFactory, transactionOptions);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory);

        // 设置写关注
        mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);

        return mongoTemplate;
    }

     */
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDbFactory);
        mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);
        return mongoTemplate;
    }
}