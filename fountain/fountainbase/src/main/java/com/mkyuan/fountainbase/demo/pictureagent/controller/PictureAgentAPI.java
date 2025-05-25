package com.mkyuan.fountainbase.demo.pictureagent.controller;

import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.demo.pictureagent.bean.PicBase64RequestPayload;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
public class PictureAgentAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());
    private final static String removeBGUrl="http://localhost:9999/api/v1/run_plugin_gen_image";
    private final static String removeWatermarkUrl="http://localhost:9999/api/v1/inpaint";

    @Autowired
    private RestTemplate restTemplate;

    @MyTokenCheck
    @RequestMapping(value = "/api/demo/pictureAgent/removeWatermark", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public String removeWatermark(@RequestHeader("token") String token, @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        String image = params.getString("image");
        String mask = params.getString("mask");
        try {
            if (StringUtils.isNotBlank(mask) && StringUtils.isNotBlank(image)) {

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // headers.set("Authorization", "Bearer YOUR_API_KEY");

                HttpEntity<JSONObject> requestEntity = new HttpEntity<>(params, headers);

                // 使用byte[]接收图片数据
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        removeWatermarkUrl,
                        HttpMethod.POST,
                        requestEntity,
                        byte[].class
                );

                // 创建新的响应头，设置为图片类型
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.IMAGE_PNG);
                responseHeaders.setContentLength(response.getBody().length);
                String returnImagePrefix = "data:image/png;base64,";
                String returnImage = returnImagePrefix + Base64.getEncoder().encodeToString(response.getBody());
                // 返回图片数据
                logger.info(">>>>>>返回的Response->\n{}", returnImage);
                return returnImage;
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    @MyTokenCheck
    @RequestMapping(value = "/api/demo/pictureAgent/removeBG", method = RequestMethod.POST)
    @ResponseBody
    @PrivilegeCheck
    public String removeBG(@RequestHeader("token") String token, @RequestHeader("userName") String userName, @RequestBody JSONObject params) {
        PicBase64RequestPayload picBase64RequestPayload = new PicBase64RequestPayload();
        String name = params.getString("name");
        String image = params.getString("image");
        try {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(image)) {
                picBase64RequestPayload.setName(name);
                picBase64RequestPayload.setImage(image);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                // headers.set("Authorization", "Bearer YOUR_API_KEY");

                HttpEntity<PicBase64RequestPayload> requestEntity = new HttpEntity<>(picBase64RequestPayload, headers);

                // 使用byte[]接收图片数据
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        removeBGUrl,
                        HttpMethod.POST,
                        requestEntity,
                        byte[].class
                );

                // 创建新的响应头，设置为图片类型
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.IMAGE_PNG);
                responseHeaders.setContentLength(response.getBody().length);
                String returnImagePrefix = "data:image/png;base64,";
                String returnImage = returnImagePrefix + Base64.getEncoder().encodeToString(response.getBody());
                // 返回图片数据
                return returnImage;
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

}
