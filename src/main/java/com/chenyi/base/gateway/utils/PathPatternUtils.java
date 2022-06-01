package com.chenyi.base.gateway.utils;

import com.chenyi.base.gateway.entity.AuthUrl;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * url鉴权配置工具类
 *
 * @author by chenyi
 * @date 2022/5/31
 */
public class PathPatternUtils {

    private static final PathPatternParser pathPatternParser = new PathPatternParser();

    public static List<AuthPathPattern> parse(List<AuthUrl> authUrls) {
        synchronized (pathPatternParser) {
            return authUrls.stream().map(authUrl -> {
                PathPattern pattern = pathPatternParser.parse(authUrl.getUrl());
                return new AuthPathPattern(pattern, authUrl);
            }).collect(Collectors.toList());
        }
    }
}
