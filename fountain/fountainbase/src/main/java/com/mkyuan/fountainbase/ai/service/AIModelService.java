package com.mkyuan.fountainbase.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Service
public class AIModelService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String AI_MODEL_REDIS = "fountain:settings:aimodel:"; //后面跟userName
    private final static String MODEL_ROUTE_REDIS = "fountain:settings:modelroute:"; //后面跟userName
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    //增加一个ai model
    @Transactional(rollbackFor = Exception.class)
    public void addAiModel(String userName, String modelName, int type, String url, String apiKey) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            doAddAiModel(userName, modelName, type, url, apiKey);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    doAddAiModel(userName, modelName, type, url, apiKey);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>addAiModel service error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void doAddAiModel(String userName, String modelName, int type, String url, String apiKey) throws Exception {
        String collectionName = "AIModel";
        AIModel aiModel = new AIModel();
        aiModel.setUserName(userName);
        aiModel.setModelName(modelName);
        aiModel.setType(type);
        aiModel.setUrl(url);
        aiModel.setApiKey(apiKey);
        AIModel returnModel = this.mongoTemplate.save(aiModel, collectionName);
        this.storeAIModelToRedis(userName, returnModel);

    }

    //更新一个ai model
    @Transactional(rollbackFor = Exception.class)
    public void updateAiModel(String userName, String modelId, String modelName, String url, String apiKey)
            throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            doUpdateAIModel(userName, modelId, modelName, url, apiKey);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    doUpdateAIModel(userName, modelId, modelName, url, apiKey);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>updateAiModel service error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void doUpdateAIModel(String userName, String modelId, String modelName, String url, String apiKey)
            throws Exception {
        String collectionName = "AIModel";
        // 创建更新对象

        Query existedObjectQuery = new Query(Criteria.where("_id").is(modelId));
        AIModel aiModel = mongoTemplate.findOne(existedObjectQuery, AIModel.class, collectionName);
        aiModel.setId(modelId);  // 设置_id
        aiModel.setUserName(userName);
        aiModel.setModelName(modelName);
        aiModel.setUrl(url);
        aiModel.setApiKey(apiKey);

        // 使用Query和Update来执行更新操作
        Query query = new Query(Criteria.where("_id").is(modelId));
        Update update = new Update()
                .set("userName", userName)
                .set("modelName", modelName)
                .set("url", url)
                .set("apiKey", apiKey);

        // 执行更新操作，如果不存在则插入
        this.mongoTemplate.upsert(query, update, collectionName);
        this.storeAIModelToRedis(userName, aiModel);

    }

    //删除一个ai model
    @Transactional(rollbackFor = Exception.class)
    public void deleteAiModel(String userName, String modelId) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            this.doDeleteAIModel(userName, modelId);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    this.doDeleteAIModel(userName, modelId);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>deleteAiModel service error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void doDeleteAIModel(String userName, String modelId) throws Exception {
        String collectionName = "AIModel";
        Query query = new Query(Criteria.where("_id").is(modelId));
        mongoTemplate.remove(query, AIModel.class, collectionName);
        this.deleteAIModelFromRedisByModelId(userName, modelId);
    }

    //查询用户所有的aimode设定
    public List<AIModel> getUserAIModelList(String userName) {
        String redisKey = AI_MODEL_REDIS + userName;
        List<AIModel> result = new ArrayList<>();
        try {
            // 先从Redis获取
            Map<Object, Object> aiModelMap = redisTemplate.opsForHash().entries(redisKey);

            if (aiModelMap != null && !aiModelMap.isEmpty()) {
                logger.info(">>>>>>redis里有数据走redis获取");

                aiModelMap.values().forEach(model -> {
                    try {
                        //logger.info(">>>>>>从redis开始获取数据");
                        // 如果是LinkedHashMap，需要转换成JSON字符串再反序列化
                        if (model instanceof LinkedHashMap) {
                            AIModel aiModel = objectMapper.convertValue(model, AIModel.class);
                            result.add(aiModel);
                        } else if (model instanceof String) {
                            // 如果是JSON字符串，直接反序列化
                            AIModel aiModel = objectMapper.readValue(model.toString(), AIModel.class);
                            result.add(aiModel);
                        }
                    } catch (Exception e) {
                        logger.error(">>>>>>redis数据转换失败->{}", e.getMessage(), e);
                    }
                });
            } else {
                // Redis中没有数据，从MongoDB获取
                String collectionName = "AIModel";
                Query query = new Query(Criteria.where("userName").is(userName));
                List<AIModel> mongoResult = mongoTemplate.find(query, AIModel.class, collectionName);

                if (mongoResult != null && !mongoResult.isEmpty()) {
                    logger.info(">>>>>>mongodb里有数据走redis获取");
                    result.addAll(mongoResult);
                    // 将数据存回Redis
                    for (AIModel model : mongoResult) {
                        logger.info(">>>>>>mongodb里的数据->{}", model.getModelName());
                        redisTemplate.opsForHash().put(redisKey, model.getId(), model);
                    }
                }
            }
            // Sort the result list by type before returning
            Collections.sort(result, (a, b) -> a.getType() - b.getType());
            return result;
        } catch (Exception e) {
            logger.error(">>>>>>getUserAIModelList error->{}", e.getMessage(), e);
        }
        return result;
    }

    //根据一个具体的modelId获取一个AIModel对象
    public AIModel getAIModelByModelId(String userName, String modelId) {
        AIModel aiModel = new AIModel();
        try {
            String redisKey = AI_MODEL_REDIS + userName;
            Map<Object, Object> aiModelMap = redisTemplate.opsForHash().entries(redisKey);
            if (aiModelMap != null && !aiModelMap.isEmpty()) {
                logger.info(">>>>>>redis里有数据走redis获取");
                for (Object model : aiModelMap.values()) {
                    try {
                        //logger.info(">>>>>>从redis开始获取数据");
                        // 如果是LinkedHashMap，需要转换成JSON字符串再反序列化
                        if (model instanceof LinkedHashMap) {
                            AIModel existedModel = objectMapper.convertValue(model, AIModel.class);
                            if (modelId.equals(existedModel.getId())) {
                                return existedModel;
                            }
                        } else if (model instanceof String) {
                            // 如果是JSON字符串，直接反序列化
                            AIModel existedModel = objectMapper.convertValue(model, AIModel.class);
                            if (modelId.equals(existedModel.getId())) {
                                return existedModel;
                            }
                        }
                    } catch (Exception e) {
                        logger.error(">>>>>>redis数据转换失败->{}", e.getMessage(), e);
                    }
                }
            } else {
                String collectionName = "AIModel";
                logger.info(">>>>>>走mongodb取数据");
                Query query = new Query(Criteria.where("_id").is(modelId));
                AIModel existedModel = mongoTemplate.findOne(query, AIModel.class, collectionName);
                if (existedModel != null) {
                    redisTemplate.opsForHash().put(redisKey, existedModel.getId(), existedModel);//先放redis再返回
                    return existedModel;
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>getAIModelByModelId error->{}", e.getMessage(), e);
        }
        return aiModel;
    }

    //设置线路
    public int setAIModelRoute(String userName, String modelId, int type) {
        String redisKey = MODEL_ROUTE_REDIS + userName;
        try {
            Map<String, String> router = new HashMap<>();
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                router = (Map<String, String>) obj;
                if (router.containsKey(String.valueOf(type))) {
                    if (type == 1) {
                        return 101; //己有主，不可重复设置
                    } else if (type == 2) {
                        return 102;//己有从,不可重复设置
                    } else {
                        return -1;//失败，因为系统只允许1主1从
                    }
                } else {
                    router.put(String.valueOf(type), String.valueOf(modelId));
                    redisTemplate.opsForValue().set(redisKey, router);
                    return 1;//返回成功设置标志
                }
            } else {
                router.put(String.valueOf(type), String.valueOf(modelId));
                redisTemplate.opsForValue().set(redisKey, router);
                return 1;//返回成功设置标志
            }
        } catch (Exception e) {
            logger.error(">>>>>>setAIModelRoute error->{}", e.getMessage(), e);
        }
        return -1;
    }

    //unset线路
    public int unsetAIModelRoute(String userName, String modelId, int type) {
        String redisKey = MODEL_ROUTE_REDIS + userName;
        try {
            Map<String, String> router = new HashMap<>();
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                router = (Map<String, String>) obj;
                if (router.containsKey(
                        String.valueOf(type))) { //只有当redis里含有这条记录才做反向unset即delete rediskey, hashkey操作，否则都返回1
                    router.remove(String.valueOf(type));
                    redisTemplate.opsForValue().set(redisKey, router);
                    return 1;
                }
            }
            return 1;
        } catch (Exception e) {
            logger.error(">>>>>>unSetAIModelRoute service error->{}", e.getMessage(), e);
        }
        return -1;
    }

    //通过userName, modelId获取这个AIModel是主线路还是从线路
    public int getRouteTypeByModelId(String userName, String modelId) {
        String redisKey = MODEL_ROUTE_REDIS + userName;
        try {
            Map<String, String> router = new HashMap<>();
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                router = (Map<String, String>) obj;
                for (Map.Entry<String, String> entry : router.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value.equals(modelId)) {
                        logger.info(">>>>>>modelId->{} type is ->{}", modelId, key);
                        return Integer.valueOf(key);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>getRouteTypeByModelId service error->{}", e.getMessage(), e);
        }
        return 0;//即不是主也不是从
    }

    //每个用户的可用模型为一个hash，主键是AI_MODEL_REDIS+username，hashKey为AIModel的id
    private void storeAIModelToRedis(String userName, AIModel aiModel) throws Exception {
        String redisKey = AI_MODEL_REDIS + userName;
        redisTemplate.opsForHash().put(redisKey, aiModel.getId(), aiModel);
    }

    //从redis里删除model为先找到AI_MODEL_REDIS+username，再通过hashKey: modelId删除这条记录
    private void deleteAIModelFromRedisByModelId(String userName, String modelId) throws Exception {
        String redisKey = AI_MODEL_REDIS + userName;
        redisTemplate.opsForHash().delete(redisKey, modelId);
        String routeRedis = MODEL_ROUTE_REDIS + userName;
        Map<String, String> router = new HashMap<>();
        Object obj = redisTemplate.opsForValue().get(routeRedis);
        if (obj != null) {
            router = (Map<String, String>) obj;
            Iterator<Map.Entry<String, String>> iterator = router.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getValue().equals(modelId)) {
                    iterator.remove(); // 安全地删除元素
                }
            }
            if(router.isEmpty()){
                redisTemplate.delete(routeRedis);//如果router为空，直接删除这条route在redis里的值
            }else{
                redisTemplate.opsForValue().set(routeRedis,router);//把删除后的值再set回redis
            }
        }
    }

}
