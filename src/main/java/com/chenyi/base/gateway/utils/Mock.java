package com.chenyi.base.gateway.utils;

import com.chenyi.base.gateway.entity.AuthUrl;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author by chenyi
 * @date 2022/5/27
 */
public class Mock {

    private final PathPatternParser pathPatternParser = new PathPatternParser();

    public static Mono<List<AuthPathPattern>> getAuthUrls() {
        return Mono.just(PathPatternUtils.parse(Arrays.asList(
                new AuthUrl("test", "/resource/a/**", "", 0, "POST"),
                new AuthUrl("test", "/resource/b/**", "admin", 1, "POST"),
//                new AuthUrl("test", "/resource/c/**", "test", 1, "POST"))
                new AuthUrl("test", "/resource/c", "admin", 1, "POST"),
                new AuthUrl("test", "/resource/c/*", "admin", 1, "POST"),
                new AuthUrl("test", "/resource/c/**", "admin", 1, "POST"),
                new AuthUrl("test", "/resource/c/**", "test", 1, "DELETE"))
        ));
    }

    public static List<String> getAuthCodes(List<String> roleCodes) {
        return Arrays.asList(
                "admin","usr:add"
        );
    }

}
