package com.mkyuan.fountainbase.knowledge.service;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.knowledge.bean.*;
import com.mkyuan.fountainbase.office.FileBean;
import com.mkyuan.fountainbase.office.FileResolver;
import com.mkyuan.fountainbase.office.FileResolverFactory;
import com.mkyuan.fountainbase.user.service.UserLocaleService;
import com.mkyuan.fountainbase.vectordb.VectorDbRepo;
import com.mkyuan.fountainbase.vectordb.VectorDbRepoFactory;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KnowledgeVectorService {
    public final static int PAGESIZE = 20;
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private KnowledgeVectorHelper knowledgeVectorHelper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private VectorDbRepoFactory vectorDbRepoFactory;

    @Autowired
    private UserLocaleService userLocaleService;

    public ImportStatusBean getImportStatus(String userName) {
        return this.knowledgeVectorHelper.getImportStatus(userName);
    }

    public FileContentParseBean uploadDocIntoKnowledge(String token, String repoId, String userName, MultipartFile file,
                                                       boolean needSplit, boolean readImg)
            throws Exception {
        try {
            long fileId = 0l;
            String textContent = "";
            byte[] fileContent = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            //fileId = this.knowledgeVectorHelper.storeFile(token, repoId, userName, originalFilename,
            //                                              file.getContentType(), fileContent,readImg);
            FileContentParseBean fileContentParseBean =
                    this.knowledgeVectorHelper.storeFile(token, repoId, userName, originalFilename,
                                                         file.getContentType(), fileContent, readImg);
            return fileContentParseBean;
            /*
            if (fileId > 0l) {
                //执行切片动作
                UserKnowledgeFile userKnowledgeFile = this.knowledgeVectorHelper.getUserKnowledgeFileByFileId(fileId);
                if (userKnowledgeFile != null) {
                    this.knowledgeVectorHelper.processTextContent(token, repoId, userName, fileId,
                                                                  userKnowledgeFile.getFileContent(), originalFilename,
                                                                  needSplit);
                }
            }
            logger.info(">>>>>>上传并存档成功，文件内容解析为->{}" + textContent);
             */

        } catch (Exception e) {
            throw new Exception(">>>>>>upload a doc service error: " + e.getMessage(), e);
        }
    }

    public void executeEmbedding(String token, String repoId, String userName, long fileId,
                                 boolean needSplit, boolean readImg, boolean vlEmbedding) {
        this.updateVlEmbeddingStatus(repoId, userName, vlEmbedding);
        ImportStatusBean importStatusBean = new ImportStatusBean();
        importStatusBean.setStopFlag(false);
        importStatusBean.setRunning(true);
        importStatusBean.setRepoId(repoId);
        this.knowledgeVectorHelper.setImportStatus(userName, importStatusBean);
        if (fileId > 0l) {
            //执行切片动作
            UserKnowledgeFile userKnowledgeFile = this.knowledgeVectorHelper.getUserKnowledgeFileByFileId(fileId);
            if (userKnowledgeFile != null) {
                this.knowledgeVectorHelper.processTextContent(token, repoId, userName, fileId,
                                                              userKnowledgeFile.getFileContent(),
                                                              userKnowledgeFile.getFileName(),
                                                              needSplit, vlEmbedding);
            }
        }
    }

    private void updateVlEmbeddingStatus(String repoId, String userName, boolean vlEmbedding) {
        String collection = "UserKnowledgeMain";
        Query knowledgeQuery = new Query();
        knowledgeQuery.addCriteria(Criteria.where("_id").is(repoId));
        knowledgeQuery.addCriteria(Criteria.where("userName").is(userName));
        Update knowledgeUpdate = new Update()
                .set("vlEmbedding", vlEmbedding);
        mongoTemplate.updateFirst(knowledgeQuery, knowledgeUpdate, collection);//update vlEmbedding;
    }

    public void stopUploadProcess(String userName) {
        try {
            ImportStatusBean importStatusBean = new ImportStatusBean();
            importStatusBean.setStopFlag(true);
            importStatusBean.setRunning(false);
            this.knowledgeVectorHelper.setImportStatus(userName, importStatusBean);
        } catch (Exception e) {
            logger.error(">>>>>>stop import error->{}", e.getMessage(), e);
        }
    }

    public Page<UserKnowledgeDetail> getPagedKnowledgeDetails(String userName, String knowledgeRepoId, int pageNumber,
                                                              int pageSize, String searchedContent,
                                                              boolean isEmbedding) {

        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<UserKnowledgeDetail> detailList = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try {
            if (isEmbedding) {
                if (StringUtils.isBlank(searchedContent)) {
                    return detailList;
                }
                List<UserKnowledgeDetail> vectorResult = new ArrayList<>();
                //先搜向量
                VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
                List<Float> searchedV = vectorDbRepo.getEmbedding(searchedContent);
                UserKnowledgeMain userKnowledgeMain = this.knowledgeVectorHelper.getUserKnowledgeMainById(
                        knowledgeRepoId);
                if (userKnowledgeMain != null) {
                    String searchUri = userKnowledgeMain.getKnowledgeName();
                    List<QDRantQueryResult> vectorList = vectorDbRepo.queryWithVector(searchUri, searchedV, null, 100);
                    Collections.sort(vectorList, (a, b) -> Float.compare(b.getScore(), a.getScore()));
                    List<Long> searchedIds = new ArrayList<>();
                    for (QDRantQueryResult qdRantQueryResult : vectorList) {
                        if (qdRantQueryResult.getScore() > 0.4) {
                            searchedIds.add(qdRantQueryResult.getId());
                        }
                    }
                    String mongoCollection = "UserKnowledgeDetail";
                    Query query = new Query();
                    query.addCriteria(Criteria.where("itemId").in(searchedIds));
                    vectorResult = mongoTemplate.find(query, UserKnowledgeDetail.class, mongoCollection);
                }
                if (vectorResult != null && !vectorResult.isEmpty()) {
                    return new PageImpl<>(vectorResult, PageRequest.of(1, vectorResult.size()), vectorResult.size());
                } else {
                    //如何向量搜索出来的内容为空那么返回空
                    return detailList;
                }
            }
            Query query = new Query();
            query.addCriteria(Criteria.where("knowledgeRepoId").is(knowledgeRepoId));
            //query.addCriteria(Criteria.where("type").is(type));
            // 添加 searchedContent 的模糊查询
            if (StringUtils.isNotBlank(searchedContent)) {
                query.addCriteria(Criteria.where("originalContent").regex(".*" + searchedContent + ".*", "i"));
            }

            // 检查 queryDate 是否不为空

            Sort sort = Sort.by(Sort.Direction.ASC, "fileId")
                            .and(Sort.by(Sort.Direction.ASC, "seqNo"))
                            .and(Sort.by(Sort.Direction.DESC, "updatedDate"));
            query.with(sort);
            long count = mongoTemplate.count(query, UserKnowledgeDetail.class, "UserKnowledgeDetail");
            List<UserKnowledgeDetail> returnList = mongoTemplate.find(
                    query.with(PageRequest.of(pageNumber - 1, pageSize)), UserKnowledgeDetail.class,
                    "UserKnowledgeDetail");
            return new PageImpl<>(returnList, PageRequest.of(pageNumber - 1, pageSize), count);
        } catch (Exception e) {

        }
        return detailList;
    }

    //手工添加一条具体的知识库里的条目
    public void addKnowledgeDetail(String userName, String knowledgeRepoId, String inputContent,
                                   List<String> inputLabels) {
        try {
            this.knowledgeVectorHelper.addKnowledgeDetail(userName, knowledgeRepoId, inputContent, inputLabels);
        } catch (Exception e) {
            logger.error(">>>>>>addKnowledgeDetail error->{}", e.getMessage(), e);
        }
    }

    public UserKnowledgeMain getUserKnowledgeMainById(String repoId) {
        return this.knowledgeVectorHelper.getUserKnowledgeMainById(repoId);
    }

    public void syncToES(String userName, String knowledgeRepoId) {
        this.knowledgeVectorHelper.syncToES(userName, knowledgeRepoId);
    }


}
