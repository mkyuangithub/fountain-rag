package com.mkyuan.fountainbase.knowledge.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeDetail;
import com.mkyuan.fountainbase.knowledge.bean.UserKnowledgeMain;
import com.mkyuan.fountainbase.knowledge.bean.VectorIndexBean;
import com.mkyuan.fountainbase.knowledge.bean.VectorIndexRequest;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeMgtService;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class KnowledgeMgtAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String REDIS_USER_KEY = "fountain:userinfo:token:";
    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KnowledgeMgtService knowledgeMgtService;

    //建立索引
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/makeIndex", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean makeIndex(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                  @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<String> fieldNameList = params.getJSONArray("fieldNameList").toJavaList(String.class);
            String knowledgeRepoId = params.getString("knowledgeRepoId");
            int min_token_len = params.getInteger("min_token_len");
            int max_token_len = params.getInteger("max_token_len");

            for (String fieldName : fieldNameList) {
                VectorIndexRequest vectorIndexRequest = new VectorIndexRequest();
                vectorIndexRequest.setFieldName(fieldName);
                vectorIndexRequest.setLowercase(true);
                vectorIndexRequest.setFieldType("text");
                vectorIndexRequest.setTokenizer("multilingual");
                vectorIndexRequest.setMin_token_len(min_token_len);
                vectorIndexRequest.setMax_token_len(max_token_len);
                this.knowledgeMgtService.makeIndex(knowledgeRepoId, vectorIndexRequest);
            }

        } catch (Exception e) {
            logger.error(">>>>>>建立索引失败->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/labelRepo", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean labelRepo(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                  @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            int labelAction = 1;
            String inputPrompt = "";
            String repoId = "";
            if (!params.containsKey("repoPrompt")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (!params.containsKey("repoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (!params.containsKey("labelAction")) {
                labelAction = 1;
            }
            labelAction = params.getInteger("labelAction");
            inputPrompt = params.getString("repoPrompt");
            repoId = params.getString("repoId");
            this.knowledgeMgtService.labelRepo(repoId, userName, inputPrompt, labelAction);
            resp = new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "success", null);

        } catch (Exception e) {
            logger.error(">>>>>>labelRepo api call error->{}", e.getMessage(), e);
        }
        return resp;
    }

    //设定切分方式API
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/setSplitType", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean setSplitType(@RequestHeader("token") String token,
                                        @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        try {
            String repoId="";
            int splitType=0;
            String paragraphMark="\n";
            int slideNums=1;
            if(!params.containsKey("repoId")){
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if(params.containsKey("repoId")){
                repoId=params.getString("repoId");
            }
            if(params.containsKey("splitType")){
                splitType=params.getInteger("splitType");
            }
            if(params.containsKey("slideNums")){
                slideNums=params.getInteger("slideNums");
            }
            if(params.containsKey("paragraphMark")){
                paragraphMark=params.getString("paragraphMark");
            }
            if(slideNums<1){
                slideNums=1;
            }
            this.knowledgeMgtService.setSplitType(repoId,splitType,paragraphMark,slideNums);
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        }catch(Exception e){
            logger.error(">>>>>>设定段落切分标志错误->{}",e.getMessage(),e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }

    }


    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/saveMajorPrompt", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean saveMajorPrompt(@RequestHeader("token") String token,
                                        @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            String majorPrompt = "";
            String repoId = "";
            if (!params.containsKey("repoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            repoId = params.getString("repoId");
            if (params.containsKey("majorPrompt")) {
                majorPrompt = params.getString("majorPrompt");
            }
            this.knowledgeMgtService.saveMajorPrompt(repoId, majorPrompt);
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        } catch (Exception e) {
            logger.error(">>>>>>saveMajorPrompt API error->{}", e.getMessage(), e);
        }
        return resp;
    }

    /**
     * 用来显示所有的用户名下的知识库用的
     *
     * @param token
     * @param userName
     * @param params
     * @return
     */
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/list", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean list(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                             @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        //创建一个初始化的分页对象
        Pageable pageable = PageRequest.of(0, KnowledgeMgtService.PAGESIZE); // 创建一个分页请求
        Page<UserKnowledgeMain> knowledgeRepoList = new PageImpl<>(Collections.emptyList(), pageable, 0);
        try {

            int pageNumber = 0;
            int pageSize = KnowledgeMgtService.PAGESIZE;
            if (params.containsKey("pageNumber")) {
                pageNumber = params.getInteger("pageNumber");
            }
            if (params.containsKey("pageSize")) {
                pageSize = params.getInteger("pageSize");
            }
            knowledgeRepoList = this.knowledgeMgtService.getPagedUserKnowledgeRepoList(userName, pageNumber, pageSize);
            //return new ResponseBean(ResponseCodeEnum.SUCCESS, knowledgeRepoList);
        } catch (Exception e) {
            logger.error(">>>>>>display user's knowledge repo list error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, knowledgeRepoList);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/add", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean add(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                            @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            String knowledgeRepoDescr = "";
            if (!params.containsKey("knowledgeName")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            String knowledgeName = params.getString("knowledgeName");
            if (StringUtil.isBlank(knowledgeName)) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (params.containsKey("knowledgeRepoDescr")) {
                knowledgeRepoDescr = params.getString("knowledgeRepoDescr");
            }
            this.knowledgeMgtService.addUserKnowledge(userName, knowledgeName, knowledgeRepoDescr);

            return new ResponseBean(ResponseCodeEnum.SUCCESS, 100);
        } catch (Exception e) {
            logger.error(">>>>>>knowledge add API error->{}", e.getMessage(), e);
            if (e.getMessage().contains("E11000 duplicate key")) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "duplicate key", 101);
            }
        }
        return resp;
    }



    @RequestMapping(value = "/api/knowledge/delete", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean delete(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                               @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            String knowledgeRepoId = "";
            if (params.containsKey("knowledgeRepoId")) {
                knowledgeRepoId = params.getString("knowledgeRepoId");
            }
            this.knowledgeMgtService.deleteUserKnowledge(userName, knowledgeRepoId);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, "delete");
        } catch (Exception e) {
            logger.error(">>>>>>knowledge delete API error->{}", e.getMessage(), e);
        }
        return resp;
    }

    /* 通过knowledgeDetailId获取具体的一条知识库条目信息*/
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/getDetailKnowledgeItem", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getDetailKnowledgeItem(@RequestHeader("token") String token,
                                               @RequestHeader("userName") String userName,
                                               @RequestBody JSONObject params) {
        UserKnowledgeDetail userKnowledgeDetail = new UserKnowledgeDetail();
        try {
            if (!params.containsKey("detailId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            String detailId = params.getString("detailId");
            userKnowledgeDetail = this.knowledgeMgtService.getDetailKnowledgeItem(userName, detailId);
        } catch (Exception e) {
            logger.error(">>>>>>getDetailKnowledgeItem api error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, userKnowledgeDetail);
    }

    //update one detail knowledge
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/updateDetailKnowledgeItem", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean updateDetailKnowledgeItem(@RequestHeader("token") String token,
                                                  @RequestHeader("userName") String userName,
                                                  @RequestBody JSONObject params) {
        try {
            String detailId = params.getString("detailId");//UserknowledgeDetail的主键
            String knowledgeRepoId = params.getString("knowledgeRepoId");
            String originalContent = params.getString("originalContent");
            List<String> inputLabels = params.getJSONArray("labels").toJavaList(String.class);
            UserKnowledgeDetail updatedKnowledgeDetailItem = new UserKnowledgeDetail();
            updatedKnowledgeDetailItem.setId(detailId);
            updatedKnowledgeDetailItem.setKnowledgeRepoId(knowledgeRepoId);
            updatedKnowledgeDetailItem.setOriginalContent(originalContent);
            updatedKnowledgeDetailItem.setLabels(inputLabels);
            int updatedResult = this.knowledgeMgtService.updateDetailKnowledgeItem(userName,
                                                                                   updatedKnowledgeDetailItem);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, updatedResult);
        } catch (Exception e) {
            logger.error(">>>>>>update knoweldgeDetailItem error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);

        }
    }

    //delete one detail knowledgeitem
    @MyTokenCheck
    @RequestMapping(value = "/api/knowledge/deleteDetailKnowledgeItem", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean deleteDetailKnowledgeItem(@RequestHeader("token") String token,
                                                  @RequestHeader("userName") String userName,
                                                  @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            if (!params.containsKey("knowledgeRepoId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if(!params.containsKey("detailIds")){
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            List<String> detailIds=params.getJSONArray("detailIds").toJavaList(String.class);
            String knowledgeRepoId = params.getString("knowledgeRepoId");
            int result = this.knowledgeMgtService.deleteDetailKnowledgeItem(userName, knowledgeRepoId, detailIds);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
        } catch (Exception e) {
            logger.error(">>>>>>delete one detail knowledge item error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

}
