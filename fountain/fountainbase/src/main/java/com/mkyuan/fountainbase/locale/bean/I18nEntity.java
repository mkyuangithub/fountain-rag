package com.mkyuan.fountainbase.locale.bean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "i18n")
public class I18nEntity {
    @Id
    private String id;

    private String key;

    private String zhValue;

    private String enValue;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getZhValue() {
        return zhValue;
    }

    public void setZhValue(String zhValue) {
        this.zhValue = zhValue;
    }

    public String getEnValue() {
        return enValue;
    }

    public void setEnValue(String enValue) {
        this.enValue = enValue;
    }
// getters and setters
}