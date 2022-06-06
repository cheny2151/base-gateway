package com.chenyi.base.gateway.utils;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 响应工具类
 *
 * @author by chenyi
 * @date 2022/6/1
 */
public class ExchangeResponseUtils {

    /**
     * 重写响应结果
     *
     * @param exchange    ServerWebExchange
     * @param chain       GatewayFilterChain
     * @param rewriteFunc 重写的逻辑
     * @return Mono<Void>
     */
    public static Mono<Void> rewrite(ServerWebExchange exchange, GatewayFilterChain chain, Function<String, ? extends Mono<String>> rewriteFunc) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.flatMap(dataBuffer -> {
                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        String bodyStr = new String(content, StandardCharsets.UTF_8);
                        Mono<String> writer = rewriteFunc.apply(bodyStr);
                        return writer.map(rewriteBody -> bufferFactory.wrap(rewriteBody.getBytes()));
                    }));
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        // replace response with decorator
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 将数据包装为DataBuffer
     *
     * @param exchange ServerWebExchange
     * @param respBody 响应数据
     * @param status   http状态码
     * @return Mono<DataBuffer>
     */
    public static Mono<DataBuffer> wrapRespBuffer(ServerWebExchange exchange, Object respBody, HttpStatus status) {
        return Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatusCode(status);
            DataBuffer wrap = response.bufferFactory().wrap(JsonUtils.toJsonBytes(respBody));
            return Mono.just(wrap);
        });
    }

}
