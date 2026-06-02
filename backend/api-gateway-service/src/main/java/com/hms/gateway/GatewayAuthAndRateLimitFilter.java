package com.hms.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GatewayAuthAndRateLimitFilter implements GlobalFilter, Ordered {

    private static final String API_PREFIX = "/api/";

    private final String gatewayApiKey;
    private final int rateLimitPerMinute;
    private final ConcurrentMap<String, RequestWindow> requestCounters = new ConcurrentHashMap<>();

    public GatewayAuthAndRateLimitFilter(
            @Value("${hms.gateway.api-key}") String gatewayApiKey,
            @Value("${hms.gateway.rate-limit-per-minute}") int rateLimitPerMinute
    ) {
        this.gatewayApiKey = gatewayApiKey;
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (!path.startsWith(API_PREFIX) || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String apiKey = request.getHeaders().getFirst("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getQueryParams().getFirst("api_key");
        }

        if (!gatewayApiKey.equals(apiKey)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String clientKey = resolveClientKey(request);
        long minuteBucket = Instant.now().getEpochSecond() / 60;

        RequestWindow window = requestCounters.compute(clientKey, (key, existing) -> {
            if (existing == null || existing.minuteBucket != minuteBucket) {
                return new RequestWindow(minuteBucket, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (window != null && window.counter.get() > rateLimitPerMinute) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add(HttpHeaders.RETRY_AFTER, "60");
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }

        return Optional.ofNullable(remoteAddress.getAddress().getHostAddress()).orElse("unknown");
    }

    private static class RequestWindow {
        private final long minuteBucket;
        private final AtomicInteger counter;

        private RequestWindow(long minuteBucket, AtomicInteger counter) {
            this.minuteBucket = minuteBucket;
            this.counter = counter;
        }
    }
}
