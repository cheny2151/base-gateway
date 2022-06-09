package com.chenyi.base.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.chenyi.base.gateway.constants.GatewayConstants;
import com.chenyi.base.gateway.constants.ResponseCode;
import com.chenyi.base.gateway.entity.User;
import com.chenyi.base.gateway.entity.dto.LoginResp;
import com.chenyi.base.gateway.utils.BaseResponse;
import com.chenyi.base.gateway.utils.ExchangeResponseUtils;
import com.chenyi.base.gateway.utils.SessionUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 用户认证过滤器
 *
 * @author by chenyi
 * @date 2022/6/1
 */
@Component
@Slf4j
public class UserCertificationFilter extends AbstractGatewayFilterFactory<UserCertificationFilter.Config> {

    public final static int USER_CERTIFICATION_ORDER = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 100;

    @Resource
    private ReactiveSessionRepository<Session> sessionRepository;

    public UserCertificationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String curUrl = exchange.getRequest().getURI().getRawPath();
            if (config.getLoginUrl().equals(curUrl) && HttpStatus.OK.equals(exchange.getResponse().getStatusCode())) {
                // 请求登陆
                return ExchangeResponseUtils.apply(exchange, chain, body -> {
                    LoginResp dataResult = JSON.parseObject(body, LoginResp.class);
                    if (dataResult.getCode() == 200) {
                        // 登陆成功
                        User user = dataResult.getUser();
                        int expireOfDay = config.getExpireOfDay();
                        return sessionRepository.createSession()
                                .flatMap(session -> {
                                    session.setMaxInactiveInterval(Duration.of(expireOfDay, ChronoUnit.DAYS));
                                    session.setAttribute(GatewayConstants.Auth.USER_INFO_KEY, user);
                                    exchange.getResponse().getHeaders().add(GatewayConstants.Auth.USER_TOKEN_KEY, session.getId());
                                    return sessionRepository.save(session);
                                }).then(Mono.just(body));
                    }
                    return Mono.just(body);
                });
            } else if (config.getInfoUrl().equals(curUrl)) {
                // 请求当前用户信息
                return SessionUtils.getCurrentUser(exchange.getRequest())
                        .map(Optional::ofNullable)
                        .defaultIfEmpty(Optional.empty())
                        .flatMap(usrOpt -> {
                            Mono<DataBuffer> respBuffer;
                            respBuffer = usrOpt.map(sessionUser -> ExchangeResponseUtils.wrapRespBuffer(exchange, BaseResponse.success(sessionUser), HttpStatus.OK))
                                    // 用户未登录或session已失效
                                    .orElseGet(() -> ExchangeResponseUtils.wrapRespBuffer(exchange, BaseResponse.error(ResponseCode.USER_NOT_LOGIN), HttpStatus.UNAUTHORIZED));
                            return exchange.getResponse().writeWith(respBuffer);
                        });
            } else if (config.getLogoutUrl().equals(curUrl)) {
                // 请求登出
                return SessionUtils.getSession(exchange.getRequest())
                        .flatMap(session -> sessionRepository.deleteById(session.getId()))
                        .then(Mono.defer(() -> {
                            Mono<DataBuffer> respBuffer = ExchangeResponseUtils.wrapRespBuffer(exchange, BaseResponse.success(), HttpStatus.OK);
                            return exchange.getResponse().writeWith(respBuffer);
                        }));
            } else {
                return chain.filter(exchange);
            }
        }, USER_CERTIFICATION_ORDER);
    }

    @Data
    public static class Config {
        private String loginUrl;
        private String infoUrl;
        private String logoutUrl;
        private int expireOfDay;
    }

}
