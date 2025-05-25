package com.mkyuan.fountainbase.agent.chatbot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigBackup;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigDetail;
import com.mkyuan.fountainbase.agent.chatbot.bean.ChatConfigMain;
import com.mkyuan.fountainbase.agent.chatbot.bean.UserChatConfigBean;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.common.minio.beans.SmartArchiveRecord;
import com.mkyuan.fountainbase.common.util.RandomUtil;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeMgtHelper;
import com.mkyuan.fountainbase.user.bean.UserInfo;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.util.function.Tuple2;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatConfigService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ChatConfigHelper chatConfigHelper;

    @Autowired
    private KnowledgeMgtHelper knowledgeMgtHelper;

    public List<UserKnowledgeMain> getAllKnowledgeListByUserName(String userName) {
        return this.knowledgeMgtHelper.getAllKnowledgeListByUserName(userName);
    }

    @Autowired
    private RecordService recordService;

    @Transactional(rollbackFor = Exception.class)
    public void deleteChatConfig(String userName, long configMainId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            this.chatConfigHelper.deleteChatConfig(userName, configMainId);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    this.chatConfigHelper.deleteChatConfig(userName, configMainId);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>deleteChatConfig error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateChatConfig(String userName, ChatConfigMain chatConfigMain) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            this.chatConfigHelper.updateChatConfig(userName, chatConfigMain);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    this.chatConfigHelper.updateChatConfig(userName, chatConfigMain);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>updateChatConfig error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addChatConfig(String userName, ChatConfigMain chatConfigMain)
            throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            this.chatConfigHelper.doAddChatConfig(userName, chatConfigMain);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    this.chatConfigHelper.doAddChatConfig(userName, chatConfigMain);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>addChatConfig error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public List<ChatConfigMain> getChatConfigListByUserName(String userName) {
        List<ChatConfigMain> configList = new ArrayList<>();
        String chatConfigCollection = "ChatConfigMain";
        String configMainRedisKey = "fountain:settings:chatconfig:list:" + userName;
        try {
            Object obj = redisTemplate.opsForValue().get(configMainRedisKey);
            if (obj != null) {
                configList = (List<ChatConfigMain>) obj;
                return configList;
            }
            Query query = new Query();
            query.addCriteria(Criteria.where("userName").is(userName));
            configList = mongoTemplate.find(query, ChatConfigMain.class, chatConfigCollection);
            if (configList != null && !configList.isEmpty()) {
                redisTemplate.opsForValue().set(configMainRedisKey, configList);
            }
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigListByUserName service error->{}", e.getMessage(), e);
        }
        return configList;
    }

    public JSONObject initChatConfig() {
        JSONObject result = new JSONObject();
        try {
            long mainConfigId = RandomUtil.getRandomLong();
            List<String> knowledgeRepoIdList = new ArrayList<>();
            List<String> allowUsers = new ArrayList<>();

            result.put("mainConfigId", mainConfigId);
            result.put("description", "");
            result.put("knowledgeRepoIdList", knowledgeRepoIdList);
            result.put("allowUsers", allowUsers);
            result.put("systemMsg", "");
            result.put("temperature", 0.1);

            return result;
        } catch (Exception e) {
            logger.error(">>>>>>initChatConfig error->{}", e.getMessage(), e);
            return null;
        }

    }

    public JSONObject getChatConfigByMainId(String userName, long mainId) {
        JSONObject result = new JSONObject();
        ChatConfigMain chatConfigMain = new ChatConfigMain();
        try {
            chatConfigMain = this.chatConfigHelper.getChatConfigMainById(userName, mainId);
            if (chatConfigMain == null) {
                return null;
            } else {
                List<String> knowledgeRepoIdList = new ArrayList<>();
                List<String> allowUsers = new ArrayList<>();
                knowledgeRepoIdList = chatConfigMain.getKnowledgeRepoIdList();
                allowUsers = chatConfigMain.getAllowUsers();
                //组装ChatConfigMain
                result.put("mainConfigId", chatConfigMain.getId());
                result.put("description", chatConfigMain.getDescription());
                result.put("knowledgeRepoIdList", knowledgeRepoIdList);
                result.put("allowUsers", allowUsers);
                result.put("systemMsg", chatConfigMain.getSystemMsg());
                result.put("temperature", chatConfigMain.getTemperature());
                result.put("groovyRules",chatConfigMain.getGroovyRules());
                result.put("rewriteSelectedDifySequenceNo", chatConfigMain.getRewriteSelectedDifySequenceNo());
                result.put("chatSelectedDifySequenceNo", chatConfigMain.getChatSelectedDifySequenceNo());
            }
        } catch (Exception e) {
            logger.error(">>>>>>getChatConfigByMainId error->{}", e.getMessage(), e);
        }
        return result;
    }

    public int backupChatConfigByUserName(String userName, String backupDescr) {
        List<UserChatConfigBean> userChatConfigList = new ArrayList<>();
        String backupFileName = new String(backupDescr);
        String fileType = "application/json";
        String fileCode = "";
        try {
            if (StringUtil.isBlank(backupFileName)) {
                backupFileName = sdf.format(new Date()) + ".json";
            } else {
                backupFileName = backupFileName + ".json";
            }
            String existedFileName = this.chatConfigHelper.getBackupFileName(userName, backupDescr);
            if (existedFileName.equals(backupFileName)) {
                return 101;//文件己存在
            }
            userChatConfigList = this.chatConfigHelper.getAllChatConfigByUserName(userName);
            if (userChatConfigList != null && !userChatConfigList.isEmpty()) {
                //开始备份
                String backupJsonText = backupJsonText = objectMapper.writeValueAsString(userChatConfigList);
                fileCode = this.recordService.save(userName, null, backupFileName, fileType, backupJsonText.getBytes(
                        StandardCharsets.UTF_8), true);
                if (StringUtils.isBlank(fileCode)) {
                    return -1;//上传出错
                } else {
                    this.chatConfigHelper.saveUploadConfigRecord(userName, backupFileName, fileCode);
                    return 1;//上传完成
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>backupChatConfigByUserName ->{} error->{}", userName, e.getMessage(), e);
            return -1;
        }
        return -1;
    }

    public Page<ChatConfigBackup> getBackupList(String userName, String backupDescr, String queryDate, int pageNumber,
                                                int pageSize) {
        Pageable pageable = PageRequest.of(0, 10); // 创建一个分页请求
        Page<ChatConfigBackup> backupList = new PageImpl<>(Collections.emptyList(), pageable, 10);
        String collectionName = "ChatConfigBackup";
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("userName").is(userName));//添加根据用户名查询
            if (StringUtil.isNotBlank(backupDescr)) {
                query.addCriteria(Criteria.where("backupDescr").regex(".*" + backupDescr + ".*", "i"));//添加根据文件名全模糊查询
            }
            if (StringUtils.isNotBlank(queryDate)) {
                // 将 queryDate 转换为 Date 类型
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                Date date = dateFormat.parse(queryDate);
                // 添加 createdDate 查询条件
                query.addCriteria(Criteria.where("createdDate").gte(date).lt(getNextDay(date)));//添加queryDate查询
            }
            query.with(Sort.by(Sort.Direction.DESC, "updatedDate"));
            long count = mongoTemplate.count(query, UserInfo.class, collectionName);
            List<ChatConfigBackup> searchResult = mongoTemplate.find(
                    query.with(PageRequest.of(pageNumber - 1, pageSize)), ChatConfigBackup.class, collectionName);
            return new PageImpl<>(searchResult, PageRequest.of(pageNumber - 1, pageSize), count);
        } catch (Exception e) {
            logger.error(">>>>>>getBackupList error->{}", e.getMessage(), e);
        }
        return backupList;
    }

    public void removeBackupFile(String userName, String backupId) throws Exception {
        String fileCode = this.chatConfigHelper.removeBackupFile(userName, backupId);
        if (StringUtil.isNotBlank(fileCode)) {
            logger.info(">>>>>>start to delete the file with->{}", fileCode);
            this.recordService.delete(fileCode);
        }
    }

    public void restoreByBackupFile(String userName, String backupId) {

        String configMainCollectionName = "ChatConfigMain";
        String configDetailCollectionName = "ChatConfigDetail";
        try {
            //先查出backupfile的object
            ChatConfigBackup chatConfigBackup = this.getChatBackup(userName, backupId);
            if (chatConfigBackup != null) {
                String fileCode = chatConfigBackup.getFileCode();
                Tuple2<SmartArchiveRecord, byte[]> tuple = this.recordService.find(fileCode);
                SmartArchiveRecord record = tuple.getT1();
                byte[] data = tuple.getT2();

                // 将byte[]转换为字符串
                String jsonText = new String(data, StandardCharsets.UTF_8);

                // 使用ObjectMapper将JSON字符串转换为List<UserChatConfigBean>
                ObjectMapper objectMapper = new ObjectMapper();
                List<UserChatConfigBean> userChatConfigList = objectMapper.readValue(
                        jsonText,
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class,
                                UserChatConfigBean.class
                        )
                );

                if (userChatConfigList != null && !userChatConfigList.isEmpty()) {
                    for(UserChatConfigBean userChatConfigBean:userChatConfigList){
                        this.chatConfigHelper.overrideChatConfig(userName,userChatConfigBean);
                    }
                }
            }

        } catch (Exception e) {
            logger.error(">>>>>>restoreByBackupFile service error->{}", e.getMessage(), e);
        }
    }

    public ChatConfigBackup getChatBackup(String userName, String backupId) {
        String collection = "ChatConfigBackup";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(backupId));
        query.addCriteria(Criteria.where("userName").is(userName));
        ChatConfigBackup backupFile = mongoTemplate.findOne(query, ChatConfigBackup.class, collection);
        return backupFile;
    }

    private Date getNextDay(Date date) {
        // 获取下一天的日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private String getDetailStepDescriptionByType(int type) {
        String stepDescription = "";
        switch (type) {
            case 1:
                stepDescription = "判断是否闲聊";
                break;
            case 2:
                stepDescription = "对原提示语进行降噪、纠正、折分";
                break;
            case 3:
                stepDescription = "对原提示语使用BG25、HYDE、GRM进行重写";
                break;
            case 4:
                stepDescription = "对原提示语进行打标签、折关键字";
                break;
            case 5:
                stepDescription = "重排序";
                break;
            default:
                stepDescription = "";
                break;
        }
        return stepDescription;
    }
}
