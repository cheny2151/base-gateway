package com.shopline.ai.kf.gateway.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * session用户信息
 *
 * @author by chenyi
 * @date 2022/5/27
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = -1L;

    private long id;

    private String name;

    private String tenant;

    private List<String> roles;

}
