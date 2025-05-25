package com.mkyuan.fountainbase.common.controller.response;

public enum ResponseCodeEnum {
    SUCCESS(0, "成功"), FAIL(-1, "系统错误"), LOGIN_ERROR(1000, "用户名或密码错误"),
    INVALID_MOBILE(1001, "无效的手机号"), INVALID_UT(1002, "无效的ut"),
    LOGOUT_ERROR(1003, "登出系统异常"), NOPRIVILEGE_ERROR(1004, "用户没有此操作权限"),
    ILLEGAL_PARAMETERS(2001, "非法参数");


    private int code;
    private String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
