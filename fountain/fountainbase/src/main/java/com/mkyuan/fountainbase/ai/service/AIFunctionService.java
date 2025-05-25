package com.mkyuan.fountainbase.ai.service;

import com.mkyuan.fountainbase.ai.bean.AIFunctionals;
import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;

@Service
public class AIFunctionService {
    protected Logger logger = LogManager.getLogger(this.getClass());
    public final static int PAGESIZE = 20;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Page<AIFunctionals> getPagedAIFunctionList(int pageNumber, int pageSize) {
        String collectionName = "AIFunctionals";
        Pageable pageable = PageRequest.of(0, PAGESIZE); // 创建一个分页请求
        Page<AIFunctionals> functionList = new PageImpl<>(Collections.emptyList(), pageable, PAGESIZE);
        try {
            Query query = new Query();
            query.with(Sort.by(Sort.Direction.ASC, "code"));
            long count = mongoTemplate.count(query, AIFunctionals.class, collectionName);
            List<AIFunctionals> functions = mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)),
                                                               AIFunctionals.class, collectionName);
            return new PageImpl<>(functions, PageRequest.of(pageNumber - 1, pageSize), count);
        } catch (Exception e) {
            logger.error(">>>>>>get ai function list error->{}", e.getMessage(), e);
        }
        return functionList;
    }

    //编辑更新一个AIFunction
    @Transactional(rollbackFor = Exception.class)
    public void updateFunction(String userName, AIFunctionals aiFunctionals) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            doUpdateFunction(userName, aiFunctionals);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    doUpdateFunction(userName, aiFunctionals);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>updateFunction error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
    private void doUpdateFunction(String userName, AIFunctionals aiFunctionals) throws Exception {
        String collectionName = "AIFunctionals";
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(aiFunctionals.getId())
                                  .and("code").is(aiFunctionals.getCode()));
        // 创建更新对象
        Update update = new Update();
        update.set("prompt", aiFunctionals.getPrompt())
              .set("description", aiFunctionals.getDescription())
              .set("returnTemplate", aiFunctionals.getReturnTemplate());

        // 执行更新操作
        mongoTemplate.updateFirst(query, update, collectionName);

    }

    //新增一个AIFunction

    @Transactional(rollbackFor = Exception.class)
    public void addfunction(String userName, AIFunctionals aiFunctionals) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            doAddfunctionFunction(userName, aiFunctionals);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    doAddfunctionFunction(userName, aiFunctionals);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>addfunction error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
    private void doAddfunctionFunction(String userName, AIFunctionals aiFunctionals) throws Exception {
        String collectionName = "AIFunctionals";
        // 执行更新操作
        mongoTemplate.save(aiFunctionals,collectionName);

    }

    //删除选中的function
    @Transactional(rollbackFor = Exception.class)
    public void deletefunction(String userName, List<String> selectedIds) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            doDeleteFunction(userName,selectedIds);
        } else {
            mongoTemplate.execute(action -> {
                try {
                    doDeleteFunction(userName,selectedIds);
                    return null; // mongoTemplate.execute 需要返回值
                } catch (Exception e) {
                    logger.error(">>>>>>deletefunction error->{}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
    private void doDeleteFunction(String userName, List<String>ids){
        String collectionName = "AIFunctionals";
        Query query = new Query(Criteria.where("_id").in(ids));

        DeleteResult result = mongoTemplate.remove(query, collectionName);

        if (result.getDeletedCount() != ids.size()) {
            logger.info(">>>>>>successful deleteFunction, the expected to delete {} documents but actually deleted {}",
                     ids.size(), result.getDeletedCount());
        }
    }
}
