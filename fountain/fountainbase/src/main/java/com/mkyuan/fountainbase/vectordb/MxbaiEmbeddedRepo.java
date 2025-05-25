package com.mkyuan.fountainbase.vectordb;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.knowledge.bean.FieldSchema;
import com.mkyuan.fountainbase.knowledge.bean.VectorIndexBean;
import com.mkyuan.fountainbase.knowledge.bean.VectorIndexRequest;
import com.mkyuan.fountainbase.user.bean.VectorCollection;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.PointsWrapper;
import com.mkyuan.fountainbase.vectordb.bean.query.Filter;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryRequest;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResponse;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResult;
import jodd.util.StringUtil;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MxbaiEmbeddedRepo implements VectorDbRepo {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private OkHttpHelper okHttpHelper;

    @Value("${fountain.vectordb.uri}")
    private String uri = "";

    @Value("${fountain.vectordb.apiKey}")
    private String apiKey = "";

    @Value("${fountain.ai.embedding.embeddingUrl}")
    private String embeddingUrl = "";

    @Value("${fountain.ai.embedding.model}")
    private String model = "";

    @Override
    public void makeIndex(VectorIndexRequest vectorIndexRequest, String indexAddr) {
        String status = "unknown";
        VectorIndexBean indexPayload = new VectorIndexBean();
        try {

            indexPayload.setField_name(vectorIndexRequest.getFieldName());
            FieldSchema fieldSchema = new FieldSchema();
            fieldSchema.setType(vectorIndexRequest.getFieldType());

            fieldSchema.setTokenizer(vectorIndexRequest.getTokenizer());
            fieldSchema.setMin_token_len(vectorIndexRequest.getMin_token_len());
            fieldSchema.setMax_token_len(vectorIndexRequest.getMax_token_len());
            fieldSchema.setLowercase(vectorIndexRequest.isLowercase());
            indexPayload.setField_schema(fieldSchema);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("api-key", apiKey);
            HttpUrl.Builder urlBuilder = HttpUrl.parse(indexAddr).newBuilder();
            urlBuilder.addQueryParameter("wait", "true");
            String url = urlBuilder.build().toString();
            String jsonResponse = okHttpHelper.putJsonWithMultiHeaders(url, indexPayload, headers);
            JSONObject jsonObject = JSON.parseObject(jsonResponse);
            status = jsonObject.getString("status");
            if (status.equals("ok")) {
                logger.info(">>>>>>索引成功->{}", jsonResponse);
            } else {
                logger.error(">>>>>>索引失败->{}", jsonResponse);
            }
        } catch (Exception e) {
            logger.error(">>>>>索引时异常->{}", e.getMessage(), e);
        }
    }

    @Override
    public void createRepo(String repoName) throws Exception {
        try {
            String url = uri + "/" + repoName;
            VectorCollection vectorCollection = new VectorCollection();
            VectorCollection.Vectors vectors = new VectorCollection.Vectors();
            vectors.setDistance("Cosine");
            vectors.setSize(1024);
            vectorCollection.setVectors(vectors);
            //放置header
            Map<String, String> headers = new HashMap<>();
            headers.put("api-key", apiKey);
            String responseStr = okHttpHelper.putJsonWithMultiHeaders(url, vectorCollection, headers);
            logger.info(">>>>>>create vectordb repo success with->{}", responseStr);
        } catch (Exception e) {
            logger.error(">>>>>>createRepo service error->{}", e.getMessage(), e);
            throw new Exception(e);
        }
    }

    @Override
    public void deleteRepo(String repoName) throws Exception {
        try {
            StringBuilder urlStr = new StringBuilder();
            urlStr.append(uri).append("/").append(repoName).append("?wait=true");

            //放置header
            Map<String, String> headers = new HashMap<>();
            headers.put("api-key", apiKey);
            String responseStr = okHttpHelper.delete(urlStr.toString(), headers, 10000, 10000);
            logger.info(">>>>>>delete vectordb repo success with->{}", responseStr);
        } catch (Exception e) {
            logger.error(">>>>>>delete service error->{}", e.getMessage(), e);
            throw new Exception(e);
        }
    }
    @Override
    public List<Float> getVlEmbedding(String inputText,String imgData) throws Exception{
        List<Float> vector=new ArrayList<>();
        return vector;
    }
    @Override
    public List<Float> getEmbedding(String inputText) throws Exception {
        List<Float> vectorResult = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            // logger.info(">>>>>>api-key-> {}", apiKey);
            headers.put("Content-Type", "application/json");
            //headers.put("api-key", textEmbeddingApiKey);
            Map<String, String> input = new HashMap<>();
            input.put("input", inputText);
            input.put("model", model);
            logger.info(">>>>>>getEmbedding =>addr为->{}", embeddingUrl);
            String jsonStr = okHttpHelper.postJsonWithMultiHeaders(embeddingUrl, input, headers, 15000, 15000);
            logger.debug(">>>>>>getEmbedding模型返回的result->{}", jsonStr);
            if (StringUtil.isNotBlank(jsonStr)) {
                // 使用JsonNode来解析
                JsonNode rootNode = objectMapper.readTree(jsonStr);
                JsonNode embeddingsNode = rootNode.get("embeddings");
                if (embeddingsNode != null && embeddingsNode.isArray() && embeddingsNode.size() > 0) {
                    // 获取第一个embedding数组
                    JsonNode firstEmbedding = embeddingsNode.get(0);
                    if (firstEmbedding != null && firstEmbedding.isArray()) {
                        vectorResult = new ArrayList<>();
                        for (JsonNode value : firstEmbedding) {
                            vectorResult.add(value.floatValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception(">>>>>>通过Embedding获取向量值出错" + e.getMessage(), e);
            //throw new Exception(">>>>>>通过Embedding获取向量值出错->" + e.getMessage(), e);
        }
        return vectorResult;
    }

    @Override
    public void addBatchEmbeddingItemIntoVectorDB(PointsWrapper points, String url) throws Exception {
        try {
            //String knowledgeAddr=this.removeLastSegment(url);
            //knowledgeAddr=knowledgeAddr+"/"+knowledgeRepoName;
            this.logger.info("存储数据到QDrant -> {}", url);

            //PointsWrapper vectorPointsBean = new PointsWrapper();
            //Point p = new Point();
            //p.setVector(new ArrayList<>(point));
            //p.setPayload(payload);
            //p.setId(pointId);
            // 最后我们把这个Point变成List结构并完成最终发送报文前的全部工作
            //vectorPointsBean.getPoints().add(p);
            // 再做插入
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("api-key", apiKey);
            String jsonResponse = okHttpHelper.putJsonWithMultiHeaders(url, points, headers);
            logger.info(">>>>>>after addBatchjsonResponse->{}", jsonResponse);

        } catch (Exception e) {
            logger.error(">>>>>>把一条数据插入到vectordb错误: {}", e.getMessage(), e);
            throw new Exception(">>>>>>把一条数据插入到vectordb错误: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QDRantQueryResult> queryWithVector(String url, List<Float> vector, Filter queryFilter, int top) {
        String content = "";
        logger.info(">>>>>>queryWithVector top->{}", top);
        QDRantQueryResponse queryResp = null;
        QDRantQueryRequest inputBean = new QDRantQueryRequest();
        List<QDRantQueryResult> queryRespResults = new ArrayList<>();
        String searchUrl = uri + "/" + url + "/points/search";
        try {
            if (vector != null) {
                inputBean.setVector(new ArrayList<>(vector));
                inputBean.setTop(top);
                inputBean.setFilter(queryFilter);
                inputBean.setWith_payload(true);
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("api-key", apiKey);
                String responseBody = this.okHttpHelper.postJsonWithMultiHeaders(searchUrl, inputBean, headers);
                Gson gson = new Gson();
                Type type = new TypeToken<QDRantQueryResponse>() {
                }.getType();
                queryResp = gson.fromJson(responseBody, type);
                // embeddingValues = embeddingResponse.getData().get(0).getEmbedding();
                logger.info(">>>>>>在知识库内查到 {} 条数据", queryResp.getResult().size());
                queryRespResults = queryResp.getResult(); // 获得主库的查询信息
            }
        } catch (Exception e) {
            logger.error(">>>>>>通过vectorDB的point查询vector db失败->{}", e.getMessage(), e);
        }
        return queryRespResults;
    }

    @Override
    public void deletePoints(String collectionName, List<Long> pointIds)throws Exception {
        String deleteUrl = uri + "/" + collectionName + "/points/delete" + "?wait=true";
        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", apiKey);
        Map<String, Object> payload = new HashMap<>();
        payload.put("points", pointIds);
        try {
            String responseJson = this.okHttpHelper.postJsonWithMultiHeaders(deleteUrl, payload, headers);
            logger.info(">>>>>>delete point from qdrant response is->{}", responseJson);

            // 假设responseJson是包含JSON响应的字符串
            JSONObject jsonObject = JSON.parseObject(responseJson);

            // 获取外层的status值
            String status = jsonObject.getString("status");  // 将得到 "ok"

            // 获取result对象中的status值
            String complete = jsonObject.getJSONObject("result").getString("status");  // 将得到 "completed"
            if (!status.equalsIgnoreCase("ok") || !complete.equalsIgnoreCase("completed")) {
                throw new Exception("deletePoint error->" + responseJson);
            }

        } catch (Exception e) {
            logger.error(">>>>>>delete point from qdrant error->{}", e.getMessage(), e);
        }

    }

    @Override
    public void deleteRepoVl(String repoName) throws Exception {

    }

    @Override
    public void deleteVlPoints(String collectionName, List<Long> pointIds) throws Exception {

    }

}
