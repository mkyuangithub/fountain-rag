package com.mkyuan.fountainbase.Upload.service;

import com.mkyuan.fountainbase.Upload.bean.UploadBean;
import com.mkyuan.fountainbase.common.minio.RecordService;
import com.mkyuan.fountainbase.common.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import javax.imageio.ImageIO;
@Service
public class UploadService {

    protected Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RecordService recordService;


    public UploadBean uploadFile(String userName, MultipartFile file,int uploadType,boolean isPic)throws Exception{
        UploadBean uploadBean=new UploadBean();
        String uploadCode="";
        String shortCode="";
        String picName= RandomUtil.getRandomShortLong()+".png";
        uploadCode=this.recordService.save(userName, null, picName,file.getContentType() , file.getBytes(), true);

        if(isPic){
            shortCode=this.getShortPicFromUpload(userName,file.getBytes());
        }
        if(StringUtils.isNotBlank(uploadCode)){
            this.storeFileIntoMongo(userName,uploadCode,isPic,shortCode,uploadType);
            uploadBean.setCode(uploadCode);
            uploadBean.setShortCode(shortCode);
            return uploadBean;
        }else{
            throw new Exception(">>>>>>upload file error uploadCode is null");
        }
    }

    private String getShortPicFromUpload(String userName, byte[] imageBytes) throws Exception {
        String shortPicCode = "";
        try {


            String shortPicName= RandomUtil.getRandomShortLong()+".png";
            //生成图片缩略图
            byte[] shortPicBytes = this.resizeImage(imageBytes, 300);
            shortPicCode=this.recordService.save(userName, null, shortPicName,"image/png" , shortPicBytes, true);
            logger.info(">>>>>> upload shortPicture\n返回值->{}", shortPicCode);

        } catch (Exception e) {
            throw new Exception(">>>>>>上传至smart archive出错->" + e.getMessage(), e);
        }
        return shortPicCode;
    }

    private byte[] resizeImage(byte[] imageData, int targetWidth) throws IOException {
        ByteArrayInputStream bis = null;
        ByteArrayOutputStream bos = null;
        try {
            bis = new ByteArrayInputStream(imageData);
            BufferedImage originalImage = ImageIO.read(bis);

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 如果原图已经小于目标宽度，则不进行缩放
            if (originalWidth <= targetWidth) {
                return imageData;
            }

            // 计算缩放比例
            double scale = (double) targetWidth / originalWidth;
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            // 创建缩放后的图片
            BufferedImage resizedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            } finally {
                g2d.dispose();
            }

            // 将缩放后的图片转换回byte数组
            bos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", bos);
            return bos.toByteArray();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    // 记录异常，但不抛出
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public String storeFileIntoMongo(String userName, String code, boolean isPic,String shortCode,int uploadType){
        String collection="UploadedFiles";
        UploadBean uploadBean=new UploadBean();
        uploadBean.setUserId(userName);
        uploadBean.setCode(code);
        if(isPic){
            uploadBean.setShortCode(shortCode);
        }
        uploadBean.setType(uploadType);
        uploadBean.setCreatedDate(new Date());
        uploadBean.setUpdatedDate(new Date());
        UploadBean insertRecord=mongoTemplate.save(uploadBean,collection);
        return insertRecord.getId();
    }

}
