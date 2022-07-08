package com.chenyi.base.gateway.utils;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 响应工具类
 *
 * @author by chenyi
 * @date 2022/6/1
 */
public class ExchangeResponseUtils {

    /**
     * 消费响应结果
     *
     * @param exchange  ServerWebExchange
     * @param chain     GatewayFilterChain
     * @param applyFunc 消费的逻辑
     * @return Mono<Void>
     */
    public static Mono<Void> apply(ServerWebExchange exchange, GatewayFilterChain chain, Function<String, Mono<?>> applyFunc) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<DataBuffer> modifyBody = Flux.from(body).buffer().flatMap(dataBuffers -> {
                        // probably should reuse buffers
                        String bodyStr = dataBuffers.stream().map(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            //释放掉内存
                            DataBufferUtils.release(dataBuffer);
                            return new String(content, StandardCharsets.UTF_8);
                        }).collect(Collectors.joining());
                        Mono<?> mono = applyFunc.apply(bodyStr);
                        return mono.then(Mono.just(bufferFactory.wrap(bodyStr.getBytes())));
                    });
                    return super.writeWith(modifyBody);
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
        // replace response with decorator
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }


    /**
     * 消费响应结果并重写响应
     *
     * @param exchange  ServerWebExchange
     * @param chain     GatewayFilterChain
     * @param applyFunc 重写的逻辑
     * @return Mono<Void>
     */
    public static Mono<Void> rewrite(ServerWebExchange exchange, GatewayFilterChain chain, Function<String, Mono<String>> applyFunc) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<String> modifiedBody = Flux.from(body).buffer().flatMap(dataBuffers -> {
                        // probably should reuse buffers
                        String bodyStr = dataBuffers.stream().map(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            //释放掉内存
                            DataBufferUtils.release(dataBuffer);
                            return new String(content, StandardCharsets.UTF_8);
                        }).collect(Collectors.joining());
                        Mono<String> mono = applyFunc.apply(bodyStr);
                        return mono.defaultIfEmpty(bodyStr);
                    });
                    BodyInserter<Flux<String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
                    CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
                            exchange.getResponse().getHeaders());
                    return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
                        ServerHttpResponse delegate = getDelegate();
                        Mono<DataBuffer> messageBody = DataBufferUtils.join(outputMessage.getBody());
                        HttpHeaders headers = delegate.getHeaders();
                        if (!headers.containsKey(HttpHeaders.TRANSFER_ENCODING)
                                || headers.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                            messageBody = messageBody.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
                        }
                        // TODO: fail if isStreamingMediaType?
                        return delegate.writeWith(messageBody);
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
