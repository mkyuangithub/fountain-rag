package com.mkyuan.fountainbase.vectordb.bean.addpoint;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.*;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payload implements Serializable {
    private String content;
    private Date createdDate;
    private String fileName = "";
    private long fileId = 0l;

    // 移除 transient，添加 @JsonIgnore
    @JsonIgnore
    private   Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> any() {
        Map<String, Object> all = new HashMap<>(additionalProperties); // 先复制动态属性

        // 添加基本属性
        if (content != null) all.put("content", content);
        if (createdDate != null) all.put("createdDate", createdDate);
        if (fileName != null) all.put("fileName", fileName);
        all.put("fileId", fileId);

        return all;
    }

    @JsonAnySetter
    public void addProperty(String key, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(key, value);
    }
    // 添加 @JsonIgnore
    @JsonIgnore
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    // 添加一个用于调试的toString方法
    @Override
    public String toString() {
        return "Payload{" +
                "content='" + content + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileId=" + fileId +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
