package com.mkyuan.fountainbase.common.util.es;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ESHelper {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private RedisTemplate redisTemplate;
    public void deleteEsIndex(String indexName) {
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            AcknowledgedResponse deleteIndexResponse = esClient.indices()
                                                               .delete(request, RequestOptions.DEFAULT);
            if (deleteIndexResponse.isAcknowledged()) {
                logger.info(">>>>>>delete es index success, index name: {}", indexName);
            }
        } catch (ElasticsearchStatusException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                logger.info(">>>>>>index not found, index name: {}, skipit", indexName);
                return;
            }
            logger.error(">>>>>>delete es index error->{}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error(">>>>>>delete es index error->{}", e.getMessage(), e);
        }
    }

    /**
     * 向ES中插入或更新单条数据
     * @param indexName 索引名称
     * @param docId 文档ID
     * @param docData 文档数据(Map格式)
     * @return 操作结果
     */
    public void insertOrUpdateDocument(String indexName, String docId, Map<String, Object> docData) {
        try {

            // 创建索引请求
            IndexRequest indexRequest = new IndexRequest(indexName)
                    .id(docId)
                    .source(docData, XContentType.JSON);

            // 执行索引请求
            IndexResponse response = esClient.index(indexRequest, RequestOptions.DEFAULT);

            // 检查响应结果
            if (response.getResult() == DocWriteResponse.Result.CREATED) {
                logger.info(">>>>>>文档已创建，索引: {}, ID: {}", indexName, response.getId());
            } else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
                logger.info(">>>>>>文档已更新，索引: {}, ID: {}", indexName, response.getId());
            } else {
                logger.warn(">>>>>>文档操作结果: {}, 索引: {}, ID: {}",
                            response.getResult(), indexName, response.getId());
            }

        } catch (Exception e) {
            logger.error(">>>>>>插入/更新文档时发生错误，索引: {}, ID: {}, 错误: {}",
                         indexName, docId, e.getMessage(), e);
        }
    }
    public void createIndexIfNotExists(String indexName) {
        try {
            // 正确的构造函数使用方式
            GetIndexRequest request = new GetIndexRequest().indices(indexName);
            // 或者这样写也可以
            // GetIndexRequest request = new GetIndexRequest(new String[]{indexName});
            boolean exists = esClient.indices().exists(request, RequestOptions.DEFAULT);

            if (!exists) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

                // 单节点环境的简单配置
                createIndexRequest.settings(Settings.builder()
                                                    .put("index.number_of_shards", 1)      // 只需要1个分片
                                                    .put("index.number_of_replicas", 0)    // 单节点不需要副本
                );
                // 使用 XContentBuilder 构建映射
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.startObject();
                {
                    builder.startObject("properties");
                    {
                        // itemId字段定义
                        builder.startObject("itemId");
                        {
                            builder.field("type", "long");
                        }
                        builder.endObject();

                        // labels字段定义（在properties内部）
                        builder.startObject("labels");
                        {
                            builder.field("type", "keyword");
                            builder.startObject("fields");
                            {
                                builder.startObject("full");
                                {
                                    builder.field("type", "text");
                                    builder.field("analyzer", "ik_max_word");
                                }
                                builder.endObject();
                            }
                            builder.endObject();
                        }
                        builder.endObject();

                        // 如果有其他字段，继续在这里定义...
                    }
                    builder.endObject();
                }
                builder.endObject();

                createIndexRequest.mapping("_doc", builder);

                CreateIndexResponse createIndexResponse = esClient.indices().create(
                        createIndexRequest, RequestOptions.DEFAULT);

                if (createIndexResponse.isAcknowledged()) {
                    logger.info(">>>>>>索引 {} 创建成功", indexName);
                } else {
                    logger.info(">>>>>>索引 {} 创建失败", indexName);
                }
            } else {
                logger.info(">>>>>>索引 {} 已存在，无需创建", indexName);
            }
        } catch (Exception e) {
            logger.error(">>>>>>检查或创建索引 {} 时发生错误: {}", indexName, e.getMessage());
            throw new RuntimeException("索引操作失败", e);
        }
    }

    public void deleteOneEsItem(String indexName, long itemId) {
        try {
            // 将 long 类型的 itemId 转换为字符串
            String docId = String.valueOf(itemId);

            DeleteRequest deleteRequest = new DeleteRequest(indexName, docId);

            // 执行删除操作
            DeleteResponse deleteResponse = esClient.delete(deleteRequest, RequestOptions.DEFAULT);

            // 检查删除结果
            if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
                logger.info(">>>>>>文档已成功删除, 索引: {}, 文档ID: {}", indexName, docId);
            } else if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                logger.warn(">>>>>>文档不存在, 索引: {}, 文档ID: {}", indexName, docId);
            }
        } catch (Exception e) {
            logger.error(">>>>>>deleteOneEsItem error->{}", e.getMessage(), e);
        }
    }

    public boolean queryEsStatus(){
        String redisKey = "fountain:settings:es:available";
        boolean esAvailable=false;
        try {
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                esAvailable = (boolean) obj;
            }
        }catch(Exception e){
            logger.error(">>>>>>queryEsStatus error->{}",e.getMessage(),e);
        }
        return esAvailable;
    }
}
