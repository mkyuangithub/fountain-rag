package com.mkyuan.fountainbase.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.bean.DifyBean;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DifyService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String DIFY_REDIS_PREFIX = "fountain:settings:dify:"; //后面跟userName
    private ObjectMapper objectMapper = new ObjectMapper();
    public final static int PAGESIZE = 20;


    @Value("${fountain.secretKey}")
    private String secretKey = "";


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    public int addDifyConfig(String userName, DifyBean difyBean) {
        String collectionName = "DifyConfig";
        try {

            DifyBean existedItem = this.findDifyConfigBySequenceNo(userName, difyBean.getSequenceNo());
            if (existedItem != null) {
                return 2; //己存在相同的sequenceNo
            }
            mongoTemplate.save(difyBean, collectionName);
            String redisKey = DIFY_REDIS_PREFIX + userName;
            redisTemplate.opsForHash().put(redisKey, difyBean.getSequenceNo(), difyBean);
            return 1;//保存成功
        } catch (Exception e) {
            logger.error(">>>>>>addDifyConfig error->{}", e.getMessage(), e);
        }
        return 0;//保存失败
    }

    public Page<DifyBean> listAllDifyConfigs(String userName, int pageNumber, int pageSize) {
        String collectionName = "DifyConfig";
        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<DifyBean> difyConfigs = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("userName").is(userName));
            query.with(Sort.by(Sort.Direction.DESC, "updatedDate"));
            long count = mongoTemplate.count(query, DifyBean.class, collectionName);
            List<DifyBean> configs = mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)),
                                                        DifyBean.class, collectionName);
            List<DifyBean> result = new ArrayList<>();
            for (DifyBean difyBean : configs) {
                String decryptApiKey = EncryptUtil.decrypt_safeencode(difyBean.getApiKey(), secretKey);
                logger.info(">>>>>>encryptApiKey->{} decryptApiKey->{}", difyBean.getApiKey(), decryptApiKey);
                difyBean.setApiKey(decryptApiKey);
                result.add(difyBean);
            }
            return new PageImpl<>(result, PageRequest.of(pageNumber - 1, pageSize), count);
        } catch (Exception e) {
            logger.error(">>>>>>listAllDifyConfigs error->{}", e.getMessage(), e);
        }
        return difyConfigs;
    }

    public void updateDifyConfigs(String userName, DifyBean updatedDifyBean) {
        String collectionName = "DifyConfig";
        String redisKey = DIFY_REDIS_PREFIX + userName;
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(updatedDifyBean.getId()));
            Update update = new Update()
                    .set("description", updatedDifyBean.getDescription())
                    .set("responseMode", updatedDifyBean.getResponseMode())
                    .set("user", updatedDifyBean.getUser())
                    .set("apiKey", updatedDifyBean.getApiKey())
                    .set("updatedDate", new Date());
            mongoTemplate.updateFirst(query, update, collectionName);
            if (redisTemplate.hasKey(redisKey)) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    DifyBean difyBean = objectMapper.convertValue(entry.getValue(), DifyBean.class);
                    //logger.info(">>>>>>从redis里找到记录 id-> hashKey->{} 要删除的id->{}",difyBean.getId(),difyBean.getSequenceNo(),id);
                    // 比较ID是否相等
                    if (difyBean.getId().equals(difyBean.getId())) {
                        //logger.info(">>>>>>从redis里删除key->{} hashKey->{}",redisKey,difyBean.getSequenceNo());
                        redisTemplate.opsForHash().put(redisKey, updatedDifyBean.getSequenceNo(), updatedDifyBean);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>updatedDifyConfig error->{}", e.getMessage());
        }
    }

    public void deleteDifyConfigs(String userName, List<String> idList) {
        String collectionName = "DifyConfig";
        String redisKey = DIFY_REDIS_PREFIX + userName;
        if (idList == null || idList.isEmpty()) {
            return;
        }

        try {
            // 创建查询条件
            Query query = new Query(Criteria.where("userName").is(userName)
                                            .and("_id").in(idList));
            // 执行删除操作
            mongoTemplate.remove(query, collectionName);
            if (redisTemplate.hasKey(redisKey)) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
                for (String id : idList) {
                    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                        DifyBean difyBean = objectMapper.convertValue(entry.getValue(), DifyBean.class);
                        //logger.info(">>>>>>从redis里找到记录 id-> hashKey->{} 要删除的id->{}",difyBean.getId(),difyBean.getSequenceNo(),id);
                        // 比较ID是否相等
                        if (difyBean.getId().equals(id)) {
                            //logger.info(">>>>>>从redis里删除key->{} hashKey->{}",redisKey,difyBean.getSequenceNo());
                            redisTemplate.opsForHash().delete(redisKey, difyBean.getSequenceNo());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            logger.error(">>>>>>Delete DifyConfigs failed->{} ", e.getMessage(), e);
        }
    }

    public DifyBean findDifyConfigById(String userName, String id) {
        String collectionName = "DifyConfig";
        String redisKey = DIFY_REDIS_PREFIX + userName;
        DifyBean existedItem = null;
        boolean isFound = false;
        if (!redisTemplate.hasKey(redisKey)) {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(id));
            existedItem = mongoTemplate.findOne(query, DifyBean.class, collectionName);
            if (existedItem != null) {
                redisTemplate.opsForHash().put(redisKey, existedItem.getSequenceNo(), existedItem);
            }
        } else {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
            // 从Redis中获取Hash结构的所有数据
            // 遍历Hash中的数据
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                DifyBean difyBean = objectMapper.convertValue(entry.getValue(), DifyBean.class);
                // 比较ID是否相等
                if (difyBean.getId().equals(id)) {
                    existedItem = difyBean;
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(id));
                existedItem = mongoTemplate.findOne(query, DifyBean.class, collectionName);
                if (existedItem != null) {
                    redisTemplate.opsForHash().put(redisKey, existedItem.getSequenceNo(), existedItem);
                }
            }
        }
        return existedItem;
    }

    public DifyBean findDifyConfigBySequenceNo(String userName, String sequenceNo) {
        String collectionName = "DifyConfig";
        String redisKey = DIFY_REDIS_PREFIX + userName;
        DifyBean existedItem = null;
        boolean isFound = false;
        if (!redisTemplate.hasKey(redisKey)) {
            Query query = new Query();
            query.addCriteria(Criteria.where("sequenceNo").is(sequenceNo));
            existedItem = mongoTemplate.findOne(query, DifyBean.class, collectionName);
            if (existedItem != null) {
                redisTemplate.opsForHash().put(redisKey, existedItem.getSequenceNo(), existedItem);
            }
        } else {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                DifyBean difyBean = objectMapper.convertValue(entry.getValue(), DifyBean.class);
                // 比较sequenceNo是否相等
                if (difyBean.getSequenceNo().equals(sequenceNo)) {
                    existedItem = difyBean;
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                Query query = new Query();
                query.addCriteria(Criteria.where("sequenceNo").is(sequenceNo));
                existedItem = mongoTemplate.findOne(query, DifyBean.class, collectionName);
                if (existedItem != null) {
                    redisTemplate.opsForHash().put(redisKey, existedItem.getSequenceNo(), existedItem);
                }
            }
        }
        return existedItem;
    }
}
