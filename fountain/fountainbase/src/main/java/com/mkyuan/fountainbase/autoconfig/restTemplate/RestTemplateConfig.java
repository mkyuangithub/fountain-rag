package com.mkyuan.fountainbase.autoconfig.restTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class RestTemplateConfig {

    protected Logger logger = LogManager.getLogger(this.getClass());

    @Bean
    RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(600000);
        factory.setReadTimeout(600000);
        factory.setOutputStreaming(true);

        RestTemplate restTemplate = new RestTemplate(factory);
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (interceptors == null) {
            restTemplate.setInterceptors(Collections.singletonList(loggingInterceptor()));
        } else {
            interceptors.add(loggingInterceptor());
            restTemplate.setInterceptors(interceptors);
        }

        // 获取RestTemplate的消息转换器列表
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();

        // 找到MappingJackson2HttpMessageConverter
        for (HttpMessageConverter<?> converter : messageConverters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter =
                        (MappingJackson2HttpMessageConverter) converter;

                ObjectMapper objectMapper = jacksonConverter.getObjectMapper();
                // 设置不序列化null值
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            }
        }

        return restTemplate;
    }

    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            logRequestDetails(request, body);
            return execution.execute(request, body);
        };
    }

    private void logRequestDetails(HttpRequest request, byte[] body) {
        // 记录请求的详细信息
        logger.info(">>>>>>restTemplate URI: " + request.getURI());
        logger.info(">>>>>>restTemplate Method: " + request.getMethod());
        logger.info(">>>>>>restTemplate Headers: " + request.getHeaders());
        logger.info(">>>>>>restTemplate Request Body: " + new String(body, StandardCharsets.UTF_8));
    }
}
