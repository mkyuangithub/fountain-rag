package com.mkyuan.fountainbase.knowledge.service;

import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.common.util.MD5Util;
import com.mkyuan.fountainbase.common.util.RandomUtil;
import com.mkyuan.fountainbase.common.util.es.ESHelper;
import com.mkyuan.fountainbase.knowledge.bean.ImportStatusBean;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeDetail;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeFile;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.vectordb.VectorDbRepo;
import com.mkyuan.fountainbase.vectordb.VectorDbRepoFactory;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.Payload;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.Point;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.PointsWrapper;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeMgtHelper {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private String IMPORT_STATUS_REDIS_KEY = "fountain:task:import:embedding:status:";//后面加userName

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private VectorDbRepoFactory vectorDbRepoFactory;

    @Autowired
    private RecordService recordService;

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ESHelper esHelper;

    //不带分页的返回所有的UserKnowledgeMain
    public List<UserKnowledgeMain> getAllKnowledgeListByUserName(String userName) {
        List<UserKnowledgeMain> knowledgeList = new ArrayList<>();
        String knowledgeCollectionName = "UserKnowledgeMain";
        Query query = new Query();
        query.addCriteria(Criteria.where("userName").is(userName));
        knowledgeList = mongoTemplate.find(query, UserKnowledgeMain.class, knowledgeCollectionName);
        return knowledgeList;
    }

    public void addUserKnowledge(String knowledgeName, String userName, String knowledgeRepoDescr) throws Exception {
        doAddUserKnowledge(knowledgeName, userName, knowledgeRepoDescr);
    }

    private void doAddUserKnowledge(String knowledgeName, String userName, String knowledgeRepoDescr) throws Exception {
        long knowledgeId = RandomUtil.getRandomLong();
        String mongoCollection = "UserKnowledgeMain";
        UserKnowledgeMain userKnowledgeMain = new UserKnowledgeMain();
        userKnowledgeMain.setKnowledgeId(knowledgeId);
        userKnowledgeMain.setUserName(userName);
        userKnowledgeMain.setKnowledgeName(knowledgeName);
        String repoDescr = knowledgeName;
        if (StringUtil.isNotBlank(knowledgeRepoDescr)) {
            repoDescr = knowledgeRepoDescr;
        }
        userKnowledgeMain.setKnowledgeRepoDescription(repoDescr);
        mongoTemplate.save(userKnowledgeMain, mongoCollection);
        this.addUserKnowledgeIntoVectorDB(knowledgeName);
        //创建es索引
        if (this.esHelper.queryEsStatus()) {
            String esIndexName = userKnowledgeMain.getKnowledgeName() + "_index";
            this.esHelper.createIndexIfNotExists(esIndexName);
        }
    }

    private void addUserKnowledgeIntoVectorDB(String knowledgeName) throws Exception {
        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(1);
        vectorDbRepo.createRepo(knowledgeName);
    }

    public void deleteUserKnowledge(String userName, String repoId) throws Exception {
        doDeleteUserKnowledge(userName, repoId);

    }

    private void doDeleteUserKnowledge(String useName, String repoId) throws Exception {
        String mongoDbCollectionName = "UserKnowledgeMain";
        //先做导入状态check，如果这个知识库在导入不让删
        String redisKey = IMPORT_STATUS_REDIS_KEY + useName;
        Object importStatusObj = this.redisTemplate.opsForValue().get(redisKey);
        if (importStatusObj != null) {
            ImportStatusBean importStatusBean = (ImportStatusBean) importStatusObj;
            if (importStatusBean.isRunning() && repoId.equals(importStatusBean.getRepoId())) {
                return;
            }
        }
        // 创建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(repoId));
        // 执行删除操作
        UserKnowledgeMain userKnowledgeMain = mongoTemplate.findOne(query, UserKnowledgeMain.class,
                                                                    mongoDbCollectionName);
        if (userKnowledgeMain != null) {
            this.doDeleteUserKnowledgeDoc(userKnowledgeMain);
            String knowledgeName = userKnowledgeMain.getKnowledgeName();
            mongoTemplate.remove(query, mongoDbCollectionName);
            VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
            vectorDbRepo.deleteRepo(knowledgeName);
            VectorDbRepo vectorDbRepoVl = this.vectorDbRepoFactory.getVectorDbRepo(3);
            vectorDbRepoVl.deleteRepoVl(knowledgeName);

            //删除es里的内容
            String esIndexName = userKnowledgeMain.getKnowledgeName() + "_index";
            this.esHelper.deleteEsIndex(esIndexName);
        }
    }

    //把相关知识库的内容删除掉
    private void doDeleteUserKnowledgeDoc(UserKnowledgeMain knowledgeRepo) {
        try {
            //删除UserKnowledgeDetail里knowledgeRepoId为正要删除的Knowledge里的id的相关内容
            String detailCollectionName = "UserKnowledgeDetail";
            Query detailQuery = new Query();
            detailQuery.addCriteria(Criteria.where("knowledgeRepoId").is(knowledgeRepo.getId()));
            mongoTemplate.remove(detailQuery, detailCollectionName);//删除UserKnowledgeDetail里的记录
            //删除UserKnowledgeFile里的文件
            String knowledgeFileCollectionName = "UserKnowledgeFile";
            Query fileQuery = new Query();
            fileQuery.addCriteria(Criteria.where("knowledgeRepoId").is(knowledgeRepo.getId()));
            List<UserKnowledgeFile> knowledgeFileList = mongoTemplate.find(fileQuery, UserKnowledgeFile.class,
                                                                           knowledgeFileCollectionName);
            //先删文件
            if (knowledgeFileList != null && !knowledgeFileList.isEmpty()) {
                for (UserKnowledgeFile knowledgeFile : knowledgeFileList) {
                    String fileCode = knowledgeFile.getFileCode();
                    //调用minio相关方法删除文件
                    this.recordService.delete(fileCode);
                }
            }
            //删除UserKnowledgeFile里的记录
            mongoTemplate.remove(fileQuery, knowledgeFileCollectionName);//删除mongodb里的记录

        } catch (Exception e) {
            logger.error(">>>>>>删除UserKnowledgeMain id->{}相关内容时出错->{}", e.getMessage(), e);
        }
    }

    public void saveRepoMajorPrompt(String repoId, String majorPromptStr) throws Exception {
        doSaveRepoMajorPrompts(repoId, majorPromptStr);
    }

    private void doSaveRepoMajorPrompts(String repoId, String majorPromptStr) {
        String mongoDbCollectionName = "UserKnowledgeMain";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(repoId));
        // 执行删除操作
        // 创建更新对象
        Update update = new Update();
        update.set("majorPrompt", majorPromptStr);

        // 执行更新操作
        mongoTemplate.updateFirst(query, update, mongoDbCollectionName);
    }

    public void setSplitType(String repoId, int splitType, String paragraphMark, int slideNums) throws Exception {
        doSetSplitType(repoId, splitType, paragraphMark, slideNums);
    }

    private void doSetSplitType(String repoId, int splitType, String paragraphMark, int slideNums) {
        String mongoDbCollectionName = "UserKnowledgeMain";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(repoId));
        // 执行删除操作
        // 创建更新对象
        Update update = new Update();
        update.set("splitType", splitType).set("paragraphMark", paragraphMark).set("slideNums", slideNums);
        // 执行更新操作
        mongoTemplate.updateFirst(query, update, mongoDbCollectionName);
    }

    public void labelRepo(String repoId, List<String> labels, int labelAction) throws Exception {
        this.doLabelRepo(repoId, labels, labelAction);

    }

    private void doLabelRepo(String repoId, List<String> labels, int labelAction) {
        String mongoDbCollectionName = "UserKnowledgeMain";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(repoId));

        Update update = new Update();
        if (labelAction == 2) {
            // 完全替换标签列表
            update.set("labelList", labels);
        } else if (labelAction == 1) {
            // 追加新标签到现有列表
            update.addToSet("labelList").each(labels.toArray());
        }

        mongoTemplate.updateFirst(query, update, mongoDbCollectionName);
    }

    public UserKnowledgeDetail doUpdateDetailItemInMongo(String userName, UserKnowledgeDetail updatedDetailItem,
                                                         List<String> newLabels) {
        String detailCollectionName = "UserKnowledgeDetail";

        //保存mongodb
        Query updateQuery = new Query();
        updateQuery.addCriteria(Criteria.where("_id").is(updatedDetailItem.getId()));
        updateQuery.addCriteria(Criteria.where("userName").is(userName));
        String contentMd5 = MD5Util.getMD5(updatedDetailItem.getOriginalContent());

        Update update = new Update();
        if (!contentMd5.equals(updatedDetailItem.getOriginalContent())) {
            update.set("originalContent", updatedDetailItem.getOriginalContent())
                  .set("contentMd5", contentMd5);
        }
        update.set("labels", newLabels)
              .set("updatedDate", new Date());
// 返回更新后的文档
        UserKnowledgeDetail result = mongoTemplate.findAndModify(
                updateQuery,
                update,
                FindAndModifyOptions.options().returnNew(true),
                UserKnowledgeDetail.class,
                detailCollectionName
        );
        return result;
    }

    public void storeDetailItemIntoVectorDb(String userName, String pointUrl, UserKnowledgeDetail updatedDetailItem,
                                            List<String> newLabels) throws Exception {
        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(1);
        PointsWrapper points = new PointsWrapper();

        long pointId = Long.valueOf(updatedDetailItem.getItemId());

        // 构建payload
        Payload payload = new Payload();
        payload.setContent(updatedDetailItem.getOriginalContent());
        payload.setFileName(updatedDetailItem.getFileName());
        payload.setFileId(updatedDetailItem.getFileId());
        //构建payload里的additionalProperties

        String keywords = String.join(", ", newLabels);
        payload.addProperty("keywords", keywords);
        List<Float> updatedVectorResult = vectorDbRepo.getEmbedding(updatedDetailItem.getOriginalContent());
        // 构建Point
        Point p = new Point();
        p.setVector(updatedVectorResult);
        p.setPayload(payload);
        p.setId(pointId);
        // 最后我们把这个Point变成List结构并完成最终发送报文前的全部工作
        points.getPoints().add(p);
        //存储pointId到对应的文件中去
        vectorDbRepo.addBatchEmbeddingItemIntoVectorDB(points, pointUrl);
    }

    public void removeKnowledgeDetailItemFromMongo(String userName, String knowledgeRepoId, String detailId)
            throws Exception {
        long fileId = 0l;

        UserKnowledgeDetail currentItem = this.getKnowledgeDetailById(userName, detailId);
        if (currentItem == null) {
            return;
        }
        fileId = currentItem.getFileId();

        String collectionName = "UserKnowledgeDetail";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(detailId));
        query.addCriteria(Criteria.where("userName").is(userName));
        mongoTemplate.remove(query, collectionName);

        String knowledgeMainCollectionName = "UserKnowledgeMain";
        //操作UserKnowledgeMain
        Query knowledgeMainQuery = new Query();
        knowledgeMainQuery.addCriteria(Criteria.where("_id").is(knowledgeRepoId));
        Update knowledgeMainUpdate = new Update()
                .inc("successCount", -1);
        mongoTemplate.updateFirst(knowledgeMainQuery, knowledgeMainUpdate, knowledgeMainCollectionName);//successCount-1

        //删除完了要看相应的file的条数是不是也为0了，如果为0删除这个file
        List<UserKnowledgeDetail> fileList = this.getKnowledgeDetailByFileId(userName, fileId);
        if (fileList == null || fileList.isEmpty()) {
            //执行删除UserKnowledgeFile包括存储对象
            UserKnowledgeFile userKnowledgeFile = this.getKnowledgeFileById(userName, fileId);
            if (userKnowledgeFile != null) {
                String deleteFileCollection = "UserKnowledgeFile";
                this.recordService.delete(userKnowledgeFile.getFileCode());//删除文件
                Query deleteFileQuery = new Query();
                deleteFileQuery.addCriteria(Criteria.where("fileId").is(fileId));//删除db记录
                deleteFileQuery.addCriteria(Criteria.where("userName").is(userName));
                mongoTemplate.remove(deleteFileQuery, deleteFileCollection);

                //文件数-1
                knowledgeMainQuery = new Query();
                knowledgeMainQuery.addCriteria(Criteria.where("_id").is(knowledgeRepoId));
                knowledgeMainUpdate = new Update()
                        .inc("fileCount", -1);
                mongoTemplate.updateFirst(knowledgeMainQuery, knowledgeMainUpdate,
                                          knowledgeMainCollectionName);//successCount-1
            }
        }
    }

    public UserKnowledgeDetail getKnowledgeDetailById(String userName, String detailItemId) {
        String collectionName = "UserKnowledgeDetail";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(detailItemId));
        query.addCriteria(Criteria.where("userName").is(userName));
        UserKnowledgeDetail knowledgeDetail = mongoTemplate.findOne(query, UserKnowledgeDetail.class, collectionName);
        return knowledgeDetail;
    }

    public List<UserKnowledgeDetail> getKnowledgeDetailByFileId(String userName, long fileId) {
        String collectionName = "UserKnowledgeDetail";
        Query query = new Query();
        query.addCriteria(Criteria.where("fileId").is(fileId));
        query.addCriteria(Criteria.where("userName").is(userName));
        List<UserKnowledgeDetail> userKnowledgeDetails = new ArrayList<>();
        userKnowledgeDetails = mongoTemplate.find(query, UserKnowledgeDetail.class, collectionName);
        return userKnowledgeDetails;
    }

    public UserKnowledgeFile getKnowledgeFileById(String userName, long fileId) {
        String collectionName = "UserKnowledgeFile";
        Query query = new Query();
        query.addCriteria(Criteria.where("fileId").is(fileId));
        query.addCriteria(Criteria.where("userName").is(userName));
        return mongoTemplate.findOne(query, UserKnowledgeFile.class, collectionName);
    }

    public String getKnowledgeFileCodeByDetailId(String userName, String detailItemId) {
        String knowledgeFileCollection = "UserKnowledgeFile";
        Query query = new Query();
        UserKnowledgeDetail detailItem = this.getKnowledgeDetailById(userName, detailItemId);
        if (detailItem != null) {
            UserKnowledgeFile knowledgeFile = this.getKnowledgeFileById(userName, detailItem.getFileId());
            if (knowledgeFile != null) {
                return knowledgeFile.getFileCode();
            }
        }
        return "";
    }

}
