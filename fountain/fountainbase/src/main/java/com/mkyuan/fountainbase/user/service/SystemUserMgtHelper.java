package com.mkyuan.fountainbase.user.service;

import com.mkyuan.fountainbase.user.bean.UserInfo;
import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class SystemUserMgtHelper {

    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    //add user
    public void addUser(String userName, String passwordMD5, int type, String createdBy) throws Exception {
        doAddUser(userName, passwordMD5, type, createdBy);
    }

    private void doAddUser(String userName, String passwordMD5, int type, String createdBy) throws Exception {
        String userCollectionName = "UserInfo";
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(userName);
        userInfo.setPassword(passwordMD5);
        userInfo.setType(type);
        userInfo.setCreatedBy(createdBy);
        mongoTemplate.save(userInfo, userCollectionName);
    }

    //update user

    public void updateUser(String userId, int type, String createdBy) throws Exception {
        doUpdateUser(userId, type, createdBy);
    }

    private void doUpdateUser(String userId, int type, String createdBy) throws Exception {
        String userCollectionName = "UserInfo";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(userId)
                                  .and("createdBy").is(createdBy).and("userName").ne("admin"));
        // 创建更新对象
        Update update = new Update();
        update.set("type", type)
              .set("updatedDate", new Date());

        // 执行更新操作
        mongoTemplate.updateFirst(query, update, userCollectionName);

    }

    //deleteUser

    public void deleteUser(String userName, List<String> ids)throws Exception{
        doDeleteUsers(userName,ids);

    }
    private void doDeleteUsers(String userName, List<String> ids){
        String collectionName = "UserInfo";
        Query query = new Query(Criteria.where("_id").in(ids).and("createdBy").is(userName));

        DeleteResult result = mongoTemplate.remove(query, collectionName);

        if (result.getDeletedCount() != ids.size()) {
            logger.info(">>>>>>successful delete UserInfo, the expected to delete {} documents but actually deleted {}",
                        ids.size(), result.getDeletedCount());
        }
    }

}
