package com.mkyuan.fountainbase.demo.smartpurchase.controller;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.demo.smartpurchase.bean.DemoGoods;
import com.mkyuan.fountainbase.demo.smartpurchase.service.ProductService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DemoProductAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private ProductService productService;

    @MyTokenCheck
    @RequestMapping(value = "/api/demo/getGoodsByIdList", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean getGoodsByIdList(@RequestHeader("token") String token,
                                         @RequestHeader("userName") String userName,
                                         @RequestBody JSONObject params) {
        List<String> dataIds = params.getJSONArray("dataIds").toJavaList(String.class);
        List<DemoGoods> productList = new ArrayList<>();
        try {
            productList = this.productService.getGoodsByIdList(dataIds);
        } catch (Exception e) {
            logger.error(">>>>>>getGoodsByIdList error->{}", e.getMessage(), e);
        }
        return new ResponseBean(ResponseCodeEnum.SUCCESS, productList);
    }
}
