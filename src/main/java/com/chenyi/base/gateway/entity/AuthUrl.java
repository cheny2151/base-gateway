package com.chenyi.base.gateway.entity;

import lombok.Data;

/**
 * 鉴权url配置
 *
 * @author by chenyi
 * @date 2022/5/31
 */
@Data
public class AuthUrl {

    private String code;
    private String url;
    private int isAuth;
    private String authCode;
    private String method;

    public AuthUrl(String code, String url, String authCode, int isAuth, String method) {
        this.code = code;
        this.url = url;
        this.authCode = authCode;
        this.isAuth = isAuth;
        this.method = method;
    }

    public boolean isAuth() {
        return isAuth == 1;
    }

}
