package com.mkyuan.fountainbase.ai.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.ai.bean.AIFunctionals;
import com.mkyuan.fountainbase.ai.service.AIFunctionService;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
public class AIFunctionApi {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private AIFunctionService aiFunctionService;
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/function/list", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean listAIFunction(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                 @RequestBody JSONObject params) {
        //创建一个初始化的分页对象
        Pageable pageable = PageRequest.of(0, AIFunctionService.PAGESIZE); // 创建一个分页请求
        Page<AIFunctionals> functionList = new PageImpl<>(Collections.emptyList(), pageable, 0);
        try {
            int pageNumber=0;
            int pageSize=AIFunctionService.PAGESIZE;
            if(params.containsKey("pageNumber")){
                pageNumber=params.getInteger("pageNumber");
            }
            if(params.containsKey("pageSize")){
                pageSize=params.getInteger("pageSize");
            }
            functionList=this.aiFunctionService.getPagedAIFunctionList(pageNumber,pageSize);
        } catch (Exception e) {
            logger.error(">>>>>>display all ai function list error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, functionList);
    }

    //更新一个AIFunction
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/function/updateFunction", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean updateFunction(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                       @RequestBody JSONObject params) {
        try{
            String id="";
            int code=0;
            String description="";
            String prompt="";
            String returnTemplate="";
            if(!params.containsKey("code")||!params.containsKey("id")){
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            id=params.getString("id");
            code=params.getInteger("code");
            description=params.getString("description");
            prompt=params.getString("prompt");
            returnTemplate=params.getString("returnTemplate");
            AIFunctionals inputAIFunctionals=new AIFunctionals();
            inputAIFunctionals.setId(id);
            inputAIFunctionals.setCode(code);
            inputAIFunctionals.setDescription(description);
            inputAIFunctionals.setPrompt(prompt);
            inputAIFunctionals.setReturnTemplate(returnTemplate);
            this.aiFunctionService.updateFunction(userName,inputAIFunctionals);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(),"update success",1);
        }catch(Exception e){
            logger.error(">>>>>>updateFunction api error->{}",e.getMessage(),e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }

    //增加一个AIFunction
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/function/addFunction", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean addFunction(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                       @RequestBody JSONObject params) {
        try {
            int code=0;
            String description="";
            String prompt="";
            String returnTemplate="";
            if(!params.containsKey("code")||!params.containsKey("prompt")||!params.containsKey("description")){
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            code=params.getInteger("code");
            description=params.getString("description");
            prompt=params.getString("prompt");
            returnTemplate=params.getString("returnTemplate");
            AIFunctionals inputAIFunctionals=new AIFunctionals();
            inputAIFunctionals.setCode(code);
            inputAIFunctionals.setDescription(description);
            inputAIFunctionals.setPrompt(prompt);
            inputAIFunctionals.setReturnTemplate(returnTemplate);
            this.aiFunctionService.addfunction(userName,inputAIFunctionals);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(),"add success",1);
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate key error collection")) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "要添加的ai function code己经存在", 101);
            }else {
                logger.error(">>>>>>addFunction api error->{}", e.getMessage(), e);
                return new ResponseBean(ResponseCodeEnum.FAIL);
            }
        }
    }
    //删除选中的条目
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/function/deleteFunction", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean deleteFunction(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        try{
            List<String> functionIdList = params.getJSONArray("functionIdList")
                                                .toJavaList(String.class);
            if(functionIdList.size()>0){
                this.aiFunctionService.deletefunction(userName,functionIdList);
            }
            return new ResponseBean(ResponseCodeEnum.SUCCESS);
        }catch(Exception e){
            logger.error(">>>>>>删除失败->{}",e.getMessage(),e);
            return new ResponseBean(ResponseCodeEnum.FAIL);
        }
    }
}
