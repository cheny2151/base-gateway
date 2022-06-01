package com.chenyi.base.gateway.utils;

import com.chenyi.base.gateway.entity.AuthUrl;
import lombok.Data;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 鉴权url匹配，继承{@link PathPattern}
 *
 * @author by chenyi
 * @date 2022/5/31
 */
@Data
public class AuthPathPattern {

    private PathPattern pathPattern;

    private AuthUrl authUrl;

    public AuthPathPattern(PathPattern pathPattern, AuthUrl authUrl) {
        this.pathPattern = pathPattern;
        this.authUrl = authUrl;
    }
}
