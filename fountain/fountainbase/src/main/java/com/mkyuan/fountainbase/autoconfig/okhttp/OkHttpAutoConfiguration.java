package com.mkyuan.fountainbase.autoconfig.okhttp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass(OkHttpClient.class)
public class OkHttpAutoConfiguration {
	protected Logger logger = LogManager.getLogger(this.getClass());
	private static ObjectMapper mapper = new ObjectMapper();
	@Value("${fountain.okhttp.connection_timeout}")
	private int connTimeout = 30000;

	@Value("${fountain.okhttp.read_timeout}")
	private int readTimeout = 30000;

	@Value("${fountain.okhttp.write_timeout}")
	private int writeTimeout = 30000;

	// @Value("${fountain.okhttp.keep_alive_duration}")
	// private int max_idle_connections = 150;

	// @Value("${fountain.okhttp.max_idle_connections}")
	// private int keep_alive_duration = 30;

	@Value("${fountain.okhttp.client_proxy}")
	private boolean clientProxy = false;

	// @Bean
	// public ConnectionPool pool() {
	// return new ConnectionPool(max_idle_connections, keep_alive_duration, TimeUnit.SECONDS);
	// }
	private String removeVectorIfFloatArray(String jsonMessage) {
		JsonNode rootNode = null;
		String rtnMsg = jsonMessage; // 默认返回原始消息
		try {
			rootNode = mapper.readTree(jsonMessage);
			// 处理顶层的vector
			processVectorNode(rootNode);
			// 处理points数组内的vector
			JsonNode pointsNode = rootNode.path("points");
			if (pointsNode.isArray()) {
				for (JsonNode point : pointsNode) {
					processVectorNode(point);
				}
			}
			rtnMsg = mapper.writeValueAsString(rootNode);
		} catch (Exception e) {
			logger.error("处理JSON时发生错误返回原始报文");
		}
		return rtnMsg;
	}

	private void processVectorNode(JsonNode node) {
		if (node instanceof ObjectNode) {
			ObjectNode objectNode = (ObjectNode) node;
			JsonNode vectorNode = objectNode.path("vector");
			if (vectorNode.isArray()) {
				boolean allFloats = true;
				for (JsonNode element : vectorNode) {
					if (!element.isFloat() && !element.isDouble()) {
						allFloats = false;
						break;
					}
				}
				if (vectorNode.size() > 0 && allFloats) {
					objectNode.remove("vector");
				}
			}
		}
	}

	@Bean
	public OkHttpClient okHttpClient() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();

		// 添加拦截器以记录请求和响应的调用记录
		builder.addInterceptor(new Interceptor() {
			@Override
			public Response intercept(Chain chain) throws IOException {
				Request request = chain.request();
				long startTime = System.nanoTime();
				Response response = chain.proceed(request);
				long endTime = System.nanoTime();

				// 记录请求和响应的调用记录
				logger.info(">>>>>>OKHttp Request: " + request);
				logger.info(">>>>>>OKHttp Response: " + response);
				logger.info(">>>>>>OKHttp Duration: " + (endTime - startTime) / 1000000 + "ms");
				return response;
			}
		});
		builder.addInterceptor(new Interceptor() {
			@Override
			public Response intercept(Chain chain) throws IOException {
				Request request = chain.request();
				// if (logger.isDebugEnabled()) {
				if (request.method().equals("PUT") || request.method().equals("POST")) {
					Request copy = request.newBuilder().build();
					Buffer buffer = new Buffer();
					// if (logger.isDebugEnabled()) {
					if (copy.body() != null) {
						copy.body().writeTo(buffer);
						String message = buffer.readUtf8();
						logger.info(">>>>>>OKHttp->发送出去的报文: {}", removeVectorIfFloatArray(message));
						//logger.info(">>>>>>OKHttp->发送出去的报文: {}" , message);
						//logger.info(">>>>>>OKHttp->发送出去的报文: {}", message);
					}
					// }
				}
				// }
				return chain.proceed(request);
			}
		});
		// 设置connection pool
		// builder.connectionPool(pool());
		// 不允许错误重试
		// builder.retryOnConnectionFailure(true);
		// 设置连接超时和读取超时
		builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
		builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
		if (clientProxy) {
			// 设置代理
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
			builder.proxy(proxy);
		}

		return builder.build();
	}
}
