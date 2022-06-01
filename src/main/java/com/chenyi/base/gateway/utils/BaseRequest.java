package com.chenyi.base.gateway.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 基础请求协议包装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRequest<T> implements Serializable {

    private static final long serialVersionUID = 8744687666253393218L;

    /**
     * 请求标识
     */
    private String requestId;

    /**
     * 请求时间戳
     */
    private String timestamp;

    /**
     * 请求参数
     */
    private T data;

}
