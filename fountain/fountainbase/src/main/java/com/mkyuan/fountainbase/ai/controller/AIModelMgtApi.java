package com.mkyuan.fountainbase.ai.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.service.AIModelService;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class AIModelMgtApi {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static int[] AI_MODEL_TYPE = {1, 2, 3, 4, 5, 6, 7,8};
    @Autowired
    private AIModelService aiModelService;

    @Value("${fountain.secretKey}")
    private String secretKey = "";

    //增加一个aimodel
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/addModel", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean addModel(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                 @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        String modelName = "";
        int type = 0;
        String url = "";
        String apiKey = "";
        try {
            if (!params.containsKey("modelName") || !params.containsKey("url") || !params.containsKey("type")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelName = params.getString("modelName");
            type = params.getInteger("type");
            final int typeToCheck = type; // 创建一个 final 变量用于 lambda 表达式
            if (!Arrays.stream(AI_MODEL_TYPE).anyMatch(x -> x == typeToCheck)) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            url = params.getString("url");
            apiKey = params.getString("modelApiKey");
            this.aiModelService.addAiModel(userName, modelName, type, url, apiKey);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "add success", 1);
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate key error collection")) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "要添加的模型类型己经存在", 101);
            } else {
                logger.error(">>>>>>add an AI model API error->{}", e.getMessage(), e);
                return new ResponseBean(ResponseCodeEnum.SUCCESS, -1);
            }
        }
    }

    //更新一个aimodel
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/updateModel", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean updateModel(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        String modelId = "";
        String modelName = "";
        String url = "";
        String apiKey = "";
        try {
            if (!params.containsKey("modelId") || !params.containsKey("modelName") || !params.containsKey("url")) {
                logger.error(">>>>>>modelId or modelName or url was null or empty, return nothing");
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "nothing changed", "nothing changed");
            }
            modelId = params.getString("modelId");
            modelName = params.getString("modelName");
            url = params.getString("url");
            apiKey = params.getString("apiKey");
            this.aiModelService.updateAiModel(userName, modelId, modelName, url, apiKey);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "update success", 1);
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate key error collection")) {
                return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "要添加的模型类型己经存在", 101);
            } else {
                logger.error(">>>>>>add an AI model API error->{}", e.getMessage(), e);
                return new ResponseBean(ResponseCodeEnum.SUCCESS, -1);
            }
        }
    }

    //删除一个aimodel
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/deleteModel", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean deleteModel(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                    @RequestBody JSONObject params) {
        ResponseBean resp = new ResponseBean();
        String modelId = "";
        try {
            if (!params.containsKey("modelId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelId = params.getString("modelId");

            this.aiModelService.deleteAiModel(userName, modelId);
            return new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "delete success", "delete success");
        } catch (Exception e) {
            logger.error(">>>>>>delete an AI model API error->{}", e.getMessage(), e);
        }
        return resp;
    }

    //根据userName获取aiModel列表
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/listMyAIModel", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean listMyAIModel(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                      @RequestBody JSONObject params) {
        List<AIModel> aiModels = new ArrayList<>();
        try {
            aiModels = this.aiModelService.getUserAIModelList(userName);
        } catch (Exception e) {
            logger.error(">>>>>>listMyAIModel API error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, aiModels);
    }

    //通过userName, modelId获取一个具体的AIModel
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/getAIModelById", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getAIModelById(@RequestHeader("token") String token, @RequestHeader("userName") String userName,
                                       @RequestBody JSONObject params) {
        AIModel aiModel = new AIModel();
        try {
            String modelId = "";
            if (!params.containsKey("modelId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelId = params.getString("modelId");
            aiModel = this.aiModelService.getAIModelByModelId(userName, modelId);
        } catch (Exception e) {
            logger.error(">>>>>>getAIModelById API error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, aiModel);
    }

    //设置AI模型路由，支持1主1备两条线路，主线路里抛出Exception相关内容后都由备线路接手重新工作
    @MyTokenCheck
    @RequestMapping(value = "/api/ai/setAIModelRoute", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean setAIModelRoute(@RequestHeader("token") String token,
                                        @RequestHeader("userName") String userName,
                                        @RequestBody JSONObject params) {
        try {
            String modelId = "";
            int routeType = 0;
            if (!params.containsKey("modelId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (!params.containsKey("routeType")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelId = params.getString("modelId");
            routeType = params.getInteger("routeType");
            int result = this.aiModelService.setAIModelRoute(userName, modelId, routeType);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
        } catch (Exception e) {
            logger.error(">>>>>>setAIModelRoute API error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, null);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/unsetAIModelRoute", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean unSetAIModelRoute(@RequestHeader("token") String token,
                                          @RequestHeader("userName") String userName,
                                          @RequestBody JSONObject params) {
        try {
            String modelId = "";
            int routeType = 0;
            if (!params.containsKey("modelId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            if (!params.containsKey("routeType")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelId = params.getString("modelId");
            routeType = params.getInteger("routeType");
            int result = this.aiModelService.unsetAIModelRoute(userName, modelId, routeType);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, result);
        } catch (Exception e) {
            logger.error(">>>>>>unSetAIModelRoute API error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, null);
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/ai/getRouteType", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getRouteTypeByModelId(@RequestHeader("token") String token,
                                              @RequestHeader("userName") String userName,
                                              @RequestBody JSONObject params) {
        try {
            String modelId = "";
            if (!params.containsKey("modelId")) {
                return new ResponseBean(ResponseCodeEnum.ILLEGAL_PARAMETERS);
            }
            modelId = params.getString("modelId");
            int type = this.aiModelService.getRouteTypeByModelId(userName, modelId);
            return new ResponseBean(ResponseCodeEnum.SUCCESS, type);
        } catch (Exception e) {
            logger.error(">>>>>>getRouteTypeByModelId API error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, 0);
    }

}
