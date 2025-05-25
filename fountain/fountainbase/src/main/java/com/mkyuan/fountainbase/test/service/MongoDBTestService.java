package com.mkyuan.fountainbase.test.service;

import com.mkyuan.fountainbase.test.bean.MongoTestBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MongoDBTestService {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;


    @Transactional(rollbackFor = Exception.class)
    public String addStudent(String name)throws Exception{
        // 确保在事务中执行
        return TransactionSynchronizationManager.isActualTransactionActive() ?
                doAddStudent(name) :
                mongoTemplate.execute(action -> {
                    try {
                        return doAddStudent(name);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private String doAddStudent(String name) throws Exception {
        String collectionName = "MongoTest";
        MongoTestBean mongoTestBean = new MongoTestBean();
        mongoTestBean.setStudentName(name);
        MongoTestBean resultBean = mongoTemplate.save(mongoTestBean, collectionName);

        if (name.equals("abc")) {
            throw new Exception(">>>>>>throw exception to rollback");
        }

        return resultBean.getId();
    }

}
