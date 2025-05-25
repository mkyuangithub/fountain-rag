package com.mkyuan.fountainbase.common.minio.storage.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import com.mkyuan.fountainbase.common.minio.storage.RecordStorage;
@Component("RecordStorage_minio")
public class RecordStorageForMinIO implements RecordStorage {

	protected Logger logger = LogManager.getLogger(this.getClass());

	@Value("${fountain.minio.bucket-name}")
	private String bucketName;

	@Autowired
	private MinioClient minioClient;

	@Override
	public String upload(String group, String fileName, String contentType, byte[] fileData) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String filePath = null;
		if (StringUtils.isBlank(group)) {
			filePath = String.format("/%s/%s", sdf.format(new Date()), fileName);
		} else {
			filePath = String.format("/%s/%s/%s", group, sdf.format(new Date()), fileName);
		}
		logger.info(">>>>>>fileName->{}, filePath->{}",fileName,filePath);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData)) {
			PutObjectArgs args = PutObjectArgs //
					.builder() //
					.bucket(this.bucketName) //
					.object(filePath) //
					.stream(bais, fileData.length, -1) //
					.contentType(contentType) //
					.build(); //
			this.minioClient.putObject(args);
			return filePath;
		} catch (Exception e) {
			this.logger.error(">>>>>>文件上传失败,原因:{}", e.getMessage(), e);
			throw new IOException("文件上传失败");
		}
	}

	@Override
	public byte[] download(String filePath) throws IOException {
		GetObjectArgs args = GetObjectArgs.builder().bucket(this.bucketName).object(filePath).build();
		try (GetObjectResponse response = this.minioClient.getObject(args)) {
			return IOUtils.toByteArray(response);
		} catch (Exception e) {
			this.logger.error(">>>>>>文件下载失败,原因:{}", e.getMessage(), e);
			throw new IOException("文件下载失败");
		}
	}

	@Override
	public void delete(String filePath) throws IOException {
		RemoveObjectArgs remove = RemoveObjectArgs.builder().bucket(this.bucketName).object(filePath).build();
		try {
			this.minioClient.removeObject(remove);
		} catch (Exception e) {
			this.logger.error(">>>>>>文件删除失败,原因:{}", e.getMessage(), e);
			throw new IOException("文件删除失败");
		}
	}

}