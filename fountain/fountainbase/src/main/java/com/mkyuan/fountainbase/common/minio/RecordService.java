package com.mkyuan.fountainbase.common.minio;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.mkyuan.fountainbase.common.util.RandomUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;

import com.mkyuan.fountainbase.common.minio.beans.SmartArchiveRecord;
import com.mkyuan.fountainbase.common.minio.storage.RecordStorage;
import com.mkyuan.fountainbase.common.minio.storage.RecordStorageChannel;
import com.mkyuan.fountainbase.common.minio.storage.RecordStorageFactory;
import com.mkyuan.fountainbase.common.minio.storage.Radix62Toolkit;
import com.mkyuan.fountainbase.common.minio.storage.Radix62Toolkit.RadixLong;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
public class RecordService {

	private static final int RECORD_EXPIRE = 1;// 缓存时间1,小时
	private static final String RECORD_CACHE = "fountain:minio:record";
	protected Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private RecordStorageFactory recordStorageFactory;

	public String delete(String code) throws IOException {
		if (StringUtils.isBlank(code)) {
			throw new IllegalArgumentException("参数有误");
		}
		RadixLong value = Radix62Toolkit.decode(code);

		String collectionName = SmartArchiveRecord.COLLECTION_NAME;
		SmartArchiveRecord record = this.mongoTemplate.findById(value.getValue(), SmartArchiveRecord.class, collectionName);
		if (null == record) {
			throw new IllegalArgumentException("资源不存在");
		}

		RecordStorageChannel channel = this.recordStorageFactory.getDefaultChannel();
		RecordStorage recordStorage = this.recordStorageFactory.getStorage(channel);
		recordStorage.delete(record.getPath());
		// 删除数据库数据
		this.mongoTemplate.remove(new Query(Criteria.where("_id").is(record.getId())), SmartArchiveRecord.class, collectionName);

		return code;
	}

	public Tuple2<SmartArchiveRecord, byte[]> find(String code) throws IOException {
		if (StringUtils.isBlank(code)) {
			throw new IllegalArgumentException("参数有误");
		}
		String key = String.format("%s:%s", RECORD_CACHE, code);
		String cache = (String) this.redisTemplate.opsForValue().get(key);
		RadixLong value = Radix62Toolkit.decode(code);

		if (StringUtils.isNotBlank(cache)) {
			SmartArchiveRecord record = JSON.parseObject(cache, SmartArchiveRecord.class);
			byte[] data = this.getRecordBytes(value.getCompany(), record);
			return Tuples.of(record, data);
		}

		String collectionName = SmartArchiveRecord.COLLECTION_NAME;
		SmartArchiveRecord record = this.mongoTemplate.findById(value.getValue(), SmartArchiveRecord.class, collectionName);
		if (null == record) {
			throw new IllegalArgumentException("资源不存在");
		}

		this.redisTemplate.opsForValue().set(key, JSON.toJSONString(record), RECORD_EXPIRE, TimeUnit.HOURS);
		byte[] data = this.getRecordBytes(value.getCompany(), record);
		return Tuples.of(record, data);
	}

	public String save(String user, String group, MultipartFile file, boolean auth) throws IOException {
		if (null == file) {
			throw new IllegalArgumentException("请上传文件");
		}
		return this.save(user, group, file.getOriginalFilename(), file.getContentType(), file.getBytes(), auth);
	}

	public String saveImage(String user, String group, String fileName, String fileType, byte[] fileData, boolean auth) throws IOException {
		return this.save(user, group, fileName, fileType, fileData, auth);
	}

	public String save(String user, String group, String fileName, String fileType, byte[] fileData, boolean auth) throws IOException {

		Date current = new Date();
		long id=0l;
		id= RandomUtil.getRandomShortLong();

		byte[] data = fileData;
		long size = data.length;
		String code = Radix62Toolkit.encode(id);
		String name = fileName;
		String type = FilenameUtils.getExtension(name);
		logger.info(">>>>>>original upload fileName->{} and fileType->",fileName,fileType);
		String newName = String.format("%s.%s", this.generatorNewName(), type);

		RecordStorageChannel channel = this.recordStorageFactory.getDefaultChannel();
		RecordStorage recordStorage = this.recordStorageFactory.getStorage(channel);
		String path = recordStorage.upload(group, newName, fileType, data);

		SmartArchiveRecord record = new SmartArchiveRecord();
		record.setId(id);
		record.setUser(user);
		record.setAuth(auth);
		record.setCode(code);
		record.setType(type);
		record.setSize(size);
		record.setPath(path);
		record.setChannel(channel.getValue());
		record.setGroup(group);
		record.setOldName(name);
		record.setNewName(newName);
		record.setContentType(fileType);
		record.setDataCreatedTime(current);
		record.setDataUpdatedTime(current);

		String collectionName = SmartArchiveRecord.COLLECTION_NAME;
		this.mongoTemplate.save(record, collectionName);
		return code;
	}

	/**
	 * 分页查询文档信息
	 */
	public Page<SmartArchiveRecord> query(String group, JSONObject params) {
		String collectionName = SmartArchiveRecord.COLLECTION_NAME;

		Integer pageNumber = params.getInteger("pageNumber");
		Integer pageSize = params.getInteger("pageSize");

		if (null == pageNumber) {
			pageNumber = 1;
		}
		if (null == pageSize) {
			pageSize = 10;
		}

		Query query = new Query();
		query.addCriteria(Criteria.where("group").is(group));
		query.with(Sort.by(Sort.Direction.DESC, "dataCreatedTime"));
		query.fields().include("code").include("oldName").exclude("id");

		try {
			long count = this.mongoTemplate.count(query, SmartArchiveRecord.class, collectionName);
			List<SmartArchiveRecord> recordList = this.mongoTemplate.find(query.with(PageRequest.of(pageNumber - 1, pageSize)), SmartArchiveRecord.class, collectionName);
			return new PageImpl<>(recordList, PageRequest.of(pageNumber - 1, pageSize), count);
		} catch (Exception e) {
			this.logger.error(">>>>>>查询mongo文档数据出错,原因:{}", e.getMessage(), e);
		}
		return new PageImpl<>(new ArrayList<>(), PageRequest.of(0, pageSize), 0);
	}

	private byte[] getRecordBytes(int companyId, SmartArchiveRecord record) throws IOException {
		RecordStorageChannel channel = RecordStorageChannel.parseByValue(record.getChannel());
		RecordStorage recordStorage = this.recordStorageFactory.getStorage(channel);
		return recordStorage.download(record.getPath());
	}

	private String generatorNewName() {
		return UUID.randomUUID().toString().replace("-", "").toLowerCase();
	}


}