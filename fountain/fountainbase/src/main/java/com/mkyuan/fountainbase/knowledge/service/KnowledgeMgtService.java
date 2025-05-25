package com.mkyuan.fountainbase.knowledge.service;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.AIComponent;
import com.mkyuan.fountainbase.ai.AIFactory;
import com.mkyuan.fountainbase.ai.AIFunctionHelper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.service.SystemModelSupport;
import com.mkyuan.fountainbase.common.util.AIResponseUtil;
import com.mkyuan.fountainbase.common.util.MD5Util;
import com.mkyuan.fountainbase.common.util.RandomUtil;
import com.mkyuan.fountainbase.common.util.es.ESHelper;
import com.mkyuan.fountainbase.common.util.timelogger.TimeLogger;
import com.mkyuan.fountainbase.knowledge.bean.ImportStatusBean;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeDetail;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.knowledge.bean.VectorIndexRequest;
import com.mkyuan.fountainbase.user.service.UserLocaleService;
import com.mkyuan.fountainbase.vectordb.VectorDbRepo;
import com.mkyuan.fountainbase.vectordb.VectorDbRepoFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeMgtService {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    public final static int PAGESIZE = 20;
    @Autowired
    private KnowledgeMgtHelper knowledgeMgtHelper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AIFunctionHelper aiFunctionHelper;

    @Autowired
    private AIFactory aiFactory;

    @Autowired
    private KnowledgeVectorHelper knowledgeVectorHelper;

    @Autowired
    private SystemModelSupport systemModelSupport;

    @Autowired
    private VectorDbRepoFactory vectorDbRepoFactory;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ESHelper esHelper;

    @Autowired
    private UserLocaleService userLocaleService;

    @TimeLogger
    public void labelRepo(String repoId, String userName, String inputPrompt, int labelAction) {
        String result = "";
        String sendPrompt = this.aiFunctionHelper.getFunctions(10001, inputPrompt);
        double temperature = 0.1;
        List<String> labelPairs = new ArrayList<>();
        try {
            logger.info(">>>>>>先用masterModel去试");
            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
            int masterModelType = masterModel.getType();
            logger.info(">>>>>>send prompt is->{}", sendPrompt);
            AIComponent aiComponent = this.aiFactory.getAIComponent(masterModelType);
            String jsonResponse = aiComponent.jsonCall(masterModel, sendPrompt, temperature);
            logger.info(">>>>>>jsonResponse->" + jsonResponse);
            labelPairs = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode targetNode = rootNode.has("keywords") ? rootNode.get("keywords") : rootNode;

            // 如果目标节点是数组，直接处理
            if (targetNode.isArray()) {
                for (JsonNode labelNode : targetNode) {
                    // 如果节点是文本值，直接添加到列表中
                    if (labelNode.isTextual()) {
                        labelPairs.add(labelNode.asText());
                    }
                }
            }
            this.knowledgeMgtHelper.labelRepo(repoId, labelPairs, labelAction);
        } catch (Exception e) {
            logger.error(">>>>>>用masterModel发送失败，换用slaveModel再重试->{}", e.getMessage(), e);
            try {
                AIModel slaveModel = this.systemModelSupport.getSlave(userName);
                if (slaveModel == null) {
                    //系统没有配slaveModel，那么直接就抛错了
                    logger.error(">>>>>>labelRepo service error->{}", e.getMessage(), e);
                    return;
                }
                int slaveModelType = slaveModel.getType();
                logger.info(">>>>>>send prompt is->{}", sendPrompt);
                AIComponent aiComponent = this.aiFactory.getAIComponent(slaveModelType);
                String jsonResponse = aiComponent.jsonCall(slaveModel, sendPrompt, temperature);
                logger.info(">>>>>>jsonResponse->" + jsonResponse);
                labelPairs = new ArrayList<>();
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode targetNode = rootNode.has("keywords") ? rootNode.get("keywords") : rootNode;

                // 如果目标节点是数组，直接处理
                if (targetNode.isArray()) {
                    for (JsonNode labelNode : targetNode) {
                        // 如果节点是文本值，直接添加到列表中
                        if (labelNode.isTextual()) {
                            labelPairs.add(labelNode.asText());
                        }
                    }
                }
                this.knowledgeMgtHelper.labelRepo(repoId, labelPairs, labelAction);
            } catch (Exception se) {
                logger.error(">>>>>>labelRepo service error->{}", se.getMessage(), se);
            }
        }
    }

    public void addUserKnowledge(String userName, String knowledgeName, String knowledgeRepoDescr) throws Exception {
        String knowledgeVectorCollectionName = userName + "_" + knowledgeName;
        this.knowledgeMgtHelper.addUserKnowledge(knowledgeVectorCollectionName, userName, knowledgeRepoDescr);
    }

    public void deleteUserKnowledge(String userName, String knowledgeRepoId) throws Exception {
        //String knowledgeVectorCollectionName=userName+"_"+knowledgeName;
        this.knowledgeMgtHelper.deleteUserKnowledge(userName, knowledgeRepoId);
    }

    public void saveMajorPrompt(String repoId, String majorPrompt) throws Exception {
        this.knowledgeMgtHelper.saveRepoMajorPrompt(repoId, majorPrompt);
    }

    public void setSplitType(String repoId, int splitType, String paragraphMark, int slideNums) throws Exception {
        this.knowledgeMgtHelper.setSplitType(repoId,splitType,paragraphMark,slideNums);
    }
    /**
     * 获取一个用户下所有的知识库
     *
     * @param userName
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public Page<UserKnowledgeMain> getPagedUserKnowledgeRepoList(String userName, int pageNumber, int pageSize) {
        String collectionName = "UserKnowledgeMain";
        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<UserKnowledgeMain> knowledgeRepoList = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("userName").is(userName));
            //query.addCriteria(Criteria.where("type").is(type));

            query.with(Sort.by(Sort.Direction.DESC, "updatedDate"));
            long count = mongoTemplate.count(query, UserKnowledgeMain.class, collectionName);
            List<UserKnowledgeMain> repoList = mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)),
                                                                  UserKnowledgeMain.class, collectionName);
            // 为每个知识库添加文件数量和详情数量
            for (UserKnowledgeMain repo : repoList) {
                // 统计文件数量
                Query fileQuery = new Query();
                fileQuery.addCriteria(Criteria.where("knowledgeRepoId").is(repo.getId()));
                long fileCount = mongoTemplate.count(fileQuery, "UserKnowledgeFile");
                repo.setFileCount((int) fileCount);

                // 统计详情数量
                Query detailQuery = new Query();
                detailQuery.addCriteria(Criteria.where("knowledgeRepoId").is(repo.getId()));
                long itemCount = mongoTemplate.count(detailQuery, "UserKnowledgeDetail");
                repo.setItemCount((int) itemCount);
            }

            return new PageImpl<>(repoList, PageRequest.of(pageNumber - 1, pageSize), count);
        } catch (Exception e) {
            logger.error(">>>>>>分页查询用户下所有的知识库错误->{}", e.getMessage(), e);
        }
        return knowledgeRepoList;
    }

    //索引
    public void makeIndex(String repoId, VectorIndexRequest vectorIndexRequest) {
        String indexUrl = this.knowledgeVectorHelper.getIndexUrl(repoId);
        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
        vectorDbRepo.makeIndex(vectorIndexRequest, indexUrl);
    }

    //获取具体的一条知识库条目信息
    public UserKnowledgeDetail getDetailKnowledgeItem(String userName, String detailId) {
        UserKnowledgeDetail userKnowledgeDetail = new UserKnowledgeDetail();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(detailId));
        query.addCriteria(Criteria.where("userName").is(userName));
        String collectionName = "UserKnowledgeDetail";
        userKnowledgeDetail = mongoTemplate.findOne(query, UserKnowledgeDetail.class, collectionName);
        return userKnowledgeDetail;
    }

    /**
     * 更新具体的一条知识库条目
     *
     * @param userName
     * @param updatedDetailItem
     * @return 101 - 己有任务在运行
     * -1 - 更新失败
     * 1- 更新成功
     */
    public int updateDetailKnowledgeItem(String userName, UserKnowledgeDetail updatedDetailItem) {
        ImportStatusBean importStatusBean = new ImportStatusBean();
        try {
            importStatusBean = this.knowledgeVectorHelper.getImportStatus(userName);
            if (importStatusBean != null) {
                if (importStatusBean.isRunning()) {
                    return 101;
                }
            }
            UserKnowledgeMain userKnowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById(
                    updatedDetailItem.getKnowledgeRepoId());
            List<String> aiLabels = new ArrayList<>();
            if (userKnowledgeMain != null) {
                if(userKnowledgeMain.getSplitType()==0) {
                    //更新前先要用ai打标签
                    aiLabels = this.knowledgeVectorHelper.labelTextContent(userName,
                                                                           updatedDetailItem.getOriginalContent(),
                                                                           userKnowledgeMain.getLabelList());
                }
            } else {
                logger.error(">>>>>>明细条目对应不上UserKnowledgeMain里的knowledgeRepoId");
                return -1;
            }
            //把用户输入的标签全部取出
            List<String> inputLabels = new ArrayList<>();
            inputLabels.clear();
            inputLabels.addAll(updatedDetailItem.getLabels());

            //把ai打出来的标签和用户输入的标签合并
            List<String> updatedLabels = new ArrayList<>();
            updatedLabels.addAll(aiLabels);

            for (String newLabel : inputLabels) {
                updatedLabels.add(newLabel);
            }
            // 使用 Set 去重
            Set<String> updatedSet = new HashSet<>(updatedLabels);
            updatedLabels.clear();
            updatedLabels.addAll(updatedSet);

            //更新mongodb
            UserKnowledgeDetail updatedReturnItem = this.knowledgeMgtHelper.doUpdateDetailItemInMongo(userName,
                                                                                                      updatedDetailItem,
                                                                                                      updatedLabels);

            //更新qdrant
            String pointUrl = this.knowledgeVectorHelper.getPointsUrl(updatedReturnItem.getKnowledgeRepoId());

            this.knowledgeMgtHelper.storeDetailItemIntoVectorDb(userName, pointUrl, updatedReturnItem, updatedLabels);
            //更新es开始

            // 构建索引名称
            String indexName = userKnowledgeMain.getKnowledgeName() + "_index";

            // 构建文档ID

            UserKnowledgeDetail detailItem=this.knowledgeVectorHelper.getUserKnowledgeDetailById(updatedDetailItem.getId());
            if(detailItem==null){
                logger.error(">>>>>>要更新的KnowledgeDetail id->{}已经不存在",updatedDetailItem.getId());
            }

            String docId = detailItem.getItemId();

            // 构建文档数据
            Map<String, Object> docData = new HashMap<>();
            docData.put("itemId", detailItem.getItemId());
            docData.put("originalContent", detailItem.getOriginalContent());
            docData.put("mongodbId", detailItem.getId());
            docData.put("knowledgeRepoId", detailItem.getKnowledgeRepoId());
            docData.put("fileId", detailItem.getFileId());
            docData.put("userName", detailItem.getUserName());
            docData.put("fileName", detailItem.getFileName());
            docData.put("labels", detailItem.getLabels());
            // ... 其他字段

            // 调用通用方法插入数据
            this.esHelper.insertOrUpdateDocument(indexName, docId, docData);
            //更新es结束
            return 1;
        } catch (Exception e) {
            logger.error(">>>>>>在编辑一条具体的知识库条目时失败->{}", e.getMessage(), e);
            return -1;
        }
    }

    public int deleteDetailKnowledgeItem(String userName, String knowledgeRepoId, List<String>detailIds) {
        List<Long> pointIds=new ArrayList<>();
        ImportStatusBean importStatusBean = new ImportStatusBean();
        try {
            importStatusBean = this.knowledgeVectorHelper.getImportStatus(userName);
            if (importStatusBean != null) {
                if (importStatusBean.isRunning()) {
                    return 101;
                }
            }
            UserKnowledgeMain userKnowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById(knowledgeRepoId);
            if (userKnowledgeMain != null) {
                String esIndexName = userKnowledgeMain.getKnowledgeName() + "_index";
                for(String detailId: detailIds) {
                    UserKnowledgeDetail userKnowledgeDetail=this.knowledgeVectorHelper.getUserKnowledgeDetailById(detailId);
                    if (userKnowledgeDetail != null) {
                        pointIds.add(Long.parseLong(userKnowledgeDetail.getItemId()));
                    }
                    this.esHelper.deleteOneEsItem(esIndexName,Long.parseLong(userKnowledgeDetail.getItemId()));
                    this.knowledgeMgtHelper.removeKnowledgeDetailItemFromMongo(userName,knowledgeRepoId,detailId);
                }
                VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
                vectorDbRepo.deletePoints(userKnowledgeMain.getKnowledgeName(), pointIds);

                VectorDbRepo vectorDbRepoVl = this.vectorDbRepoFactory.getVectorDbRepo(3);
                vectorDbRepoVl.deleteVlPoints(userKnowledgeMain.getKnowledgeName(),pointIds);
            }
            return 1;
        } catch (Exception e) {
            logger.error(">>>>>>delete detail knowledge item service error->{}", e.getMessage(), e);
            return -1;
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
