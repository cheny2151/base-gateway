package com.chenyi.base.gateway.filter.global;

import com.chenyi.base.gateway.constants.GatewayConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 全局过滤器--request body缓存
 *
 * @author by chenyi
 * @date 2022/5/25
 */
@Component
public class CacheRequestBodyFilter implements GlobalFilter, Ordered {

    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
                ServerWebExchange delegate = exchange.mutate().request(serverHttpRequest).build();
                return ServerRequest.create(delegate, messageReaders).bodyToMono(Map.class)
                        .doOnNext((objectValue) -> exchange.getAttributes().put(GatewayConstants.Content.REQUEST_BODY_OBJECT_KEY, objectValue))
                        .then(chain.filter(delegate))
                        .doFinally(type -> exchange.getAttributes().remove(GatewayConstants.Content.REQUEST_BODY_OBJECT_KEY));
            });
        } else {
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -2147482748;
    }

}
