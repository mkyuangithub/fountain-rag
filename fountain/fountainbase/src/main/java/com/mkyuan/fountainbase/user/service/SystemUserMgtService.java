package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.common.util.MD5Util;
import com.mkyuan.fountainbase.user.bean.UserInfo;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SystemUserMgtService {

    protected Logger logger = LogManager.getLogger(this.getClass());

    public final static int PAGESIZE=20;

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SystemUserMgtHelper systemUserMgtHelper;

    //add user
    public void addUser(String userName, String decryptPassword,int type,String createdBy)throws Exception{
        String passwordMd5= MD5Util.getMD5(decryptPassword);
        this.systemUserMgtHelper.addUser(userName,passwordMd5,type,createdBy);
    }

    //add user
    public void updateUser(String userId,int type, String createdBy)throws Exception{
        this.systemUserMgtHelper.updateUser(userId,type,createdBy);
    }


    //search user
    public Page<UserInfo> searchUser(String createdBy, int pageNumber, int pageSize,String searchUserName){
        String collectionName = "UserInfo";
        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<UserInfo> userList = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try{
            Query query = new Query();
            query.addCriteria(Criteria.where("createdBy").is(createdBy));
            if(StringUtil.isNotBlank(searchUserName)) {
                query.addCriteria(Criteria.where("userName").regex(".*" + searchUserName + ".*", "i"));
            }
            // 检查 queryDate 是否不为空

            query.with(Sort.by(Sort.Direction.DESC, "updatedDate"));
            long count = mongoTemplate.count(query, UserInfo.class, collectionName);
            List<UserInfo> users = mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)), UserInfo.class, collectionName);
            return new PageImpl<>(users, PageRequest.of(pageNumber - 1, pageSize), count);
        }catch(Exception e){
            logger.error(">>>>>>listAllUsers service error->{}",e.getMessage(),e);
        }
        return userList;
    }

    //list user
    public Page<UserInfo> listAllUsers(String createdBy, int pageNumber, int pageSize){
        String collectionName = "UserInfo";
        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<UserInfo> userList = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try{
            Query query = new Query();
            query.addCriteria(Criteria.where("createdBy").is(createdBy));
            //query.addCriteria(Criteria.where("type").is(type));

            // 检查 queryDate 是否不为空

            query.with(Sort.by(Sort.Direction.DESC, "updatedDate"));
            long count = mongoTemplate.count(query, UserInfo.class, collectionName);
            List<UserInfo> users = mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)), UserInfo.class, collectionName);
            return new PageImpl<>(users, PageRequest.of(pageNumber - 1, pageSize), count);
        }catch(Exception e){
            logger.error(">>>>>>listAllUsers service error->{}",e.getMessage(),e);
        }
        return userList;
    }

    //deleteUsers
    public void deleteUsers(String userName, List<String>ids)throws Exception{
        this.systemUserMgtHelper.deleteUser(userName,ids);
    }
}
