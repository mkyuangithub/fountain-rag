package com.mkyuan.fountainbase.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.util.EncryptUtil;
import com.mkyuan.fountainbase.knowledge.service.KnowledgeMgtService;
import com.mkyuan.fountainbase.user.bean.UserInfo;
import com.mkyuan.fountainbase.user.service.SystemUserMgtService;
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
public class SystemUserMgtApi {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private SystemUserMgtService systemUserMgtService;

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    //search user api
    @RequestMapping(value = "/api/user/searchUser", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean searchUser(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                               @RequestBody JSONObject params) {
        Pageable pageable = PageRequest.of(0, KnowledgeMgtService.PAGESIZE); // 创建一个分页请求
        Page<UserInfo> userList = new PageImpl<>(Collections.emptyList(), pageable, 0);
        int pageNumber = 1;
        int pageSize = SystemUserMgtService.PAGESIZE;
        String searchUserName="";
        if (!params.containsKey("pageNumber")) {
            pageNumber = 1;
        }
        if (!params.containsKey("pageSize")) {
            pageSize = SystemUserMgtService.PAGESIZE;
        }
        if(params.containsKey("searchUserName")){
            searchUserName=params.getString("searchUserName");
        }
        pageNumber = params.getInteger("pageNumber");
        pageSize = params.getInteger("pageSize");
        try {
            userList = this.systemUserMgtService.searchUser(userName,pageNumber,pageSize,searchUserName);
        } catch (Exception e) {
            logger.error(">>>>>>searchUser api error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, userList);
    }

    @RequestMapping(value = "/api/user/add", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean addUser(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                @RequestBody JSONObject params) {
        int result = 0;
        try {
            String inputUserName = "";
            String inputEncryptPassword = "";
            String decryptInputPassword = "";
            int inputType = 0;

            if (!params.containsKey("inputUserName") || !params.containsKey("inputPassword") || !params.containsKey(
                    "inputType")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }

            inputUserName = params.getString("inputUserName");
            inputEncryptPassword = params.getString("inputPassword");
            inputType = params.getInteger("inputType");
            if (inputUserName.equalsIgnoreCase("admin")) {
                logger.info(">>>>>>对于admin用户直接跳过认为是成功以避免误操作系统最高权限用户");
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "success", 1);
            }
            try {
                decryptInputPassword = EncryptUtil.decrypt_safeencode(inputEncryptPassword, secretKey);
            } catch (Exception e) {
                decryptInputPassword = inputEncryptPassword;
            }
            this.systemUserMgtService.addUser(inputUserName, decryptInputPassword, inputType, userName);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, 1);
        } catch (Exception e) {
            if (e.getMessage().contains("E11000 duplicate key")) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "duplicate key", 101);
            } else {
                logger.error(">>>>>>addUser error->{}", e.getMessage(), e);
                return new ResponseBean(ResponseCodeEnum.FAIL);
            }
        }
    }

    //list all users by createdBy
    @RequestMapping(value = "/api/user/listUsers", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean listUsers(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                  @RequestBody JSONObject params) {
        Pageable pageable = PageRequest.of(0, KnowledgeMgtService.PAGESIZE); // 创建一个分页请求
        Page<UserInfo> userList = new PageImpl<>(Collections.emptyList(), pageable, 0);
        int pageNumber = 1;
        int pageSize = SystemUserMgtService.PAGESIZE;
        if (!params.containsKey("pageNumber")) {
            pageNumber = 1;
        }
        if (!params.containsKey("pageSize")) {
            pageSize = SystemUserMgtService.PAGESIZE;
        }
        pageNumber = params.getInteger("pageNumber");
        pageSize = params.getInteger("pageSize");
        try {
            userList = this.systemUserMgtService.listAllUsers(userName, pageNumber, pageSize);
        } catch (Exception e) {
            logger.error(">>>>>>listUsers api error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, userList);
    }

    //edit user by userId
    @RequestMapping(value = "/api/user/updateUser", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean updateUser(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                   @RequestBody JSONObject params) {
        ResponseBean responseBean = new ResponseBean();
        try {
            String inputUserId = "";

            int inputType = 0;
            if (!params.containsKey("userId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            inputUserId = params.getString("userId");

            inputType = params.getInteger("inputType");

            this.systemUserMgtService.updateUser(inputUserId, inputType, userName);
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        } catch (Exception e) {
            logger.error(">>>>>>updateUser error->{}", e.getMessage(), e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }

    //delete users
    @RequestMapping(value = "/api/user/deleteUsers", method = RequestMethod.POST)
    @ResponseBody
    @MyTokenCheck
    @PrivilegeCheck
    public ResponseBean deleteUsers(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        try {
            List<String> userIdList = params.getJSONArray("userIdList").toJavaList(String.class);
            if (userIdList.size() > 0) {
                this.systemUserMgtService.deleteUsers(userName, userIdList);
            }
        } catch (Exception e) {
            logger.error(">>>>>>deleteUsers error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS);
    }

}
