package com.mkyuan.fountainbase.autoconfig.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
@Configuration
@ConfigurationProperties(prefix = "fountain.minio")
public class MinioConfiguration {

    private String url;
    private String accessKey;
    private String secretKey;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Bean
    MinioClient minioClient() {
        return new MinioClient.Builder().endpoint(this.url).credentials(this.accessKey, this.secretKey).build();
    }
}
