package com.mkyuan.fountainbase.ai.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.ai.bean.DifyBean;
import com.mkyuan.fountainbase.ai.service.AIFunctionService;
import com.mkyuan.fountainbase.ai.service.DifyService;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
public class DifyMgtApi {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private DifyService difyService;

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/dify/listAllDifyConfigs", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean listAllDifyConfigs(@RequestHeader("token") String token,
                                           @RequestHeader("userName") String userName,
                                           @RequestBody JSONObject params) {
        //创建一个初始化的分页对象
        Pageable pageable = PageRequest.of(0, DifyService.PAGESIZE); // 创建一个分页请求
        Page<DifyBean> difConfigs = new PageImpl<>(Collections.emptyList(), pageable, 0);
        try {
            int pageNumber = 0;
            int pageSize = AIFunctionService.PAGESIZE;
            if (params.containsKey("pageNumber")) {
                pageNumber = params.getInteger("pageNumber");
            }
            if (params.containsKey("pageSize")) {
                pageSize = params.getInteger("pageSize");
            }
            difConfigs = this.difyService.listAllDifyConfigs(userName, pageNumber, pageSize);
        } catch (Exception e) {
            logger.error(">>>>>>listAllDifyConfigs error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, difConfigs);
    }


    @MyTokenCheck
    @RequestMapping(value = "/api/ai/dify/addDifyConfig", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean addDifyConfig(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                      @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        String responseMode = "blocking";
        //String decryptApiKey="";
        try {
            if (!params.containsKey("sequenceNo")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (!params.containsKey("description")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            int sequenceNo = params.getInteger("sequenceNo");
            String description = params.getString("description");
            responseMode = params.getString("responseMode");
            String user = params.getString("user");
            String encryptApiKey=params.getString("apiKey");
            //decryptApiKey= EncryptUtil.decrypt_safeencode(encryptApiKey,secretKey);
            DifyBean difyBean = new DifyBean();
            difyBean.setSequenceNo(String.valueOf(sequenceNo));
            difyBean.setDescription(description);
            difyBean.setUser(user);
            difyBean.setResponseMode(responseMode);
            difyBean.setUserName(userName);
            difyBean.setApiKey(encryptApiKey);
            int result = this.difyService.addDifyConfig(userName, difyBean);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "保存成功", result);
        } catch (Exception e) {
            logger.error(">>>>>>add difyConfig error->{}", e.getMessage(), e);
            resp = new ResponseBean(ResponseCodeEnum.FAIL);
        }
        return resp;
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/dify/updateDifyConfigs", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean updateDifyConfigs(@RequestHeader("token") String token,
                                          @RequestHeader("userName") String userName,
                                          @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        try {
            int sequenceNo = params.getInteger("sequenceNo");
            String description = params.getString("description");
            String responseMode = params.getString("responseMode");
            String user = params.getString("user");
            String difyId = params.getString("difyId");
            String encryptApiKey=params.getString("apiKey");
            DifyBean updatedDifyBean = new DifyBean();
            updatedDifyBean.setId(difyId);
            updatedDifyBean.setSequenceNo(String.valueOf(sequenceNo));
            updatedDifyBean.setDescription(description);
            updatedDifyBean.setResponseMode(responseMode);
            updatedDifyBean.setApiKey(encryptApiKey);
            updatedDifyBean.setUser(user);
            this.difyService.updateDifyConfigs(userName, updatedDifyBean);
            resp = new ResponseBean(ResponseCodeEnum.SUCCESS);
        } catch (Exception e) {
            logger.error(">>>>>>updateDifConfig error->{}", e.getMessage(), e);
            resp = new ResponseBean(ResponseCodeEnum.FAIL);
        }
        return resp;
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/dify/deleteDifyConfigs", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean deleteDifyConfigs(@RequestHeader("token") String token,
                                          @RequestHeader("userName") String userName,
                                          @RequestBody JSONObject params) {
        try {
            List<String> idList = params.getJSONArray("idList")
                                        .toJavaList(String.class);
            if (idList.size() > 0) {
                this.difyService.deleteDifyConfigs(userName, idList);
            }
        } catch (Exception e) {
            logger.error(">>>>>>delete selected dify configs error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }
}
