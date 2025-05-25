package com.mkyuan.fountainbase.knowledge.service;

import com.mkyuan.fountainbase.agent.chatbot.bean.KnowledgeResult;
import com.mkyuan.fountainbase.ai.bean.Message;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;
import com.mkyuan.fountainbase.common.util.*;
import com.mkyuan.fountainbase.common.util.es.ESHelper;
import com.mkyuan.fountainbase.user.service.UserLocaleService;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkyuan.fountainbase.ai.AIComponent;
import com.mkyuan.fountainbase.ai.AIFactory;
import com.mkyuan.fountainbase.ai.AIFunctionHelper;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.service.SystemModelSupport;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.knowledge.bean.*;
import com.mkyuan.fountainbase.office.FileBean;
import com.mkyuan.fountainbase.office.FileResolver;
import com.mkyuan.fountainbase.office.FileResolverFactory;
import com.mkyuan.fountainbase.vectordb.VectorDbRepo;
import com.mkyuan.fountainbase.vectordb.VectorDbRepoFactory;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.Payload;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.Point;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.PointsWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.springframework.web.client.RestTemplate;

@Component
public class KnowledgeVectorHelper {

    protected Logger logger = LogManager.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final int THREAD_POOL_SIZE = 3;
    private static final int CHUNK_SIZE = 400;
    private static final int slideWords = 400;
    //private static final char[] SENTENCE_ENDINGS = {'.', '。', '!', '！', '?', '？'}; // 定义句子结束符
    private static final char[] SENTENCE_ENDINGS = {'.', '。', '!', '！'}; // 定义句子结束符
    private final ExecutorService executorService;
    private String IMPORT_STATUS_REDIS_KEY = "fountain:task:import:embedding:status:";//后面加userName

    // 首先添加一个内部类来包装文本和向量结果
    public KnowledgeVectorHelper() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    private static class EmbeddingResult {
        private final String text;
        private final List<Float> vector;
        private final List<String> labelPairs;
        private final long itemId;

        public EmbeddingResult(long itemId, String text, List<Float> vector, List<String> labelPairs) {
            this.text = text;
            this.vector = vector;
            this.labelPairs = labelPairs;
            this.itemId = itemId;
        }
    }

    @Value("${fountain.vectordb.uri}")
    private String knowledgeRepoUri = "";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VectorDbRepoFactory vectorDbRepoFactory;

    @Autowired
    private FileResolverFactory fileResolverFactory;

    @Autowired
    private RecordService recordService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AIFunctionHelper aiFunctionHelper;

    @Autowired
    private AIFactory aiFactory;

    @Autowired
    private SystemModelSupport systemModelSupport;

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ESHelper esHelper;

    @Autowired
    private SequenceUtil sequenceUtil;

    @Autowired
    private UserLocaleService userLocaleService;

    public FileContentParseBean storeFile(String token, String repoId, String userName, String fileOriginalName,
                                          String fileType,
                                          byte[] fileContent, boolean readImg) throws Exception {
        return doStoreFile(token, repoId, userName, fileOriginalName, fileType, fileContent, readImg);
    }

    private FileContentParseBean doStoreFile(String token, String repoId, String userName, String originalFileName,
                                             String fileType,
                                             byte[] fileContent, boolean readImg) throws Exception {
        FileContentParseBean fileContentParseBean = new FileContentParseBean();
        String fileCode = this.handleStoreFile(userName, originalFileName, fileType, fileContent);

        fileContentParseBean = this.handleParseFileContent(repoId, token, userName, originalFileName, fileContent,
                                                           readImg);
        //String textResult = this.handleParseFileContent(repoId, token, userName, originalFileName, fileContent,
        //readImg);
        long fileId = RandomUtil.getRandomLong();
        String mongoDbCollectionName = "UserKnowledgeFile";
        UserKnowledgeFile userKnowledgeFile = new UserKnowledgeFile();
        userKnowledgeFile.setFileCode(fileCode);
        userKnowledgeFile.setFileId(fileId);
        userKnowledgeFile.setFileName(originalFileName);
        userKnowledgeFile.setKnowledgeRepoId(repoId);
        userKnowledgeFile.setUserName(userName);
        userKnowledgeFile.setFileContent(fileContentParseBean.getFileContent());
        this.mongoTemplate.save(userKnowledgeFile, mongoDbCollectionName);
        fileContentParseBean.setFileId(String.valueOf(fileId));
        return fileContentParseBean;
    }

    private String handleStoreFile(String userName, String originalFileName, String fileType, byte[] fileContent)
            throws Exception {
        String uploadedCode = "";
        try {
            uploadedCode = this.recordService.save(userName, null, originalFileName, fileType, fileContent, true);
        } catch (Exception e) {
            //logger.error(">>>>>>store file error->{}",e.getMessage(),e);
            throw new Exception(">>>>>>store file error: " + e.getMessage(), e);
        }
        return uploadedCode;

    }

    private FileContentParseBean handleParseFileContent(String repoId, String token, String userName,
                                                        String fileOriginalName,
                                                        byte[] fileContent, boolean readImg)
            throws Exception {
        FileContentParseBean fileContentParseBean = new FileContentParseBean();
        String textContent = "";
        List<String> result = new ArrayList<String>();
        try {
            FileResolver fileResolver = this.fileResolverFactory.getFileResolver(token, userName, fileOriginalName,
                                                                                 readImg);
            FileBean fileBean = fileResolver.resolve(fileOriginalName, fileContent);
            if (fileBean.isExcel()) {
                List<Map<String, String>> jsonContents = fileBean.getExcelContentJson();
                fileContentParseBean.setType(2);
                //JSONArray contentArray=new JSONArray();
                //for(String content:jsonContents){
                //    contentArray.put(content);
                //}
                fileContentParseBean.setFileContent(objectMapper.writeValueAsString(jsonContents));
            } else {
                fileBean.eachContent(false, (text) -> {
                    if (text != null && !text.trim().isEmpty()) {
                        //logger.info(">>>>>>after parse pdf get the text->{}",text);
                        result.add(text);
                    }
                });
                textContent = String.join("\n", result);
                fileContentParseBean.setFileContent(textContent);
            }

        } catch (Exception e) {
            logger.error(">>>>>>parse file content error->{}", e.getMessage(), e);
            throw new Exception(">>>>>>parse file content error->" + e.getMessage(), e);
        }
        return fileContentParseBean;
    }

    /*把一个文档开始切片*/
    @Async
    public void processTextContent(String token, String repoId, String userName, long fileId, String textContent,
                                   String fileOriginalName, boolean needSplit, boolean vlEmbedding) {

        //UploadStatusBean existedStatusBean = new UploadStatusBean();
        //existedStatusBean = this.getCurrentProcessStatus(companyId, loginId);

        //ImportStatusBean importStatusBean = this.getImportStatus(userName);
        //long existedRecordCount = importStatusBean.getHasProcessCount();
        ImportStatusBean importStatusBean = new ImportStatusBean();
        String stepText = userLocaleService.getI18nValue(userName, "upload.training");
        importStatusBean.setCurrentStep(stepText);
        importStatusBean.setRepoId(repoId);
        importStatusBean.setRunning(true);
        this.setImportStatus(userName, importStatusBean);
        List<String> textChunks = new ArrayList<>();
        AtomicLong hasProcessCount = new AtomicLong(0); // 已处理条数
        AtomicReference<Double> processPercentage = new AtomicReference<>(0.0); // 处理百分比
        AtomicReference<Double> running = new AtomicReference<>(0.0); // 新增：running值

        try {
            List<CompletableFuture<EmbeddingResult>> futures = new ArrayList<>();
            UserKnowledgeMain userKnowledgeMain = this.getUserKnowledgeMainById(repoId);

            if (userKnowledgeMain == null) {
                return;
            }
            if (fileResolverFactory.isExcel(fileOriginalName)) { //对于excel的处理就是按照换行符，slideNums设1
                textChunks = splitInExcelFormat(repoId, textContent);
            } else {
                if (userKnowledgeMain.getSplitType() == 0) {
                    textChunks = splitTextIntoChunks(repoId, userName, textContent, needSplit);
                } else if (userKnowledgeMain.getSplitType() == 1) {
                    textChunks = splitTextByParagraphMark(repoId, userName, textContent,
                                                          userKnowledgeMain.getParagraphMark(),
                                                          userKnowledgeMain.getSlideNums(), needSplit,
                                                          fileOriginalName);
                } else if (userKnowledgeMain.getSplitType() == 2) {
                    textChunks = splitByPage(repoId, userName, textContent, needSplit, fileOriginalName);
                }
            }
            int totalChunks = textChunks.size();

            String knowledgeRepoPointUrl = this.getPointsUrl(repoId);
            String sequenceKey = "userKnowledgeDetail:" + repoId;
            StringBuilder vlRepoName = new StringBuilder();
            if (vlEmbedding) {
                VectorDbRepo vectorDbRepo = vectorDbRepoFactory.getVectorDbRepo(3);
                vlRepoName.append(userKnowledgeMain.getKnowledgeName()).append("_vl");
                vectorDbRepo.createRepo(vlRepoName.toString());
            }
            for (int i = 0; i < totalChunks; i += THREAD_POOL_SIZE) {
                ImportStatusBean currentStatus = this.getImportStatus(userName);
                if (currentStatus.isStopFlag()) {
                    logger.info(">>>>>>use trigger the stop button");
                    break;
                }
                try {
                    // embedding当前批次的块（最多THREAD_POOL_SIZE个）
                    for (int j = i; j < Math.min(i + THREAD_POOL_SIZE, totalChunks); j++) {
                        long seqNo = this.sequenceUtil.getIncrementalSequence(sequenceKey, 1);
                        final int chunkIndex = j;
                        final String chunkText = textChunks.get(chunkIndex);
                        CompletableFuture<EmbeddingResult> future = CompletableFuture.supplyAsync(() -> {
                            String handleContent = String.valueOf(chunkText);
                            String imgUrl = "";
                            String imgBase64Content = "";
                            try {
                                List<Float> vector = new ArrayList<>();
                                long itemId = RandomUtil.getRandomLong();
                                if (vlEmbedding) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(handleContent);
                                        // 提取content和img字段
                                        handleContent = jsonObject.optString("content", "");
                                        imgUrl = jsonObject.optString("img", "");
                                        if (StringUtil.isNotBlank(imgUrl)) {
                                            imgBase64Content = this.getImageAsBase64(imgUrl);
                                            if (StringUtil.isNotBlank(imgBase64Content)) {
                                                VectorDbRepo imgVectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(
                                                        3);
                                                List<Float> imgVector = imgVectorDbRepo.getVlEmbedding("",
                                                                                                       imgBase64Content);
                                                // 构建payload
                                                PointsWrapper points = new PointsWrapper();
                                                Payload payload = new Payload();
                                                payload.setContent(handleContent);
                                                payload.setFileName(fileOriginalName);
                                                payload.setFileId(fileId);
                                                //构建payload里的additionalProperties
                                                List<String> labelPairs = new ArrayList<>();
                                                String keywords = String.join(", ", labelPairs);
                                                payload.addProperty("keywords", keywords);
                                                // 构建Point
                                                Point p = new Point();
                                                p.setVector(imgVector);
                                                p.setPayload(payload);
                                                p.setId(itemId);
                                                // 最后我们把这个Point变成List结构并完成最终发送报文前的全部工作
                                                points.getPoints().add(p);
                                                imgVectorDbRepo.addBatchEmbeddingItemIntoVectorDB(points,
                                                                                                  this.getVlPointsUrl(
                                                                                                          repoId));
                                            }
                                        }
                                    } catch (Exception ve) {
                                        logger.error(">>>>>>getImg to embedding error->{}", ve.getMessage(), ve);
                                    }
                                }
                                List<String> labelPairs = new ArrayList<>();
                                if (userKnowledgeMain.getLabelList() != null && !userKnowledgeMain.getLabelList()
                                                                                                  .isEmpty()) {
                                    labelPairs = this.labelTextContent(userName, handleContent,
                                                                       userKnowledgeMain.getLabelList());//AI 为每一条数据打标
                                }
                                // 打印结果
                                /*
                                for (Map<String, String> pair : labelPairs) {
                                    logger.info(">>>>>>标签名称: {}, 标签内容: {}",
                                                pair.get("labelName"),
                                                pair.get("labelContent"));
                                }
                                 */
                                //embedding开始
                                //1. 第一步，先把数据保存到mongodb里
                                String savedDetailId = this.storeFileItem(repoId, userName, fileId, fileOriginalName,
                                                                          itemId, labelPairs,
                                                                          handleContent, seqNo);
                                //2. 第二步，把数据存入es
                                this.syncFileItemToES(userName, userKnowledgeMain, repoId, fileId, fileOriginalName,
                                                      String.valueOf(itemId), labelPairs, handleContent, savedDetailId);

                                //3. 第三步，调embedding算法
                                vector = getEmbedding(handleContent, imgBase64Content, 2);

                                // 随机生成500-800毫秒的延时
                                int delay = 2500 + new Random().nextInt(2601); // 生成500-800之间的随机数
                                Thread.sleep(delay);
                                return new EmbeddingResult(itemId, handleContent, vector, labelPairs);
                            } catch (Exception e) {
                                logger.error("线程休眠被中断: {}", e.getMessage(), e);
                                Thread.currentThread().interrupt();
                                return new EmbeddingResult(0l, handleContent, null, null);
                            }
                        }, executorService);
                        futures.add(future);  // 添加这行，将future添加到列表中
                    }
                    // 等待当前批次完成并收集结果
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    // 等待当前批次完成并收集结果，过滤掉空的结果
                    List<EmbeddingResult> batchResults = futures.stream()
                                                                .map(CompletableFuture::join)
                                                                .filter(result -> result.vector != null && !result.vector.isEmpty())
                                                                .collect(Collectors.toList());
                    //经过上述batchResults我们已经得到了一个线程批次的embedding了下面要执行真正进入embedding数据库了
                    // 只有当批次中有有效结果时才计数
                    if (!batchResults.isEmpty()) {
                        PointsWrapper points = new PointsWrapper();
                        // 处理每个有效的结果
                        for (EmbeddingResult result : batchResults) {
                            // 生成唯一的pointId
                            long pointId = result.itemId;

                            // 构建payload
                            Payload payload = new Payload();
                            payload.setContent(result.text);
                            payload.setFileName(fileOriginalName);
                            payload.setFileId(fileId);
                            //构建payload里的additionalProperties
                            List<String> labelPairs = result.labelPairs;
                            String keywords = String.join(", ", labelPairs);
                            payload.addProperty("keywords", keywords);
                            // 构建Point
                            Point p = new Point();
                            p.setVector(result.vector);
                            p.setPayload(payload);
                            p.setId(pointId);
                            // 最后我们把这个Point变成List结构并完成最终发送报文前的全部工作
                            points.getPoints().add(p);
                            //存储pointId到对应的文件中去

                        }
                        VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
                        try {
                            vectorDbRepo.addBatchEmbeddingItemIntoVectorDB(points, knowledgeRepoPointUrl);
                            logger.info(">>>>>>批量进embedding向量库成功->{}", knowledgeRepoPointUrl);
                            this.updateSuccessInfo(repoId, fileId, batchResults.size());
                        } catch (Exception e) {
                            logger.error(">>>>>>批量进embedding向量库失败->{}，把失败记录放入UserKnowledgeFailResult",
                                         e.getMessage(), e);
                            for (EmbeddingResult result : batchResults) {
                                this.storeFailResut(repoId, userName, fileId, fileOriginalName, result.labelPairs,
                                                    result.text);
                            }
                        }
                    }
                    logger.info(">>>>>>totalChunks->{} hasProcessCount->{}", totalChunks, hasProcessCount.get());
                    // 更新处理进度
                    int currentBatchSize = Math.min(THREAD_POOL_SIZE, totalChunks - i);
                    long newCount = hasProcessCount.addAndGet(currentBatchSize);
                    // 计算处理百分比，直接使用0-100的范围
                    double percentage = Math.min(100.0, ((double) hasProcessCount.get() / totalChunks) * 100.0);
                    processPercentage.set(Math.round(percentage * 100.0) / 100.0);
                    logger.info(">>>>>>已处理进度: {}/{} 条记录 ({}%)",
                                newCount,
                                totalChunks,
                                processPercentage
                    );
                    /*把当前状态存入redis*/
                    ImportStatusBean currentStatusBean = this.getImportStatus(userName);
                    currentStatusBean.setRepoId(repoId);
                    stepText = this.userLocaleService.getI18nValue(userName, "upload.embedding");
                    currentStatusBean.setCurrentStep(stepText);
                    //currentStatusBean.setCurrentStep();
                    currentStatusBean.setRunning(true);
                    currentStatusBean.setProcessPercentage(processPercentage.get());
                    currentStatusBean.setHasProcessCount(newCount);
                    currentStatusBean.setRecordCounts(totalChunks);
                    this.setImportStatus(userName, currentStatusBean);
                    futures.clear(); // 清空当前批次的futures列表，准备处理下一批次
                } catch (Exception e) {
                    logger.error(">>>>>>处理第{}批次时发生错误，跳过当前批次->{}",
                                 i / THREAD_POOL_SIZE + 1, e.getMessage(), e);
                    continue;
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>处理用户个人知识库时发生了严重错误->{}", e.getMessage(), e);
        } finally {
            //finally里要把状态设会未运行
            ImportStatusBean currentStatusBean = this.getImportStatus(userName);
            currentStatusBean.setRunning(false);
            this.setImportStatus(userName, currentStatusBean);
        }
    }

    //手工添加一条具体的知识库里的条目
    public void addKnowledgeDetail(String userName, String knowledgeRepoId, String inputContent,
                                   List<String> inputLabels) throws Exception {
        try {
            long fileId = RandomUtil.getRandomShortLong();
            String fileName = "manual add item";
            long itemId = RandomUtil.getRandomLong();
            String contentMd5 = MD5Util.getMD5(inputContent);
            String sequenceKey = knowledgeRepoId + ":manual add";
            long seqNo = this.sequenceUtil.getIncrementalSequence(sequenceKey, 1);
            /**
             * 插入mongodb
             */
            String savedDetailId = this.storeFileItem(knowledgeRepoId, userName, fileId, fileName, itemId, inputLabels,
                                                      inputContent, seqNo);//存mongo
            this.updateSuccessInfo(knowledgeRepoId, fileId, 1);
            /**
             * 插入向量
             */
            List<Float> inputV = this.getEmbedding(inputContent, "", 2);
            VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(2);
            PointsWrapper points = new PointsWrapper();
            // 构建payload
            Payload payload = new Payload();
            payload.setContent(inputContent);
            payload.setFileName(fileName);
            payload.setFileId(fileId);
            //构建payload里的additionalProperties
            String keywords = String.join(",", inputLabels);
            payload.addProperty("keywords", keywords);
            // 构建Point
            Point p = new Point();
            p.setVector(inputV);
            p.setPayload(payload);
            p.setId(itemId);
            // 最后我们把这个Point变成List结构并完成最终发送报文前的全部工作
            points.getPoints().add(p);
            //存储pointId到对应的文件中去
            String url = this.getPointsUrl(knowledgeRepoId);
            vectorDbRepo.addBatchEmbeddingItemIntoVectorDB(points, url);
            /**
             * 构建单条索引
             */
            UserKnowledgeMain userKnowledgeMain = this.getUserKnowledgeMainById(knowledgeRepoId);
            this.syncFileItemToES(userName, userKnowledgeMain, knowledgeRepoId, fileId, fileName,
                                  String.valueOf(itemId), inputLabels, inputContent, savedDetailId);
        } catch (Exception e) {
            throw new Exception(">>>>>>manual add one knowledgeDetail item error: " + e.getMessage(), e);
        }
    }

    private void syncFileItemToES(String userName, UserKnowledgeMain userKnowledgeMain, String repoId, long fileId,
                                  String originalFileName, String itemId, List<String> labelPairs,
                                  String originalContent, String detailId) {
        // 构建索引名称
        String indexName = userKnowledgeMain.getKnowledgeName() + "_index";

        // 构建文档ID
        String docId = itemId;

        // 构建文档数据
        Map<String, Object> docData = new HashMap<>();
        docData.put("itemId", itemId);
        docData.put("originalContent", originalContent);
        docData.put("mongodbId", detailId);
        docData.put("knowledgeRepoId", repoId);
        docData.put("fileId", fileId);
        docData.put("userName", userName);
        docData.put("fileName", originalFileName);
        docData.put("labels", labelPairs);
        this.esHelper.insertOrUpdateDocument(indexName, docId, docData);
    }

    public void setImportStatus(String userName, ImportStatusBean importStatusBean) {
        String redisKey = IMPORT_STATUS_REDIS_KEY + userName;
        try {
            redisTemplate.opsForValue().set(redisKey, importStatusBean);
        } catch (Exception e) {
            logger.error(">>>>>>setImportStatus error->{}", e.getMessage(), e);
        }
    }

    public ImportStatusBean getImportStatus(String userName) {
        ImportStatusBean importStatusBean = new ImportStatusBean();
        String redisKey = IMPORT_STATUS_REDIS_KEY + userName;
        try {
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                importStatusBean = (ImportStatusBean) obj;
                return importStatusBean;
            }
        } catch (Exception e) {
            logger.error("get import status error->{}", e.getMessage());
            importStatusBean = new ImportStatusBean();
        }
        return importStatusBean;
    }

    public List<String> labelTextContent(String userId, String textContent,
                                         List<String> existedLabels) {
        double temperature = 0.1;
        String sendPrompt = this.aiFunctionHelper.getFunctions(10002, textContent);

        List<String> labelPairs = new ArrayList<>();
        try {
            String existedLabelJsonStr = objectMapper.writeValueAsString(existedLabels);
            sendPrompt = sendPrompt.replace("$<labels>", existedLabelJsonStr);
            logger.info(">>>>>>send prompt is->{}", sendPrompt);
            logger.info(">>>>>>先用masterModel去取AI结果");
            //先获得masterModel， masterModel没设会默认取type==1即deekseek本地
            AIModel masterModel = this.systemModelSupport.getMasterModel(userId);
            //如果masterModel为空代表没有设置
            if (masterModel == null) {
                throw new Exception(">>>>>>no any system ai model configured");
            }
            int masterModelType = masterModel.getType();//得到masterModel的type
            AIComponent aiComponent = this.aiFactory.getAIComponent(masterModelType);//通过masterModel的type获取系统ai模型
            String jsonResponse = aiComponent.jsonCall(masterModel, sendPrompt);
            logger.info(">>>>>>jsonResponse->" + jsonResponse);

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            // 确定要处理的节点
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
            return labelPairs;
        } catch (Exception e) {
            try {
                logger.error(">>>>>>先用masterModel去访问出错再用slaveModel试试->{}", e.getMessage(), e);
                AIModel slaveModel = this.systemModelSupport.getSlave(userId);
                //如果slaveModel为空代表没有设置那么也不需要用slave model去重试了，直接就抛错
                if (slaveModel == null) {
                    return new ArrayList<>();
                }

                int slaveModelType = slaveModel.getType();//得到slaveModel的type
                AIComponent aiComponent = this.aiFactory.getAIComponent(slaveModelType);//通过salveModel的type获取系统ai模型
                String jsonResponse = aiComponent.jsonCall(slaveModel, sendPrompt);
                logger.info(">>>>>>jsonResponse->" + jsonResponse);
                labelPairs = new ArrayList<>();
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                // 确定要处理的节点
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
            } catch (Exception se) {
                logger.error(">>>>>>use slave to do ai json call error->{}", e.getMessage(), e);
                return new ArrayList<>();
            }
        }
        return labelPairs;
    }

    private String storeFileItem(String repoId, String userName, long fileId, String originalFileName, long itemId,
                                 List<String> labels, String originalFileContent, long seqNo) throws Exception {
        return doStoreFileItem(repoId, userName, fileId, originalFileName, itemId, labels, originalFileContent, seqNo);
    }

    private String doStoreFileItem(String repoId, String userName, long fileId, String originalFileName, long itemId,
                                   List<String> labels, String originalFileContent, long seqNo) {
        String saveId = "";
        String detailCollectionName = "UserKnowledgeDetail";
        String fileCollectionName = "UserKnowledgeFile";
        List<String> recordLabels = new ArrayList<>();
        String contentMd5 = MD5Util.getMD5(originalFileContent);
        UserKnowledgeDetail userKnowledgeDetail = new UserKnowledgeDetail();
        userKnowledgeDetail.setItemId(String.valueOf(itemId));
        userKnowledgeDetail.setFileId(fileId);
        userKnowledgeDetail.setKnowledgeRepoId(repoId);
        userKnowledgeDetail.setContentMd5(contentMd5);

        if (labels != null && !labels.isEmpty()) {
            recordLabels.clear();
            recordLabels.addAll(labels);
        } else {
            recordLabels = new ArrayList<>();
        }
        userKnowledgeDetail.setLabels(recordLabels);
        userKnowledgeDetail.setFileName(originalFileName);
        userKnowledgeDetail.setOriginalContent(originalFileContent);
        userKnowledgeDetail.setUserName(userName);
        userKnowledgeDetail.setSeqNo(seqNo);

        //Query md5Query = new Query();
        //md5Query.addCriteria(Criteria.where("contentMd5").is(contentMd5));
        //UserKnowledgeDetail md5Object = mongoTemplate.findOne(md5Query, UserKnowledgeDetail.class,
        //                                                     detailCollectionName);
        //if (md5Object == null) {
        UserKnowledgeDetail savedDetail = mongoTemplate.save(userKnowledgeDetail, detailCollectionName);//保存单条记录
        saveId = savedDetail.getId();
        //}

        //操作UserKnowledgeFile
        Query fileQuery = new Query();
        fileQuery.addCriteria(Criteria.where("_id").is(fileId));
        // 使用Update对象来执行items字段+1操作
        Update fileUpdate = new Update().inc("items", 1);
        mongoTemplate.updateFirst(fileQuery, fileUpdate, fileCollectionName);
        return saveId;
    }

    private void updateSuccessInfo(String repoId, long fileId, int successCount) {
        doUpdateSuccessInfo(repoId, fileId, successCount);
    }

    private void doUpdateSuccessInfo(String repoId, long FileId, int successCount) {
        String knowledgeCollectionName = "UserKnowledgeMain";
        //操作UserKnowledgeMain
        Query knowledgeQuery = new Query();
        knowledgeQuery.addCriteria(Criteria.where("_id").is(repoId));
        Update knowledgeUpdate = new Update()
                .inc("successCount", successCount);
        mongoTemplate.updateFirst(knowledgeQuery, knowledgeUpdate, knowledgeCollectionName);//全局成功+1
    }

    private void storeFailResut(String repoId, String userName, long fileId, String originalFileName,
                                List<String> labels, String originalFileChunkContent) throws Exception {
        doStoreFailResult(repoId, userName, fileId, originalFileName, labels, originalFileChunkContent);
    }

    private void doStoreFailResult(String repoId, String userName, long fileId, String originalFileName,
                                   List<String> labels, String originalFileChunkContent) {
        String failCollectionName = "UserKnowledgeFailResult";
        List<String> recordLabels = new ArrayList<>();

        if (labels != null && !labels.isEmpty()) {
            recordLabels.clear();
            recordLabels.addAll(labels);
        } else {
            recordLabels = new ArrayList<>();
        }
        String contentMd5 = MD5Util.getMD5(originalFileChunkContent);
        UserKnowledgeFailResult failResult = new UserKnowledgeFailResult();
        failResult.setContentMd5(contentMd5);
        failResult.setKnowledgeRepoId(repoId);
        failResult.setUserName(userName);
        failResult.setFileId(fileId);
        failResult.setOriginalFileName(originalFileName);
        failResult.setLabels(recordLabels);
        failResult.setFileChunkContent(originalFileChunkContent);
        Query md5Query = new Query();
        md5Query.addCriteria(Criteria.where("contentMd5").is(contentMd5));
        UserKnowledgeFailResult md5Object = mongoTemplate.findOne(md5Query, UserKnowledgeFailResult.class,
                                                                  failCollectionName);
        if (md5Object == null) {
            mongoTemplate.save(failResult, failCollectionName);//保存单条失败记录
        }
        this.updateFailInfo(repoId, fileId);//全局失败+1
    }

    private void updateFailInfo(String repoId, long fileId) {
        String knowledgeCollectionName = "UserKnowledgeMain";
        //操作UserKnowledgeMain
        Query knowledgeQuery = new Query();
        knowledgeQuery.addCriteria(Criteria.where("_id").is(repoId));
        Update knowledgeUpdate = new Update()
                .inc("failCount", 1)
                .inc("itemCount", 1);
        mongoTemplate.updateFirst(knowledgeQuery, knowledgeUpdate, knowledgeCollectionName);
    }

    public UserKnowledgeFile getUserKnowledgeFileByFileId(long fileId) {
        String mongoDbCollectionName = "UserKnowledgeFile";
        UserKnowledgeFile userKnowledgeFile = new UserKnowledgeFile();

        Query query = new Query();
        query.addCriteria(Criteria.where("fileId").is(fileId));
        userKnowledgeFile = this.mongoTemplate.findOne(query, UserKnowledgeFile.class, mongoDbCollectionName);
        return userKnowledgeFile;
    }

    public UserKnowledgeFile getUserKnowledgeFileByRepoId(String repoId) {
        String mongoDbCollectionName = "UserKnowledgeFile";
        UserKnowledgeFile userKnowledgeFile = new UserKnowledgeFile();

        Query query = new Query();
        query.addCriteria(Criteria.where("knowledgeRepoId").is(repoId));
        userKnowledgeFile = this.mongoTemplate.findOne(query, UserKnowledgeFile.class, mongoDbCollectionName);
        return userKnowledgeFile;
    }

    public UserKnowledgeMain getUserKnowledgeMainById(String repoId) {
        String mongoDbCollectionName = "UserKnowledgeMain";
        UserKnowledgeMain userKnowledgeMain = new UserKnowledgeMain();

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(repoId));
        userKnowledgeMain = this.mongoTemplate.findOne(query, UserKnowledgeMain.class, mongoDbCollectionName);
        return userKnowledgeMain;
    }

    public UserKnowledgeDetail getUserKnowledgeDetailById(String detailId) {
        String mongoDbCollectionName = "UserKnowledgeDetail";
        UserKnowledgeDetail detailItem = new UserKnowledgeDetail();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(detailId));
        detailItem = this.mongoTemplate.findOne(query, UserKnowledgeDetail.class, mongoDbCollectionName);
        return detailItem;
    }

    private List<String> splitTextIntoChunks(String repoId, String userName, String text, boolean needSplit) {
        List<String> chunks = new ArrayList<>();
        List<String> returnChunks = new ArrayList<>();
        int length = text.length();
        int startIndex = 0;
        this.setLastChunk(userName, "");
        while (startIndex < length) {
            ImportStatusBean currentStatus = this.getImportStatus(userName);
            if (currentStatus.isStopFlag()) {
                logger.info(">>>>>>use trigger the stop button");
                break;
            }
            // 从startIndex开始寻找下一个段落结束符
            int endIndex = findNextParagraphEnd(text, startIndex);

            // 如果找不到段落结束符，说明已经到了最后一段
            if (endIndex == -1) {
                chunks.add(text.substring(startIndex));
                break;
            }

            // 如果当前段落小于CHUNK_SIZE，尝试合并后续的小段落
            while (endIndex - startIndex < CHUNK_SIZE) {
                int nextEnd = findNextParagraphEnd(text, endIndex);
                // 如果没有下一段了，退出循环
                if (nextEnd == -1) {
                    break;
                }
                endIndex = nextEnd;
            }

            // 检查切割点是否在句子中间
            if (endIndex < length) {
                int completeEndIndex = findCompleteSentenceEnd(text, endIndex);
                if (completeEndIndex != -1) {
                    endIndex = completeEndIndex;
                }
            }

            chunks.add(text.substring(startIndex, endIndex));
            startIndex = endIndex;
        }
        if (needSplit) {
            if (chunks != null && !chunks.isEmpty()) {
                returnChunks = this.aiSplitChunk(repoId, userName, chunks);
                try {
                    Thread.sleep(2500);
                } catch (Exception e) {
                }
            }
        } else {
            returnChunks = new ArrayList<>();
            returnChunks.clear();
            returnChunks.addAll(chunks);
        }
        this.setLastChunk(userName, "");
        return returnChunks;
    }

    private void setLastChunk(String userName, String chunkText) {
        String redisKey = "fountain:task:import:embedding:lastChunk:" + userName;
        redisTemplate.opsForValue().set(redisKey, chunkText);
    }

    private String getLastChunk(String userName, int wordCount) {
        String returnWords = "";
        String redisKey = "fountain:task:import:embedding:lastChunk:" + userName;
        Object obj = redisTemplate.opsForValue().get(redisKey);
        if (obj != null) {
            returnWords = (String) obj;
        }

        return returnWords;
    }

    /**
     * 按照PDF页面分割文本
     *
     * @param userName  用户名
     * @param text      完整PDF文本
     * @param needSplit 是否需要进一步分割
     * @return 分割后的文本块列表
     */
    private List<String> splitByPage(String repoId, String userName, String text, boolean needSplit, String fileName) {
        List<String> pageChunks = new ArrayList<>();
        List<String> returnChunks = new ArrayList<>();
        String lastAbstractPrompt = "";
        // 使用页分隔符分割文本
        String[] pages = text.split("\f");
        this.setLastChunk(userName, "");
        for (int i = 0; i < pages.length; i++) {
            ImportStatusBean currentStatus = this.getImportStatus(userName);
            if (currentStatus.isStopFlag()) {
                logger.info(">>>>>>user triggered the stop button");
                break;
            }
            String page = pages[i].trim();
            if (page.isEmpty()) {
                continue;
            }
            if (needSplit) {
                try {
                    // 添加切分后的文本块
                    String pageString = new String(page);
                    String lastPageContent = this.getLastChunk(userName, 0);
                    if (StringUtils.isBlank(lastPageContent)) {
                        this.setLastChunk(userName, page);
                        returnChunks.add(page);
                    } else {
                        lastAbstractPrompt = this.aiFunctionHelper.getFunctions(30003, lastPageContent);
                        lastAbstractPrompt = lastAbstractPrompt.replace("$<fileName>", fileName);
                        AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
                        String jsonResponse = this.safeJsonCall(userName, lastAbstractPrompt, masterModel, 0.9);
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        String aiAbstract = jsonObject.getString("abstract");
                        //添加上下切分的文本块的末尾slideWords字
                        String finalChunk = aiAbstract + "。\n本章节内容:" + pageString;
                        //切分的文本块先存入redis
                        this.setLastChunk(userName, pageString);
                        returnChunks.add(finalChunk);
                        ImportStatusBean importStatusBean = new ImportStatusBean();
                        String stepText = userLocaleService.getI18nValue(userName, "upload.aiHandleWords");
                        stepText += ": " + pageString.length();
                        importStatusBean.setCurrentStep(stepText);
                        importStatusBean.setRepoId(repoId);
                        importStatusBean.setRunning(true);
                        this.setImportStatus(userName, importStatusBean);
                    }
                } catch (Exception e) {
                    logger.error(">>>>>>ai split page error->{}", e.getMessage(), e);
                }
            } else {
                returnChunks.add(page);
            }
        }
        int pc = 0;
        for (String line : returnChunks) {
            pc += 1;
            logger.info(">>>>>>pc->{}, String->{}", pc, line);
        }
        this.setLastChunk(userName, "");
        return returnChunks;
    }

    //按照段落切，传入的slideNums如果>1代表连续追加几段（滑动窗口的意思）
    private List<String> splitTextByParagraphMark(String repoId, String userName, String text, String paragraphMark,
                                                  int slideNums, boolean needSplit, String fileName) {
        //List<String> chunks = new ArrayList<>();
        List<String> returnChunks = new ArrayList<>();

        // 处理slideNums参数
        slideNums = Math.max(1, slideNums);

        // 设置默认的段落标记
        String actualMark;
        if (StringUtils.isBlank(paragraphMark)) {
            actualMark = "\n";
        } else {
            actualMark = paragraphMark.replace("\\n", "\n");
        }
        logger.info(">>>>>>actualMark->{}", actualMark);
        // 如果文本为空，直接返回空列表
        if (StringUtils.isBlank(text)) {
            return returnChunks;
        }

        int startIndex = 0;
        int textLength = text.length();
        this.setLastChunk(userName, "");
        //logger.info(">>>>>>text->{}",text);
        //logger.info(">>>>>>text length->{}",textLength);
        while (startIndex < textLength) {
            ImportStatusBean currentStatus = this.getImportStatus(userName);
            if (currentStatus.isStopFlag()) {
                logger.info(">>>>>>use trigger the stop button");
                break;
            }
            int endIndex = startIndex;
            int foundMarks = 0;

            // 寻找指定数量的段落标记
            for (int i = 0; i < slideNums && endIndex < textLength; i++) {
                int nextMark = text.indexOf(actualMark, endIndex + (i > 0 ? 1 : 0));
                if (nextMark == -1) {
                    // 如果找不到下一个标记，使用文本末尾
                    endIndex = textLength;
                    break;
                }
                endIndex = nextMark;
                foundMarks++;
            }

            // 提取文本段落
            String chunk = text.substring(startIndex, endIndex).trim();
            //logger.info(">>>>>>split by paragraph chunk->{}",chunk);
            if (StringUtils.isNotBlank(chunk)) {
                String lastAbstractPrompt = "";
                if (needSplit) {
                    try {
                        // 添加切分后的文本块
                        String pageString = new String(chunk);
                        String lastPageContent = this.getLastChunk(userName, 0);
                        if (StringUtils.isBlank(lastPageContent)) {
                            this.setLastChunk(userName, chunk);
                            returnChunks.add(chunk);
                        } else {
                            lastAbstractPrompt = this.aiFunctionHelper.getFunctions(30003, lastPageContent);
                            lastAbstractPrompt = lastAbstractPrompt.replace("$<fileName>", fileName);
                            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
                            String jsonResponse = this.safeJsonCall(userName, lastAbstractPrompt, masterModel, 0.9);
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            String aiAbstract = jsonObject.getString("abstract");
                            //添加上下切分的文本块的末尾slideWords字
                            String finalChunk = aiAbstract + "。\n本章节内容：" + pageString;
                            //切分的文本块先存入redis
                            this.setLastChunk(userName, pageString);
                            returnChunks.add(finalChunk);

                        }
                        ImportStatusBean importStatusBean = new ImportStatusBean();
                        importStatusBean.setRunning(true);
                        importStatusBean.setRepoId(repoId);
                        String stepText = userLocaleService.getI18nValue(userName, "upload.aiHandleWords");
                        stepText += ": " + chunk.length();
                        importStatusBean.setCurrentStep(stepText);
                        this.setImportStatus(userName, importStatusBean);
                    } catch (Exception e) {
                        logger.error(">>>>>>ai split page error->{}", e.getMessage(), e);
                    }
                } else {
                    returnChunks.add(chunk);
                }
            }

            // 更新起始位置
            startIndex = (foundMarks == slideNums) ? endIndex + 1 : endIndex;
        }


        return returnChunks;
    }

    private List<String> splitInExcelFormat(String repoId, String contentJson) {

        List<String> returnChunks = new ArrayList<>();

        // 解析JSON数组
        JSONArray jsonArray = new JSONArray(contentJson);

        // 将每个JSON对象转换为字符串添加到列表中
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String jsonObjectStr = jsonObject.toString();
            returnChunks.add(jsonObjectStr);
        }

        return returnChunks;
    }

    /**
     * 用AI多线程去折一遍每一条chunks
     */
    private List<String> aiSplitChunk(String repoId, String userName, List<String> inputChunks) {
        List<String> result = new ArrayList<>();
        try {
            // 创建固定大小的线程池
            int nThreads = THREAD_POOL_SIZE; // 可以通过配置文件或参数注入
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);

            // 使用 CompletableFuture 收集所有异步任务
            List<CompletableFuture<List<String>>> futures = inputChunks.stream()
                                                                       .map(chunk -> CompletableFuture.supplyAsync(
                                                                               () -> {
                                                                                   return this.aiSplit(repoId, userName,
                                                                                                       chunk);
                                                                               }, executor))
                                                                       .collect(Collectors.toList());

            // 等待所有任务完成并收集结果
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 合并所有结果并展平
            for (CompletableFuture<List<String>> future : futures) {
                ImportStatusBean currentStatus = this.getImportStatus(userName);
                if (currentStatus.isStopFlag()) {
                    logger.info(">>>>>>use trigger the stop button");
                    break;
                }
                List<String> subResults = future.get();
                if (subResults != null) {
                    result.addAll(subResults);
                }
            }

            // 关闭线程池
            executor.shutdown();
        } catch (Exception e) {
            logger.error(">>>>>>aiSplitChunk failed return the original chunk->{}", e.getMessage(), e);
            result = new ArrayList<>();
            result.addAll(inputChunks);
        }
        return result;
    }

    private List<String> aiSplit(String repoId, String userName, String chunkItem) {
        List<String> subResult = new ArrayList<>();
        try {
            ImportStatusBean currentStatus = this.getImportStatus(userName);
            if (currentStatus.isStopFlag()) {
                logger.info(">>>>>>use trigger the stop button");
                return new ArrayList<>();
            }
            AIModel masterModel = this.systemModelSupport.getMasterModel(userName);
            int masterModelType = masterModel.getType();//得到masterModel的type
            AIComponent aiComponent = this.aiFactory.getAIComponent(masterModelType);//通过masterModel的type获取系统ai模型
            String sendPrompt = this.aiFunctionHelper.getFunctions(20003, chunkItem);
            try {
                String jsonResponse = aiComponent.jsonCall(masterModel, sendPrompt);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                // 确定要处理的节点
                JsonNode targetNode = rootNode.has("records") ? rootNode.get("records") : rootNode;
                // 如果目标节点是数组，直接处理
                if (targetNode.isArray()) {
                    for (JsonNode labelNode : targetNode) {
                        // 如果节点是文本值，直接添加到列表中
                        if (labelNode.isTextual()) {
                            subResult.add(labelNode.asText());
                            ImportStatusBean importStatusBean = new ImportStatusBean();
                            String stepText = userLocaleService.getI18nValue(userName, "upload.aiHandleWords");
                            stepText += ": " + labelNode.asText().length();
                            importStatusBean.setCurrentStep(stepText);
                            importStatusBean.setRepoId(repoId);
                            importStatusBean.setRunning(true);
                            this.setImportStatus(userName, importStatusBean);
                        }
                    }
                }

            } catch (Exception ae) {
                try {
                    AIModel slaveModel = this.systemModelSupport.getSlave(userName);
                    int slaveType = slaveModel.getType();//得到masterModel的type
                    aiComponent = this.aiFactory.getAIComponent(slaveType);//通过masterModel的type获取系统ai模型
                    String jsonResponse = aiComponent.jsonCall(masterModel, sendPrompt);
                    JsonNode rootNode = objectMapper.readTree(jsonResponse);
                    // 确定要处理的节点
                    JsonNode targetNode = rootNode.has("records") ? rootNode.get("records") : rootNode;
                    // 如果目标节点是数组，直接处理
                    if (targetNode.isArray()) {
                        for (JsonNode labelNode : targetNode) {
                            // 如果节点是文本值，直接添加到列表中
                            if (labelNode.isTextual()) {
                                subResult.add(labelNode.asText());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(">>>>>>use ai slave model call error->{}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>use ai split single item error->{}", e.getMessage(), e);
            subResult = new ArrayList<>();
            subResult.clear();
            subResult.add(chunkItem);
            return subResult;
        }
        if (subResult == null || subResult.isEmpty()) {
            subResult = new ArrayList<>();
            subResult.add(chunkItem);
        }
        return subResult;
    }

    //按页数折时生成上一页的总结
    private String safeJsonCall(String userName, String prompt, AIModel aiModel, double temperature) {
        int pc = 1;
        String jsonResponse = "";
        try {
            AIComponent aiComponent = this.aiFactory.getAIComponent(aiModel.getType());
            RequestPayload requestPayload = new RequestPayload();
            requestPayload.setTemperature(temperature);
            requestPayload.setStream(false);
            requestPayload.setModel(aiModel.getModelName());
            List<Object> messages = new ArrayList<>();
            Message userMessage = new Message();
            userMessage.setRole("user");
            userMessage.setContent(prompt.toString());
            messages.add(userMessage);
            requestPayload.setMessages(messages);
            jsonResponse = aiComponent.jsonCall(aiModel, requestPayload);
            if (this.isValidJson(jsonResponse)) {
                return jsonResponse;
            } else {
                throw new Exception(">>>>>>master call jsonResponse->" + jsonResponse + " is not a valid response");
            }
        } catch (Exception e) {
            try {
                logger.error(">>>>>>use master json call ai error->{}, switch to slave json call", e.getMessage(), e);
                if (pc >= 2) {
                    logger.error(">>>>>>use slave model call has error->{}", e.getMessage(), e);
                    return "";
                }
                AIModel slaveModel = this.systemModelSupport.getSlave(userName);
                jsonResponse = this.safeJsonCall(userName, prompt, slaveModel, temperature);
                if (this.isValidJson(jsonResponse)) {
                    return jsonResponse;
                } else {
                    throw new Exception(">>>>>>slave call jsonResponse->" + jsonResponse + " is not a valid response");
                }
            } catch (Exception ae) {
                logger.error(">>>>>>safeJsonCall error->{}", e.getMessage());
                return "";
            }
        }
    }

    /**
     * 查找下一个段落结束位置
     */
    private int findNextParagraphEnd(String text, int startPos) {
        for (int i = startPos; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 查找完整句子的结束位置
     *
     * @param text     要查找的文本
     * @param startPos 开始查找的位置
     * @return 完整句子的结束位置，如果找不到返回文本长度
     */
    private int findCompleteSentenceEnd(String text, int startPos) {
        for (int i = startPos; i < text.length(); i++) {
            char currentChar = text.charAt(i);
            // 检查是否是句子结束符
            for (char endingChar : SENTENCE_ENDINGS) {
                if (currentChar == endingChar) {
                    return i + 1;
                }
            }
        }
        // 如果找不到句子结束符，返回文本末尾
        return text.length();
    }

    // 调用后端API的方法
    private List<Float> getEmbedding(String text, String imgBase64Content, int embeddingType) throws Exception {
        List<Float> vectorResult = new ArrayList<>();
        //这儿需要请求bge
        try {
            VectorDbRepo vectorDbRepo = this.vectorDbRepoFactory.getVectorDbRepo(embeddingType);
            vectorResult = vectorDbRepo.getEmbedding(text);
        } catch (Exception e) {
            throw new Exception(">>>>>>获取embedding时出错了->" + e.getMessage(), e);
        }
        return vectorResult;
    }

    //获得qdrant库内的主url
    public String getKnowledgeRepoUrl(String repoId) throws Exception {
        String majorUrl = knowledgeRepoUri + "/" + this.getKnowledgeRepoName(repoId);
        return majorUrl;
    }

    //获得qdrant库内的主url_vl/points用作points相关操作
    public String getVlPointsUrl(String repoId) throws Exception {
        String pointsUrl = this.getKnowledgeRepoUrl(repoId) + "_vl" + "/points";
        return pointsUrl;
    }

    //获得qdrant库内的主url/points用作points相关操作
    public String getPointsUrl(String repoId) throws Exception {
        String pointsUrl = this.getKnowledgeRepoUrl(repoId) + "/points";
        return pointsUrl;
    }

    //获得qdrant库内的主url/index用作index相关操作
    public String getIndexUrl(String repoId) {
        try {
            String indexUrl = this.getKnowledgeRepoUrl(repoId) + "/index";
            return indexUrl;
        } catch (Exception e) {
            logger.error(">>>>>>getIndexUrl error->{}", e.getMessage(), e);
        }
        return "";
    }

    //通过repoId得到知识库的knowledgeName
    private String getKnowledgeRepoName(String repoId) throws Exception {
        String knowledgeRepoName = "";
        String mongoCollectionName = "UserKnowledgeMain";
        Query query = new Query(Criteria.where("_id").is(repoId));
        UserKnowledgeMain userKnowledgeMain = mongoTemplate.findOne(query, UserKnowledgeMain.class,
                                                                    mongoCollectionName);
        if (userKnowledgeMain != null) {
            knowledgeRepoName = userKnowledgeMain.getKnowledgeName();
        }
        return knowledgeRepoName;
    }

    public List<UserKnowledgeDetail> getDetailListWithMainId(String userName, String knowledgeRepoId) {
        List<UserKnowledgeDetail> detailList = new ArrayList<>();
        String collectionName = "UserKnowledgeDetail";
        Query query = new Query();
        query.addCriteria(Criteria.where("knowledgeRepoId").is(knowledgeRepoId));
        query.addCriteria(Criteria.where("userName").is(userName));
        detailList = mongoTemplate.find(query, UserKnowledgeDetail.class, collectionName);
        return detailList;
    }

    //根据UserKnowledgeMain里的id查到UserKnowledgeDetail里所有的数据然后同步到es，此同步为覆盖式同步
    public void syncToES(String userName, String knowledgeRepoId) {
        try {
            UserKnowledgeMain userKnowledgeMain = this.getUserKnowledgeMainById(knowledgeRepoId);
            if (userKnowledgeMain == null) {
                logger.error(">>>>>>该知识库id->{} 已经不存在", knowledgeRepoId);
                return;
            }
            List<UserKnowledgeDetail> detailList = new ArrayList<>();
            detailList = this.getDetailListWithMainId(userName, knowledgeRepoId);
            if (detailList != null && !detailList.isEmpty()) {

                String esIndexName = userKnowledgeMain.getKnowledgeName() + "_index";
                this.esHelper.createIndexIfNotExists(esIndexName);
                // 设置批量大小
                final int BATCH_SIZE = 25;
                BulkRequest bulkRequest = new BulkRequest();
                int count = 0;

                for (UserKnowledgeDetail detailItem : detailList) {
                    IndexRequest indexRequest = new IndexRequest(esIndexName)
                            .id(String.valueOf(detailItem.getItemId()))
                            .source(convertToMap(detailItem), XContentType.JSON);
                    bulkRequest.add(indexRequest);
                    count++;

                    // 当达到批量大小或是最后一批数据时，执行批量插入
                    if (count >= BATCH_SIZE || count == detailList.size()) {
                        this.executeBulkRequest(bulkRequest);
                        Thread.sleep(0);
                        // 重新创建新的 BulkRequest
                        bulkRequest = new BulkRequest();
                        count = 0;
                    }
                }

                // 处理最后可能剩余的数据
                if (bulkRequest.numberOfActions() > 0) {
                    this.executeBulkRequest(bulkRequest);
                }
            }

        } catch (Exception e) {
            logger.error(">>>>>>syncToES process error->{}", e.getMessage(), e);
        }
    }


    // 添加 convertToMap 方法
    private Map<String, Object> convertToMap(UserKnowledgeDetail detail) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", detail.getItemId());
        // 添加其他需要存入 ES 的字段
        // 例如：
        map.put("originalContent", detail.getOriginalContent());
        map.put("mongodbId", detail.getId());
        map.put("knowledgeRepoId", detail.getKnowledgeRepoId());
        map.put("fileId", detail.getFileId());
        map.put("userName", detail.getUserName());
        map.put("fileName", detail.getFileName());
        map.put("labels", detail.getLabels());
        // ... 其他字段

        return map;
    }

    // 抽取执行批量请求的方法
    private void executeBulkRequest(BulkRequest bulkRequest) throws Exception {
        try {
            if (bulkRequest.numberOfActions() == 0) {
                return;
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);

            // 检查是否有失败的操作
            if (bulkResponse.hasFailures()) {
                // 处理失败情况
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        logger.error(">>>>>>DetailList {} failed: {}",
                                     bulkItemResponse.getId(),
                                     failure.getMessage());
                    }
                }
            } else {
                logger.info(">>>>>>批量插入成功，本批次处理 {} 条数据", bulkRequest.numberOfActions());
            }
        } catch (Exception e) {
            logger.error(">>>>>>es blukInsert error, skip this batch->{}", e.getMessage(), e);
        }
    }


    public List<KnowledgeResult> queryFromEs(String repoId, String rewritedPrompt) {
        List<KnowledgeResult> esResult = new ArrayList<>();
        try {
            UserKnowledgeMain userKnowledgeMain = this.getUserKnowledgeMainById(repoId);
            if (userKnowledgeMain == null) {
                logger.error(">>>>>>索引依赖的知识库UserKnowledgeMain.id->{} 已经不存在 ", repoId);
                return new ArrayList<>();
            }
            String esIndexName = userKnowledgeMain.getKnowledgeName() + "_index";
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // 1. labels精确匹配
            boolQuery.should(QueryBuilders.termsQuery("labels", rewritedPrompt).boost(1.2f));

            // 2. 原文内容匹配
            boolQuery.should(QueryBuilders.matchQuery("originalContent", rewritedPrompt).boost(1.0f));

            // 3. labels全文搜索匹配
            boolQuery.should(QueryBuilders.matchQuery("labels.full", rewritedPrompt).boost(0.8f));

            // 设置最小匹配条件，至少满足其中一个should条件
            boolQuery.minimumShouldMatch(1);

            sourceBuilder.query(boolQuery);

            // 创建搜索请求
            SearchRequest searchRequest = new SearchRequest(esIndexName);
            searchRequest.source(sourceBuilder);

            // 执行搜索
            SearchResponse searchResponse = this.esClient.search(searchRequest, RequestOptions.DEFAULT);

            // 处理搜索结果
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                KnowledgeResult result = new KnowledgeResult();
                result.setId(sourceAsMap.get("mongodbId") != null ? sourceAsMap.get("mongodbId").toString() : "");
                result.setPointId(
                        sourceAsMap.get("itemId") != null ? Long.parseLong(sourceAsMap.get("itemId").toString()) : 0L);
                result.setFileName(sourceAsMap.get("fileName") != null ? sourceAsMap.get("fileName").toString() : "");
                result.setFileContent(sourceAsMap.get("originalContent") != null ? sourceAsMap.get("originalContent")
                                                                                              .toString() : "");
                esResult.add(result);
            }
        } catch (Exception e) {
            // 处理异常
            logger.error(">>>>>>es query error->{}", e.getMessage(), e);
            return new ArrayList<>();
        }
        return esResult;
    }

    private boolean isValidJson(String jsonString) {
        try {
            if (jsonString.trim().startsWith("{")) {
                // 尝试解析为JSONObject
                new JSONObject(jsonString);
            } else if (jsonString.trim().startsWith("[")) {
                // 尝试解析为JSONArray
                new org.json.JSONArray(jsonString);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getImageAsBase64(String imgUrl) {
        try {
            if (imgUrl == null || imgUrl.trim().isEmpty()) {
                return "";
            }

            // 使用RestTemplate获取图片字节数组
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imgUrl, byte[].class);
            byte[] imageBytes = response.getBody();

            if (imageBytes != null && imageBytes.length > 0) {
                // 将图片字节数组转换为Base64字符串
                return Base64.getEncoder().encodeToString(imageBytes);
            }

            return "";
        } catch (Exception e) {
            // 记录异常但不中断流程
            logger.error("获取图片失败: " + e.getMessage(), e);
            return "";
        }
    }
}
