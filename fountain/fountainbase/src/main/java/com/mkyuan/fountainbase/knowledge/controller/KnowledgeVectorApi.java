package com.mkyuan.fountainbase.knowledge.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.knowledge.bean.FileContentParseBean;
import com.mkyuan.fountainbase.knowledge.bean.ImportStatusBean;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeDetail;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeMgtHelper;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeMgtService;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeVectorHelper;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeVectorService;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
public class KnowledgeVectorApi {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private String IMPORT_STATUS_REDIS_KEY = "fountain:task:import:embedding:status:";//后面加userName

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @Autowired
    private RedisTemplate redisTemplate;

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/detail/stopUploadProcess", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean stopUploadProcess(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                          @RequestBody JSONObject params){
        try{
            this.knowledgeVectorService.stopUploadProcess(userName);
        }catch(Exception e){
            logger.error(">>>>>>stopUploadProcess error->{}",e.getMessage(),e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/uploadDoc", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PrivilegeCheck
    public ResponseBean uploadDoc(@RequestParam("file") MultipartFile file, @RequestParam("repoId") String repoId,
                                  @RequestParam("needSplit") boolean needSplit,
                                  @RequestParam("readImg") boolean readImg,
                                  @RequestHeader("token") String token, @RequestHeader("userName") String userName) {
        FileContentParseBean fileContentParseBean=new FileContentParseBean();
        try {
            fileContentParseBean=this.knowledgeVectorService.uploadDocIntoKnowledge(token, repoId, userName, file, needSplit,readImg);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, fileContentParseBean);//代表上传成功
        } catch (Exception e) {
            logger.error(">>>>>>上传文档出错->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, 500);//代表上传出错
        }
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/executeEmbedding", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean executeEmbedding(@RequestBody JSONObject params,
                                         @RequestHeader("token") String token, @RequestHeader("userName") String userName) {

        String repoId=params.getString("repoId");
        String fileIdStr=params.getString("fileId");
        long fileId=Long.parseLong(fileIdStr);
        boolean needSplit=params.getBoolean("needSplit");
        boolean readImg=params.getBoolean("readImg");
        boolean vlEmbedding=params.getBoolean("vlEmbedding");
        this.knowledgeVectorService.executeEmbedding(token,repoId,userName,fileId,needSplit,readImg,vlEmbedding);
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/detail/list", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean detailList(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                   @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        //创建一个初始化的分页对象
        Pageable pageable = PageRequest.of(0, KnowledgeMgtService.PAGESIZE); // 创建一个分页请求
        Page<UserKnowledgeDetail> detailList = new PageImpl<>(Collections.emptyList(), pageable, 0);
        int pageNumber = 0;
        int pageSize = KnowledgeMgtService.PAGESIZE;
        String repoId = "";
        try {
            if (!params.containsKey("repoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            repoId = params.getString("repoId");
            if (params.containsKey("pageNumber")) {
                pageNumber = params.getInteger("pageNumber");
            }
            if (params.containsKey("pageSize")) {
                pageSize = params.getInteger("pageSize");
            }
            String searchedContent = "";
            if (params.containsKey("searchedContent")) {
                searchedContent = params.getString("searchedContent");
            }
            boolean isEmbedding = false;
            if (params.containsKey("isEmbedding")) {
                isEmbedding = params.getBoolean("isEmbedding");
            }
            detailList = this.knowledgeVectorService.getPagedKnowledgeDetails(userName, repoId, pageNumber, pageSize,
                                                                              searchedContent, isEmbedding);
        } catch (Exception e) {
            logger.error(">>>>>>display knowledge detail error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, detailList);
    }

    //通过repoId得到所有的UserKnowledgeMain信息
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/detail/getKnowledgeInfo", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getKnowledgeInfo(@RequestHeader("token") String token,
                                         @RequestHeader("userName") String userName,
                                         @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        UserKnowledgeMain userKnowledgeMain = new UserKnowledgeMain();
        String repoId = "";
        try {
            if (!params.containsKey("repoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            repoId = params.getString("repoId");
            userKnowledgeMain = this.knowledgeVectorService.getUserKnowledgeMainById(repoId);
        } catch (Exception e) {
            logger.error(">>>>>>getKnowledgeInfo api error->{}", e.getMessage(), e);
            userKnowledgeMain = new UserKnowledgeMain();
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, userKnowledgeMain);
    }

    /**
     * 用于查看导入进度用的
     *
     * @param token
     * @param userName
     * @param params
     * @return
     */
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/detail/getImportStatus", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getImportStatus(@RequestHeader("token") String token,
                                        @RequestHeader("userName") String userName,
                                        @RequestBody JSONObject params) {
        ImportStatusBean importStatusBean = new ImportStatusBean();
        try {
            if (!params.containsKey("userName")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            String redisKey = IMPORT_STATUS_REDIS_KEY + userName;
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                importStatusBean = (ImportStatusBean) obj;
                return new ResponseBean(ResponseCodeEnum.SUCCESS, importStatusBean);
            }
        } catch (Exception e) {
            logger.error(">>>>>>getImportStatus api error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, importStatusBean);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/settings/queryElasticStatus", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean queryElasticStatus(@RequestHeader("token") String token,
                                           @RequestHeader("userName") String userName,
                                           @RequestBody JSONObject params) {
        String redisKey = "fountain:settings:es:available";
        boolean esAvailable = false;
        try {
            Object obj = redisTemplate.opsForValue().get(redisKey);
            if (obj != null) {
                esAvailable = (boolean) obj;
            }
            return new ResponseBean(ResponseCodeEnum.SUCCESS, esAvailable);
        } catch (Exception e) {
            logger.error(">>>>>>query elastic is available error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, false);
        }
    }

    //把mongodb里的数据同步es操作

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/syncToES", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean syncToES(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                 @RequestBody JSONObject params) {
        try {
            if (!params.containsKey("knowledgeRepoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            String knowledgeRepoId = params.getString("knowledgeRepoId");
            this.knowledgeVectorService.syncToES(userName, knowledgeRepoId);
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        } catch (Exception e) {
            logger.error(">>>>>>syncToES api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }

    //手工添加一条记录
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/detail/addKnowledgeDetail", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean addKnowledgeDetail(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                 @RequestBody JSONObject params) {
        try {
            if (!params.containsKey("knowledgeRepoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            String knowledgeRepoId = params.getString("knowledgeRepoId");
            String inputContent=params.getString("content");
            List<String> inputLabels = params.getJSONArray("labels").toJavaList(String.class);
            this.knowledgeVectorService.addKnowledgeDetail(userName,knowledgeRepoId,inputContent,inputLabels);
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        } catch (Exception e) {
            logger.error(">>>>>>syncToES api error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }
}
