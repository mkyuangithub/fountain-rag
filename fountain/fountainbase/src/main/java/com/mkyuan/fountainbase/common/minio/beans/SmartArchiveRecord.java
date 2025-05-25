package com.mkyuan.fountainbase.common.minio.beans;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class SmartArchiveRecord {

	public static final String COLLECTION_NAME = "FountainArchiveRecord";

	@Id
	private Long id;

	private Long size;

	private Boolean auth;

	private String user;
	private String code;
	private String type;
	private String path;
	private String channel;
	private String group;
	private String oldName;
	private String newName;
	private String contentType;

	private Date dataCreatedTime;
	private Date dataUpdatedTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Boolean getAuth() {
		return auth;
	}

	public void setAuth(Boolean auth) {
		this.auth = auth;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Date getDataCreatedTime() {
		return dataCreatedTime;
	}

	public void setDataCreatedTime(Date dataCreatedTime) {
		this.dataCreatedTime = dataCreatedTime;
	}

	public Date getDataUpdatedTime() {
		return dataUpdatedTime;
	}

	public void setDataUpdatedTime(Date dataUpdatedTime) {
		this.dataUpdatedTime = dataUpdatedTime;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}