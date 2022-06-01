package com.shopline.ai.kf.gateway.filter.global;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopline.ai.kf.gateway.constants.GatewayConstants;
import com.shopline.ai.kf.gateway.constants.ResponseCode;
import com.shopline.ai.kf.gateway.entity.AuthUrl;
import com.shopline.ai.kf.gateway.entity.User;
import com.shopline.ai.kf.gateway.utils.AuthPathPattern;
import com.shopline.ai.kf.gateway.utils.BaseResponse;
import com.shopline.ai.kf.gateway.utils.Mock;
import com.shopline.ai.kf.gateway.utils.SessionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 全局过滤器--鉴权
 * 时序图：https://shopline.yuque.com/lffo6q/yglcsm/fgp1yb
 *
 * @author by chenyi
 * @date 2022/5/27
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String reqUrl = getReqUrl(exchange);
        Mono<Optional<User>> userOptMono = SessionUtils.getCurrentUser(request)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
        return Mono.from(Flux.zip(Mock.getAuthUrls(), userOptMono)
                .flatMap(t2 -> {
                    ServerWebExchange finallyExchange = exchange;
                    Map<String, Object> attributes = exchange.getAttributes();
                    List<AuthPathPattern> authUrls = t2.getT1();
                    Optional<User> userOpt = t2.getT2();
                    boolean hasLogin = userOpt.isPresent();
                    if (hasLogin) {
                        User user = userOpt.get();
                        attributes.put(GatewayConstants.Content.USER_KEY, user);
                        // 用户已登陆
                        ServerHttpRequest newReq = request.mutate().header(GatewayConstants.Auth.USER_ID_KEY, String.valueOf(user.getId()))
                                .header(GatewayConstants.Auth.USER_NAME_KEY, user.getName())
                                .header(GatewayConstants.Auth.USER_ROLES_KEY, user.getRoles().toArray(new String[0]))
                                .header(GatewayConstants.Auth.USER_TENANT_KEY, user.getTenant())
                                .build();
                        finallyExchange = exchange.mutate().request(newReq).build();
                    }
                    Optional<AuthUrl> authUrlOpt = matchUrl(reqUrl, request.getMethodValue(), authUrls);
                    if (authUrlOpt.isPresent()) {
                        ServerHttpResponse response = exchange.getResponse();
                        // url配置了鉴权
                        AuthUrl authUrl = authUrlOpt.get();
                        if (!authUrl.isAuth()) {
                            // 无需鉴权,需要登陆
                            if (!hasLogin) {
                                Mono<DataBuffer> respBuffer = wrapRespBuffer(exchange, ResponseCode.USER_NOT_LOGIN, HttpStatus.UNAUTHORIZED);
                                return response.writeWith(respBuffer);
                            }
                        } else {
                            // 需要鉴权
                            List<String> authCodes = null;
                            String requiredAuth = authUrl.getAuthCode();
                            if (hasLogin && CollectionUtils.isNotEmpty(userOpt.get().getRoles())) {
                                User user = userOpt.get();
                                authCodes = Mock.getAuthCodes(user.getRoles());
                            }
                            if (authCodes != null && authCodes.contains(requiredAuth)) {
                                // 鉴权通过
                                return chain.filter(finallyExchange);
                            } else {
                                Mono<DataBuffer> respBuffer = wrapRespBuffer(exchange, ResponseCode.FORBIDDEN, HttpStatus.FORBIDDEN);
                                return response.writeWith(respBuffer);
                            }
                        }
                    }
                    return chain.filter(finallyExchange);
                }));
    }

    @Override
    public int getOrder() {
        return -2147482738;
    }

    /**
     * 通过当前url与http method进行鉴权url匹配
     *
     * @param url      当前请求url
     * @param method   当前请求方法
     * @param authUrls 鉴权url配置集合
     * @return 匹配的配置
     */
    private Optional<AuthUrl> matchUrl(String url, String method, List<AuthPathPattern> authUrls) {
        PathContainer path = PathContainer.parsePath(url);
        return authUrls.stream().filter(pattern -> (StringUtils.isEmpty(pattern.getAuthUrl().getMethod()) ||
                        pattern.getAuthUrl().getMethod().equals(method)) &&
                        pattern.getPathPattern().matches(path))
                .map(AuthPathPattern::getAuthUrl)
                .findFirst();
    }

    private String getReqUrl(ServerWebExchange exchange) {
        return exchange.getRequest().getURI().getRawPath();
    }

    private Mono<DataBuffer> wrapRespBuffer(ServerWebExchange exchange, ResponseCode responseCode, HttpStatus status) {
        return Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            BaseResponse<Object> resp = BaseResponse.error(responseCode);
            response.setStatusCode(status);
            DataBuffer wrap;
            try {
                wrap = response.bufferFactory().wrap(new ObjectMapper().writeValueAsBytes(resp));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
            return Mono.just(wrap);
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Flux<Tuple2<Integer, String>> flux = Flux.zip(
                        Mono.defer(() -> Mono.fromFuture(CompletableFuture.supplyAsync(getData(6)))),
                        Mono.defer(() -> Mono.just("5"))
                )
                .doOnNext(t2 -> {
                    System.out.println(t2);
                });
        Thread.sleep(10);
        System.out.println("---------------");
        long l = System.currentTimeMillis();
        Disposable subscribe = flux.subscribe();
        while (!subscribe.isDisposed()) {
            System.out.println("un isDisposed");
            Thread.sleep(500);
        }
        System.out.println(System.currentTimeMillis() - l);
    }

    public static Supplier<Integer> getData(int time) {
        return () -> {
            try {
                System.out.println("+++++++++++");
                Thread.sleep(time * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return time;
        };
    }

}
