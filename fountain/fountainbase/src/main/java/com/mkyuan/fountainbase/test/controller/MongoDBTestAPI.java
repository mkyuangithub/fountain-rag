package com.mkyuan.fountainbase.test.controller;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.test.service.MongoDBTestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class MongoDBTestAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoDBTestService mongoDBTestService;

    @RequestMapping(value = "/api/test/mongoRollBack", method = RequestMethod.POST)
    @ResponseBody
    public ResponseBean testMongoRollBack(@RequestBody JSONObject params){
        ResponseBean resp=new ResponseBean();
        try{
            String studentName=params.getString("studentName");
            String newId=this.mongoDBTestService.addStudent(studentName);
            resp=new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(),"success",newId);
        }catch(Exception e){
            logger.error(">>>>>>testMongoRollBack API error->{}",e.getMessage(),e);
        }
        return resp;
    }
}
