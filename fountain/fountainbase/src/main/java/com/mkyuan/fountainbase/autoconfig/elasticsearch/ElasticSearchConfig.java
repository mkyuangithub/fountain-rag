package com.mkyuan.fountainbase.autoconfig.elasticsearch;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;

@Configuration
public class ElasticSearchConfig {
    protected Logger logger = LogManager.getLogger(this.getClass());
    @Value("${spring.elasticsearch.rest.uris}")
    private String elasticsearchUrl;

    @Value("${spring.elasticsearch.rest.username}")
    private String username;

    @Value("${spring.elasticsearch.rest.password}")
    private String password;

    @Autowired
    private RedisTemplate redisTemplate;

    private RestHighLevelClient esClient;
    private static final String REDIS_KEY = "fountain:settings:es:available";

    @PostConstruct
    public void init() {
        try {
            String[] urlParts = elasticsearchUrl.split(":");
            String hostname = urlParts[0];
            int port = Integer.parseInt(urlParts[1]);

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                                               new UsernamePasswordCredentials(username, password));

            esClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(hostname, port, "http"))
                              .setHttpClientConfigCallback(
                                      httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(
                                              credentialsProvider)));

            // 测试连接是否成功
            boolean isAvailable = esClient.ping(RequestOptions.DEFAULT);
            redisTemplate.opsForValue().set(REDIS_KEY, isAvailable);
            logger.info("Elasticsearch connection status: {}", isAvailable);
        } catch (Exception e) {
            logger.error("Elasticsearch configuration failed: {}", e.getMessage());
            redisTemplate.opsForValue().set(REDIS_KEY, false);
        }
    }

    @Bean
    public RestHighLevelClient client() {
        return esClient;
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }
}
