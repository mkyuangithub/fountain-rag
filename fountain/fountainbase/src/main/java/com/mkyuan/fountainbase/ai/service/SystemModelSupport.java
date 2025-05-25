package com.mkyuan.fountainbase.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class SystemModelSupport {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String AI_MODEL_REDIS = "fountain:settings:aimodel:"; //后面跟userName
    private final static String MODEL_ROUTE_REDIS = "fountain:settings:modelroute:"; //后面跟userName
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AIModelService aiModelService;

    public AIModel getMasterModel(String userName) throws Exception {
        AIModel aiModel = new AIModel();
        String redisKey = MODEL_ROUTE_REDIS + userName;
        try {
            Map<String, String> router = new HashMap<>();
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                logger.info(">>>>>>寻找全局Master AIModel->redis里设置了modelRoute");
                router = (Map<String, String>) obj;
                Iterator<Map.Entry<String, String>> iterator = router.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    int type = Integer.valueOf(entry.getKey());
                    if (type == 1) {
                        //取出modelId
                        String masterModelId = entry.getValue();
                        logger.info(">>>>>>寻找全局Master AIModel->得到master model id->{}", masterModelId);
                        //通过modelId取aiModel对象
                        aiModel=this.aiModelService.getAIModelByModelId(userName,masterModelId);
                        return aiModel;
                    }
                }
            } else {
                logger.info(">>>>>>寻找全局Master AIModel->redis里没有设置modelRoute，拿全局aiModel的type==1的值");
                //系统没有设置主也没有设置从，因此AIModel取type=1。
                aiModel = this.getAIModeByTypeId(userName, 1);
                return aiModel;
            }
        } catch (Exception e) {
            logger.error(">>>>>>getMasterModel error->{}", e.getMessage(), e);
        }
        return null;
    }
    public AIModel getSlave(String userName) {
        AIModel aiModel = new AIModel();
        String redisKey = MODEL_ROUTE_REDIS + userName;
        try {
            Map<String, String> router = new HashMap<>();
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                logger.info(">>>>>>寻找全局Slave AIModel->redis里设置了modelRoute");
                router = (Map<String, String>) obj;
                Iterator<Map.Entry<String, String>> iterator = router.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    int type = Integer.valueOf(entry.getKey());
                    if (type == 2) {
                        //取出modelId
                        String slaveModelId = entry.getValue();
                        logger.info(">>>>>>寻找全局Slave AIModel->得到slave model id->{}", slaveModelId);
                        //通过modelId取aiModel对象
                        aiModel=this.aiModelService.getAIModelByModelId(userName,slaveModelId);
                        return aiModel;
                    }
                }
            } else {
                logger.info(">>>>>寻找全局Salve AIModel->>redis里没有设置modelRoute，拿全局aiModel的type==3的值");
                //系统没有设置主也没有设置从，因此AIModel取type=1。
                aiModel = this.getAIModeByTypeId(userName, 3);
                return aiModel;
            }
        } catch (Exception e) {
            logger.error(">>>>>>getSlaveModel error->{}", e.getMessage(), e);
        }
        return null;
    }

    private AIModel getAIModeByTypeId(String userName, int type) throws Exception {
        int pc = 0;
        List<AIModel> aiModelList = this.aiModelService.getUserAIModelList(userName);
        for (AIModel model : aiModelList) {
            if (model.getType() == type) {
                pc++;
                return model;
            }
        }
        return null;
    }

}
