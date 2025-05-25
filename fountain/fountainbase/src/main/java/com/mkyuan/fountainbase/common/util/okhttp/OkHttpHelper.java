package com.mkyuan.fountainbase.common.util.okhttp;

import java.io.File;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;


import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class OkHttpHelper {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private OkHttpClient okHttpClient;

    @Value("${fountain.okhttp.client_proxy}")
    private boolean clientProxy = false;

    private int connTimeout = 30000;

    private int readTimeout = 30000;

//    private int writeTimeout = 10000;

    public String getJsonDirectly(String url, Map<String, String> params) throws Exception {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        OkHttpClient httpClient = builder.build();

        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpBuilder.addQueryParameter(param.getKey(), param.getValue());
        }

        Request request = new Request.Builder().url(httpBuilder.build()).get().addHeader("Content-Type", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new Exception(">>>>>>OKHttp Get调用失败" + response);
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Get调用失败: {}", e.getMessage(), e);
            throw new Exception(">>>>>>OKHttp Get调用失败: " + e.getMessage(), e);
        }
    }

    public String getJson(String url, Map<String, String> params) throws Exception {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (clientProxy) {
            // 设置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
            builder.proxy(proxy);
        }
        OkHttpClient httpClient = builder.build();

        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpBuilder.addQueryParameter(param.getKey(), param.getValue());
        }

        Request request = new Request.Builder().url(httpBuilder.build()).get().addHeader("Content-Type", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new Exception(">>>>>>OKHttp Get调用失败" + response);
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Get调用失败: {}", e.getMessage(), e);
            throw new Exception(">>>>>>OKHttp Get调用失败: " + e.getMessage(), e);
        }
    }
    public String getJson(String url, Map<String, String> params,Map<String, String> headers) throws Exception {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (clientProxy) {
            // 设置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
            builder.proxy(proxy);
        }
        OkHttpClient httpClient = builder.build();

        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpBuilder.addQueryParameter(param.getKey(), param.getValue());
        }
        Request.Builder requestBuilder = new Request.Builder().url(httpBuilder.build()).get();

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new Exception(">>>>>>OKHttp Get调用失败" + response);
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Get调用失败: {}", e.getMessage(), e);
            throw new Exception(">>>>>>OKHttp Get调用失败: " + e.getMessage(), e);
        }
    }
    public String getJson(String url) throws Exception {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (clientProxy) {
            // 设置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
            builder.proxy(proxy);
        }
        OkHttpClient httpClient = builder.build();
        Request request = new Request.Builder().url(url).get().addHeader("Content-Type", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new Exception(">>>>>>OKHttp Get调用失败" + response);
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Get调用失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getJsonWithMultiHeaders(String url, Map<String, String> headers) throws Exception {
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        if (clientProxy) {
            // 设置代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
            builder.proxy(proxy);
        }
        OkHttpClient httpClient = builder.build();
        // Request request = new
        // Request.Builder().url(url).get().addHeader("Content-Type",
        // "application/json").build();
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception(response.body().string());
            }
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Get调用失败: {}", e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
    }

    public <T> String postJson(String url, T object) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        Response response = null;
        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request request = new Request.Builder().url(url).post(requestBody).addHeader("Content-Type", "application/json").build();

            response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Post调用失败: {}", e.getMessage(), e);
        }finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
    public String delete(String url, Map<String, String> headers, Object body, long overrideConnTimeout, long overrideReadTimeout) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            long sysConnTimeout = overrideConnTimeout > 0 ? overrideConnTimeout : connTimeout;
            long sysHandleTimeout = overrideReadTimeout > 0 ? overrideReadTimeout : readTimeout;

            builder.connectTimeout(sysConnTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(sysHandleTimeout, TimeUnit.MILLISECONDS);

            OkHttpClient httpClient = builder.build();

            // 将body对象转换为JSON字符串
            String jsonBody = JSON.toJSONString(body);
            // 创建RequestBody
            RequestBody requestBody = RequestBody.create(mediaType, jsonBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .delete(requestBody); // 使用带有请求体的delete方法

            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.code() != 200) {
                    throw new RuntimeException(String.format("HTTP请求失败，状态码：%d，响应内容：%s",
                                                             response.code(), responseBody));
                }
                return responseBody;
            }
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Delete With Body调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Delete With Body请求执行失败: " + e.getMessage(), e);
        }
    }
    public String delete(String url, Map<String, String> headers, long overrideConnTimeout, long overrideReadTimeout) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            long sysConnTimeout = overrideConnTimeout > 0 ? overrideConnTimeout : connTimeout;
            long sysHandleTimeout = overrideReadTimeout > 0 ? overrideReadTimeout : readTimeout;

            builder.connectTimeout(sysConnTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(sysHandleTimeout, TimeUnit.MILLISECONDS);

            OkHttpClient httpClient = builder.build();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .delete(); // 简单的DELETE请求，不带请求体

            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.code() != 200) {
                    throw new RuntimeException(String.format("HTTP请求失败，状态码：%d，响应内容：%s",
                            response.code(), responseBody));
                }
                return responseBody;
            }
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Delete调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Delete请求执行失败: " + e.getMessage(), e);
        }
    }

    public <T> String postJson(String url, T object, Map<String, String> headers, Map<String, String> parameters, long overrideConnTimeout, long overrideReadTimeout) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        try {
            // 处理URL参数
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            if (parameters != null && !parameters.isEmpty()) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
            String finalUrl = urlBuilder.build().toString();

            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            long sysConnTimeout;
            long sysHandleTimeout;
            if (overrideConnTimeout > 0) {
                sysConnTimeout = overrideConnTimeout;
            } else {
                sysConnTimeout = connTimeout;
            }
            if (overrideReadTimeout > 0) {
                sysHandleTimeout = overrideReadTimeout;
            } else {
                sysHandleTimeout = readTimeout;
            }

            // ... existing code ...
            // builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            // builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            builder.connectTimeout(sysConnTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(sysHandleTimeout, TimeUnit.MILLISECONDS);
            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request.Builder requestBuilder = new Request.Builder().url(finalUrl).post(requestBody);
            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = requestBuilder.build();
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    // 修改异常信息，将实际的响应内容包含进去
                    throw new Exception("访问服务出错 - HTTP状态码:" + response.code()
                            + ", 错误信息:" + response.message()
                            + ", 响应内容:" + responseBody
                            + ", 请求内容:" + requestBody);
                }
                return responseBody;
            }
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Post调用失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 支持多个http header传入
    public <T> String postJsonWithMultiHeaders(String url, T object, Map<String, String> headers, long overrideConnTimeout, long overrideReadTimeout) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        Response response = null;
        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            long sysConnTimeout;
            long sysHandleTimeout;
            if (overrideConnTimeout > 0) {
                sysConnTimeout = overrideConnTimeout;
            } else {
                sysConnTimeout = connTimeout;
            }
            if (overrideReadTimeout > 0) {
                sysHandleTimeout = overrideReadTimeout;
            } else {
                sysHandleTimeout = readTimeout;
            }

            // builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            // builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            builder.connectTimeout(sysConnTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(sysHandleTimeout, TimeUnit.MILLISECONDS);
            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = requestBuilder.build();

//			Request request = new Request.Builder().url(url).post(requestBody)
//					.addHeader("Content-Type", "application/json").build();
            response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Post调用失败: {}", e.getMessage(), e);
        }finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }


    // 支持多个http header传入
    public <T> String postJsonWithMultiHeaders(String url, T object, Map<String, String> headers) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        Response response = null;
        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();

            builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);

            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = requestBuilder.build();

//				Request request = new Request.Builder().url(url).post(requestBody)
//						.addHeader("Content-Type", "application/json").build();
            response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Post调用失败: {}", e.getMessage(), e);
        }finally {
            try{
                response.close();
            }catch(Exception e){}
        }
        return null;
    }

    public <T> String putJsonWithMultiHeaders(String url, T object, Map<String, String> headers) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request.Builder requestBuilder = new Request.Builder().url(url).put(requestBody);
            // 添加多个HTTP头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = requestBuilder.build();
            Response response = httpClient.newCall(request).execute();
            if(response.isSuccessful()) {
                return response.body().string();
            }else{
                throw new Exception("exexute PUT to url->"+url+"error. Becaused: "+response.body().string());
            }
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp put调用失败: {}", e.getMessage(), e);
        }
        return null;
    }
    /**
     * 发送图片数据到OCR服务
     * @param url OCR服务URL
     * @param imageBytes 图片字节数组
     * @param params 其他参数，如cls等
     * @return OCR识别结果
     */
    public String postImageWithHeaders(String url, byte[] imageBytes, Map<String, String> headers, Map<String, String> params) {
        try {
            // 创建MultipartBody.Builder
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.jpg",
                                     RequestBody.create(MediaType.parse("image/jpeg"), imageBytes));

            // 添加其他表单参数
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    builder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }

            // 构建请求体
            RequestBody requestBody = builder.build();

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(requestBody);

            // 添加所有请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = requestBuilder.build();

            // 发送请求并获取响应
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error(">>>>>>HTTP请求失败: " + response.body().string());
                    return null;
                }

                // 返回响应内容
                return response.body().string();
            }
        } catch (Exception e) {
            logger.error(">>>>>>HTTP请求异常->", e.getMessage(),e);
            return null;
        }
    }
    public <T> Response putJson(String url, T object) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        Response response=null;
        try {
            OkHttpClient.Builder builder = okHttpClient.newBuilder();
            builder.connectTimeout(connTimeout, TimeUnit.MILLISECONDS);
            builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
            if (clientProxy) {
                // 设置代理
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 19180));
                builder.proxy(proxy);
            }
            OkHttpClient httpClient = builder.build();
            String json = mapper.writeValueAsString(object);
            RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
            Request request = new Request.Builder().url(url).put(requestBody).addHeader("Content-Type", "application/json").build();

            response = httpClient.newCall(request).execute();
            return response;
        } catch (Exception e) {
            logger.error(">>>>>>OKHttp Post调用失败: {}", e.getMessage(), e);
        }finally {
            try{
                response.close();
            }catch(Exception e){}
        }
        return null;
    }
}
