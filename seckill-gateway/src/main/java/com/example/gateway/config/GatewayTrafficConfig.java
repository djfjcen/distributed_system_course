package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayTrafficConfig {

    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return (ServerWebExchange exchange) -> {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress == null || remoteAddress.getAddress() == null) {
                return Mono.just("unknown");
            }
            return Mono.just(remoteAddress.getAddress().getHostAddress());
        };
    }
}
