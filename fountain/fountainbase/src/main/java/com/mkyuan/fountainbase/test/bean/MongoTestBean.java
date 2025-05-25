package com.mkyuan.fountainbase.test.bean;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class MongoTestBean {
    private String id;
    private String studentName="";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
