package com.mkyuan.fountainbase.user.bean;

import java.io.Serializable;

public class UserLoginInfo implements Serializable {

    // 添加无参构造函数
    public UserLoginInfo() {
    }
    public UserLoginInfo(String userId, String userName, int type) {
        this.userId=userId;
        this.userName = userName;
        this.type = type;
    }
    private String userId="";
    private String userName="";
    private int type=0;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
