package com.chenyi.base.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author by chenyi
 * @date 2022/5/25
 */
@Component
public class TestFilter2 implements GatewayFilterFactory<TestFilter2.TestConfig> {

    @Override
    public GatewayFilter apply(TestConfig testConfig) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            Map cachedRequestBodyObject = exchange.getAttribute("cachedRequestBodyObject");
            System.out.println(cachedRequestBodyObject);
            try {
                System.out.println(exchange.getAttribute("cachedRequestBody").getClass());
                System.out.println(exchange.getAttribute("cachedRequestBodyObject").getClass());
            } catch (Exception e) {

            }
            return chain.filter(exchange);
        }, 200);
    }

    @Data
    public static class TestConfig {
        private int test;
    }

    @Override
    public Class<TestConfig> getConfigClass() {
        return TestConfig.class;
    }

    public static void main(String[] args) throws InterruptedException {
        Flux.just(2, 1, 3, 4, 5, 6).concatMap(i -> {
            return Mono.fromFuture(
                    // 模拟异步任务
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            System.out.println("innnnnn");
                            Thread.sleep(i * 1000);
                            return i;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return i;
                    }));
        }).next().doOnNext(e -> System.out.println(Thread.currentThread().getName() + ":" + e)).subscribe();
        Thread.sleep(100000);
    }
}
