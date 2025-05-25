package com.mkyuan.fountainbase.ai;

import com.mkyuan.fountainbase.ai.bean.AIFunctionals;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class AIFunctionHelper {

    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;
    public String getFunctions(int code, String prompt){
        String result="";
        String collectionName="AIFunctionals";
        try{
            Query query = new Query();
            query.addCriteria(Criteria.where("code").is(code));
            AIFunctionals aiFunctionals=mongoTemplate.findOne(query,AIFunctionals.class,collectionName);
            if(aiFunctionals!=null){
                result=aiFunctionals.getPrompt().replace("$<prompt>",prompt);
                if(StringUtil.isNotBlank(aiFunctionals.getReturnTemplate())){
                    result=result.replace("$<returnTemplate>",aiFunctionals.getReturnTemplate());
                }
            }
        }catch(Exception e){
            logger.error(">>>>>>get AIFunctionals by code->{} error: {}",code,e.getMessage(),e);
        }
        return result;
    }
}
