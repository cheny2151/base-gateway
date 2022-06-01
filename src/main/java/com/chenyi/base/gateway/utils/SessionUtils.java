package com.chenyi.base.gateway.utils;

import cn.cheny.toolbox.spring.SpringUtils;
import com.chenyi.base.gateway.constants.GatewayConstants;
import com.chenyi.base.gateway.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import reactor.core.publisher.Mono;

/**
 * session工具类
 *
 * @author by chenyi
 * @date 2022/5/27
 */
public class SessionUtils {

    private static ReactiveSessionRepository<Session> sessionRepository;

    public static Mono<User> getCurrentUser(ServerHttpRequest request) {
        return getSession(request)
                .flatMap(session -> Mono.just(session.getAttribute(GatewayConstants.Auth.USER_INFO_KEY)));
    }

    public static Mono<Session> getSession(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(GatewayConstants.Auth.USER_TOKEN_KEY);
        if (StringUtils.isEmpty(token)) {
            return Mono.empty();
        }
        return getSession(token);
    }

    public static Mono<Session> getSession(String token) {
        return getRepository().findById(token);
    }

    private static ReactiveSessionRepository<Session> getRepository() {
        ReactiveSessionRepository<Session> sessionRepository = SessionUtils.sessionRepository;
        if (sessionRepository == null) {
            sessionRepository = initRepository();
        }
        return sessionRepository;
    }

    @SuppressWarnings("unchecked")
    private static synchronized ReactiveSessionRepository<Session> initRepository() {
        if (sessionRepository == null) {
            sessionRepository = SpringUtils.getBean(ReactiveSessionRepository.class);
            if (sessionRepository == null) {
                // todo 修改为合适的异常
                throw new RuntimeException("程序未完成初始化");
            }
        }
        return sessionRepository;
    }

}
