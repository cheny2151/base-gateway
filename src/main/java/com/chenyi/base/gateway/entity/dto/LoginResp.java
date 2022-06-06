package com.chenyi.base.gateway.entity.dto;

import com.chenyi.base.gateway.entity.User;
import lombok.Data;

/**
 * @author by chenyi
 * @date 2022/6/1
 */
@Data
public class LoginResp {

    private Integer code;

    private User user;

}
