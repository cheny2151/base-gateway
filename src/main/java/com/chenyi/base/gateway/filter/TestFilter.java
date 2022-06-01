package com.chenyi.base.gateway.filter;

import com.chenyi.base.gateway.constants.GatewayConstants;
import com.chenyi.base.gateway.entity.User;
import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author by chenyi
 * @date 2022/5/25
 */
@Component
public class TestFilter implements GatewayFilterFactory<TestFilter.TestConfig> {

    @Resource
    private ReactiveSessionRepository<Session> sessionRepository;

    @Override
    public GatewayFilter apply(TestConfig testConfig) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String tokenStr = headers.getFirst(GatewayConstants.Auth.USER_TOKEN_KEY);
            if (tokenStr == null) {
                return exchange.getSession().doOnNext(session -> {
                            System.out.println(session.getId());
                        }).then(sessionRepository.createSession())
                        .flatMap(session1 -> {
                            System.out.println("create session:" + session1.getId());
                            User user = new User();
                            user.setId(123);
                            user.setName("test");
                            user.setTenant("test");
                            user.setRoles(Arrays.asList("admin", "user:update"));
                            session1.setAttribute(GatewayConstants.Auth.USER_INFO_KEY, user);
                            exchange.getResponse().getHeaders().add(GatewayConstants.Auth.USER_TOKEN_KEY, session1.getId());
                            return sessionRepository.save(session1);
                        }).then(chain.filter(exchange));
            } else {
                System.out.println(tokenStr);
                return sessionRepository.findById(tokenStr).doOnNext(session -> {
                    System.out.println(session.getId());
                    System.out.println((String) session.getAttribute("user"));
                }).then(chain.filter(exchange));
            }
        }, -2147482758);
    }

    @Data
    public static class TestConfig {
        private int test;
    }

    @Override
    public Class<TestConfig> getConfigClass() {
        return TestConfig.class;
    }

}
