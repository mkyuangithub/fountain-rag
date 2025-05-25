package com.mkyuan.fountainbase.Upload.controller;

import com.mkyuan.fountainbase.Upload.bean.UploadBean;
import com.mkyuan.fountainbase.Upload.service.UploadService;
import com.mkyuan.fountainbase.aop.MyTokenCheck;
import com.mkyuan.fountainbase.aop.PrivilegeCheck;
import com.mkyuan.fountainbase.common.controller.response.ResponseBean;
import com.mkyuan.fountainbase.common.controller.response.ResponseCodeEnum;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.common.minio.beans.SmartArchiveRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.util.function.Tuple2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@RestController
public class UploadAPI {
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private UploadService uploadService;

    @Autowired
    private RecordService recordService;

    @MyTokenCheck
    @RequestMapping(value = "/api/tool/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @PrivilegeCheck
    public ResponseBean upload(@RequestParam("file") MultipartFile file, @RequestParam("isPic") boolean isPic,
                               @RequestParam("uploadType") int uploadType,
                               @RequestHeader("token") String token, @RequestHeader("userName") String userName) {
        ResponseBean resp = new ResponseBean();
        try {
            logger.info(">>>>>>the upload file is fileName->{} and fileType->{}", file.getOriginalFilename(),
                        file.getContentType());
            UploadBean uploadBean = this.uploadService.uploadFile(userName, file, uploadType, isPic);
            resp = new ResponseBean(ResponseCodeEnum.SUCCESS.getCode(), "upload success", uploadBean);
        } catch (Exception e) {
            logger.error(">>>>>>upload error->{}", e.getMessage(), e);
            resp = new ResponseBean(ResponseCodeEnum.FAIL);
        }
        return resp;
    }

    @GetMapping("/api/tool/viewUploadedFile/{code}")
    public void image(@PathVariable("code") String code,  HttpServletResponse response) {
        try (OutputStream os = response.getOutputStream()) {
            Tuple2<SmartArchiveRecord, byte[]> tuple = this.recordService.find(code);
            SmartArchiveRecord record = tuple.getT1();
            byte[] data = tuple.getT2();

            response.setHeader("Content-Type", record.getContentType());
            response.setHeader("Content-Length", String.valueOf(data.length));
            os.write(data);
        } catch (IOException e) {
            this.logger.error(">>>>>>文件预览失败,原因:{}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "resource not found");
            } catch (IOException e1) {
                this.logger.error(">>>>>>响应404错误,原因:{}", e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error(">>>>>>根据code查看上传的文件出错->{}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "service error");
            } catch (IOException e1) {
                this.logger.error(">>>>>>响应500错误,原因:{}", e.getMessage(), e);
            }
        }
    }
}
