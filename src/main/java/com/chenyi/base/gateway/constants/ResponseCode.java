package com.chenyi.base.gateway.constants;

public enum ResponseCode {

    SUCCESS("success", 200),

    ERROR("Server Error", 500),

    USER_NOT_LOGIN("用户未登录", 401),

    FORBIDDEN("无访问权限", 403),

    PARAM_EMPTY("必要参数为空", 1000),

    PARAM_ERROR("参数错误", 1001);

    private String msg;

    private int status;

    ResponseCode(String msg, int status) {
        this.msg = msg;
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public int getStatus() {
        return status;
    }

}
