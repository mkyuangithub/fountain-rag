package com.mkyuan.fountainbase.demo.smartpurchase.service;

import com.mkyuan.fountainbase.demo.smartpurchase.bean.DemoGoods;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<DemoGoods> getGoodsByIdList(List<String> dataIds) {
        String collectionName = "DemoGoods";
        if (dataIds == null || dataIds.isEmpty()) {
            return new ArrayList<>();
        }

        Query query = new Query();
        // 方案1: 将字符串ID转换为数值类型
        List<Number> numericIds = new ArrayList<>();
        for (String id : dataIds) {
            try {
                // 根据实际情况选择转换为Integer或Long
                numericIds.add(Integer.valueOf(id));
            } catch (NumberFormatException e) {
                // 如果转换失败，记录日志但继续处理其他ID
                logger.error(">>>>>>convert to number exception: " + id);
            }
        }

        if (!numericIds.isEmpty()) {
            query.addCriteria(Criteria.where("productCode").in(numericIds));
        } else {
            return new ArrayList<>(); // 如果没有有效ID，返回空列表
        }

        return mongoTemplate.find(query, DemoGoods.class, collectionName);

    }
}
