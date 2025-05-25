package com.mkyuan.fountainbase.agent.chatbot.service;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigBackup;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigMain;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChatConfigHelper {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    //删除对话配置

    public void deleteChatConfig(String userName, long configMainId) {
        String mainCollection = "ChatConfigMain";
        String detailCollection = "ChatConfigDetail";
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        String configDetailRedisKey = "fountain:settings:chatconfigdetail:list:" + userName;

        //删除mongo里的ChatConfigMain
        Query mainQuery = new Query();
        mainQuery.addCriteria(Criteria.where("_id").is(configMainId));
        mongoTemplate.remove(mainQuery, mainCollection);
        //-删除redis里的ChatConfigMain
        Object configMainObj = redisTemplate.opsForValue().get(configMainRedisKey);
        if (configMainObj != null) {
            List<ChatConfigMain> configList = (List<ChatConfigMain>) configMainObj;
            // 使用Iterator来安全地删除元素
            Iterator<ChatConfigMain> iterator = configList.iterator();
            while (iterator.hasNext()) {
                ChatConfigMain config = iterator.next();
                if (config.getId() == configMainId) {
                    iterator.remove();
                    break;  // 找到并删除后即退出循环
                }
            }
            // 更新Redis中的值
            redisTemplate.opsForValue().set(configMainRedisKey, configList);
        }

        //删除mongo里的ChatConfigDetail
        Query detailQuery = new Query();
        detailQuery.addCriteria(Criteria.where("configMainId").is(configMainId));
        mongoTemplate.remove(detailQuery, detailCollection);
        //-删除redis里的ChatconfigDetail
        redisTemplate.opsForHash().delete(configDetailRedisKey, String.valueOf(configMainId));
    }

    //增加一个完整的对话配置
    public void doAddChatConfig(String userName, ChatConfigMain chatConfigMain) {
        String mainCollectionName = "ChatConfigMain";
        String detailCollectionName = "ChatConfigDetail";
        List<ChatConfigMain> existedList = new ArrayList<>();
        mongoTemplate.save(chatConfigMain, mainCollectionName);


        //store configMain into redis
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        Object obj = redisTemplate.opsForValue().get(configMainRedisKey);
        if (obj != null) {
            existedList = (List<ChatConfigMain>) obj;
        }
        existedList.add(chatConfigMain);
        redisTemplate.opsForValue().set(configMainRedisKey, existedList);
    }

    //更新一个完整的对话配置
    public void updateChatConfig(String userName, ChatConfigMain chatConfigMain) {
        String configMainCollection = "ChatConfigMain";
        String configDetailCollection = "ChatConfigDetail";
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        String configDetailRedisKey = "fountain:settings:chatconfigdetail:list:" + userName;
        //更新configMain
        // 先更新mongo里的数据
        Query mainQuery = new Query();
        mainQuery.addCriteria(Criteria.where("_id").is(chatConfigMain.getId()));
        Update mainUpdate = new Update();
        mainUpdate.set("description", chatConfigMain.getDescription())
                  .set("temperature", chatConfigMain.getTemperature()).set("systemMsg", chatConfigMain.getSystemMsg())
                  .set("groovyRules", chatConfigMain.getGroovyRules())
                  .set("updatedDate", new Date()).set("knowledgeRepoList", chatConfigMain.getKnowledgeRepoIdList())
                  .set("rewriteSelectedDifySequenceNo", chatConfigMain.getRewriteSelectedDifySequenceNo())
                  .set("chatSelectedDifySequenceNo", chatConfigMain.getChatSelectedDifySequenceNo())
                  .set("allowUsers", chatConfigMain.getAllowUsers());
        mongoTemplate.updateFirst(mainQuery, mainUpdate, configMainCollection);
        // 再更新redis里的数据
        List<ChatConfigMain> existedList = new ArrayList<>();
        Object obj = redisTemplate.opsForValue().get(configMainRedisKey);
        if (obj != null) {
            existedList = (List<ChatConfigMain>) obj;
            for (ChatConfigMain existedItem : existedList) {
                if (existedItem.getId() == chatConfigMain.getId()) {
                    existedItem.setDescription(chatConfigMain.getDescription());
                    existedItem.setSystemMsg(chatConfigMain.getSystemMsg());
                    existedItem.setGroovyRules(chatConfigMain.getGroovyRules());
                    existedItem.setTemperature(chatConfigMain.getTemperature());
                    existedItem.setAllowUsers(chatConfigMain.getAllowUsers());
                    existedItem.setKnowledgeRepoIdList(chatConfigMain.getKnowledgeRepoIdList());
                    existedItem.setUpdatedDate(new Date());
                    existedItem.setRewriteSelectedDifySequenceNo(chatConfigMain.getRewriteSelectedDifySequenceNo());
                    existedItem.setChatSelectedDifySequenceNo(chatConfigMain.getChatSelectedDifySequenceNo());
                    break;
                }
            }
            //existedList.add(chatConfigMain);
        } else {
            existedList = new ArrayList<>();
            existedList.add(chatConfigMain);
        }
        redisTemplate.opsForValue().set(configMainRedisKey, existedList);


    }

    public ChatConfigMain getChatConfigMainById(String userName, long mainId) {
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        ChatConfigMain chatConfigMain = new ChatConfigMain();
        Object obj = redisTemplate.opsForValue().get(configMainRedisKey);
        if (obj != null) {
            List<ChatConfigMain> configList = (List<ChatConfigMain>) obj;
            int pc = 0;
            for (ChatConfigMain mainItem : configList) {
                if (mainItem.getId() == mainId) {
                    pc++;
                    return mainItem;
                }
            }
            if (pc < 1) {
                chatConfigMain = this.getChatConfigMainFromMongo(userName, mainId);
                if (chatConfigMain != null) {
                    configList.add(chatConfigMain);
                    redisTemplate.opsForValue().set(configMainRedisKey, configList);
                    return chatConfigMain;
                }
            }
        } else {
            chatConfigMain = this.getChatConfigMainFromMongo(userName, mainId);
            if (chatConfigMain != null) {
                List<ChatConfigMain> configList = new ArrayList<>();
                configList.add(chatConfigMain);
                redisTemplate.opsForValue().set(configMainRedisKey, configList);
                return chatConfigMain;
            }
        }
        return null;
    }

    private ChatConfigMain getChatConfigMainFromMongo(String username, long mainId) {
        ChatConfigMain chatConfigMain = new ChatConfigMain();
        String collectionName = "ChatConfigMain";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(mainId));
        chatConfigMain = mongoTemplate.findOne(query, ChatConfigMain.class, collectionName);
        return chatConfigMain;
    }

    public List<ChatConfigDetail> getChatConfigDetailByMainId(String userName, long mainId) {
        List<ChatConfigDetail> resultList = new ArrayList<>();
        String configDetailRedisKey = "fountain:settings:chatconfigdetail:list:" + userName;
        ChatConfigDetail chatConfigDetail1 = new ChatConfigDetail();
        Object obj = redisTemplate.opsForHash().get(configDetailRedisKey, String.valueOf(mainId));
        if (obj != null) {
            List<LinkedHashMap<String, Object>> mapList = (List<LinkedHashMap<String, Object>>) obj;
            for (LinkedHashMap<String, Object> map : mapList) {
                ChatConfigDetail detail = convertMapToChatConfigDetail(map);
                resultList.add(detail);
            }
            return resultList;
        } else {
            resultList = this.getChatConfigDetailFromMongo(userName, mainId);
            if (resultList != null && !resultList.isEmpty()) {
                redisTemplate.opsForHash().put(configDetailRedisKey, String.valueOf(mainId), resultList);
                return resultList;
            }
        }
        return new ArrayList<>();
    }

    // 添加转换方法
    private ChatConfigDetail convertMapToChatConfigDetail(Map<String, Object> map) {
        ChatConfigDetail detail = new ChatConfigDetail();
        // 根据ChatConfigDetail的属性进行设置
        // 示例：假设ChatConfigDetail有id和name属性
        //if (map.containsKey("id")) {
        //    detail.setId(map.get("id").toString());
        //}
        if (map.containsKey("configMainId")) {
            detail.setConfigMainId(((Number) map.get("configMainId")).longValue());
        }
        if (map.containsKey("sequence")) {
            detail.setConfigMainId(((Number) map.get("sequence")).intValue());
        }
        if (map.containsKey("stepDescription")) {
            detail.setStepDescription(map.get("stepDescription").toString());
        }
        if (map.containsKey("type")) {
            detail.setType(((Number) map.get("type")).intValue());
        }
        if (map.containsKey("enabled")) {
            detail.setEnabled(((Boolean) map.get("enabled")).booleanValue());
        }
        if (map.containsKey("stepPrompt")) {
            detail.setStepPrompt(map.get("stepPrompt").toString());
        }
        if (map.containsKey("temperature")) {
            detail.setTemperature(((Double) map.get("temperature")).doubleValue());
        }
        if (map.containsKey("userName")) {
            detail.setUserName(((String) map.get("userName")).toString());
        }
        if (map.containsKey("createdBy")) {
            detail.setUserName(((String) map.get("createdBy")).toString());
        }
        return detail;
    }

    private List<ChatConfigDetail> getChatConfigDetailFromMongo(String userName, long mainId) {
        List<ChatConfigDetail> resultList = new ArrayList<>();
        String collectionName = "ChatConfigDetail";
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.addCriteria(Criteria.where("configMainId").is(mainId));
        resultList = mongoTemplate.find(query, ChatConfigDetail.class, collectionName);
        return resultList;
    }

    public List<UserChatConfigBean> getAllChatConfigByUserName(String userName) {
        List<UserChatConfigBean> userChatConfigList = new ArrayList<>();
        List<ChatConfigMain> mainList = new ArrayList<>();
        try {
            String chatConfigCollection = "ChatConfigMain";
            //先得到所有的ChatConfirMain
            String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
            Object obj = redisTemplate.opsForValue().get(configMainRedisKey);
            if (obj != null) {
                mainList = (List<ChatConfigMain>) obj;
            } else {
                Query query = new Query();
                query.addCriteria(Criteria.where("userName").is(userName));
                mainList = mongoTemplate.find(query, ChatConfigMain.class, chatConfigCollection);
                if (mainList != null && !mainList.isEmpty()) {
                    redisTemplate.opsForValue().set(configMainRedisKey, mainList);
                }
            }
            //遍历得到一个个ChatConfigDetailItem并追加到userChatConfigList
            for (ChatConfigMain chatConfigMainItem : mainList) {
                UserChatConfigBean userChatConfigBean = new UserChatConfigBean();
                userChatConfigBean.setConfigMainId(chatConfigMainItem.getId());
                userChatConfigBean.setGlobalTemperature(chatConfigMainItem.getTemperature());
                userChatConfigBean.setAllowUsers(chatConfigMainItem.getAllowUsers());
                userChatConfigBean.setKnowledgeRepoIdList(chatConfigMainItem.getKnowledgeRepoIdList());
                userChatConfigBean.setSystemMsg(chatConfigMainItem.getSystemMsg());
                userChatConfigBean.setGroovyRules(chatConfigMainItem.getGroovyRules());
                userChatConfigBean.setDescription(chatConfigMainItem.getDescription());
                userChatConfigBean.setUserName(chatConfigMainItem.getUserName());
                userChatConfigBean.setCreatedBy(chatConfigMainItem.getCreatedBy());
                List<ChatConfigDetail> detailIst = this.getChatConfigDetailByMainId(userName,
                                                                                    chatConfigMainItem.getId());
                userChatConfigBean.setDetailList(detailIst);
                userChatConfigList.add(userChatConfigBean);
            }
        } catch (Exception e) {
            logger.error(">>>>>>getAllChatConfigByUserName->{} error->{}", e.getMessage(), e);
        }
        return userChatConfigList;
    }

    public void saveUploadConfigRecord(String userName, String fileName, String fileCode) {
        String collectionName = "ChatConfigBackup";
        ChatConfigBackup backup = new ChatConfigBackup();
        backup.setUserName(userName);
        backup.setBackupDescr(fileName);
        backup.setFileCode(fileCode);
        mongoTemplate.save(backup, collectionName);
    }

    public String getBackupFileName(String userName, String fileName) {
        String collectionName = "ChatConfigBackup";
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        query.addCriteria(Criteria.where("backupDescr").is(fileName));
        ChatConfigBackup chatConfigBackup = mongoTemplate.findOne(query, ChatConfigBackup.class, collectionName);
        if (chatConfigBackup == null) {
            return "";
        } else {
            return chatConfigBackup.getBackupDescr();
        }
    }

    public String removeBackupFile(String userName, String backupId) {
        String collectionName = "ChatConfigBackup";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(backupId));
        query.addCriteria(Criteria.where("userName").is(userName));
        ChatConfigBackup backupFile = mongoTemplate.findOne(query, ChatConfigBackup.class, collectionName);
        if (backupFile != null) {
            mongoTemplate.remove(query, collectionName);
            return backupFile.getFileCode();
        }
        return "";
    }

    public void overrideChatConfig(String userName, UserChatConfigBean userChatConfigBean) {
        String collectionName = "ChatConfigMain";
        String detailCollectionName = "ChatConfigDetail";
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        String configDetailRedisKey = "fountain:settings:chatconfigdetail:list:" + userName;

        //先把main和detail里的东西删了
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(userChatConfigBean.getConfigMainId()));
        query.addCriteria(Criteria.where("userName").is(userName));
        ChatConfigMain existedMainItem = mongoTemplate.findOne(query, ChatConfigMain.class, collectionName);
        if (existedMainItem != null) {
            mongoTemplate.remove(query, collectionName);
        }
        Query detailQuery = new Query();
        detailQuery.addCriteria(Criteria.where("configMainId").is(userChatConfigBean.getConfigMainId()));
        detailQuery.addCriteria(Criteria.where("userName").is(userName));
        List<ChatConfigDetail> existedDetailList = mongoTemplate.find(detailQuery, ChatConfigDetail.class,
                                                                      detailCollectionName);
        if (existedDetailList != null && !existedDetailList.isEmpty()) {
            mongoTemplate.remove(detailQuery, detailCollectionName);
        }
        //删除redis里的内容
        redisTemplate.delete(configMainRedisKey);
        redisTemplate.delete(configDetailRedisKey);

        //再进行插入

        //插入ChatConfigMain
        ChatConfigMain newConfigMain = new ChatConfigMain();
        newConfigMain.setId(userChatConfigBean.getConfigMainId());
        newConfigMain.setTemperature(userChatConfigBean.getGlobalTemperature());
        newConfigMain.setDescription(userChatConfigBean.getDescription());
        newConfigMain.setAllowUsers(userChatConfigBean.getAllowUsers());
        newConfigMain.setSystemMsg(userChatConfigBean.getSystemMsg());
        newConfigMain.setGroovyRules(userChatConfigBean.getGroovyRules());
        newConfigMain.setKnowledgeRepoIdList(userChatConfigBean.getKnowledgeRepoIdList());
        newConfigMain.setUserName(userName);
        newConfigMain.setCreatedBy(userChatConfigBean.getCreatedBy());
        mongoTemplate.save(newConfigMain, collectionName);

        //插入detail
        for (ChatConfigDetail detailItem : userChatConfigBean.getDetailList()) {
            detailItem.setConfigMainId(userChatConfigBean.getConfigMainId());
            detailItem.setUserName(userName);
            detailItem.setCreatedBy(userName);
            mongoTemplate.save(detailItem, detailCollectionName);
        }
    }
}
