package com.edtech.lms.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final String jwtSecret;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid authorization header for path: {}", path);
            return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            ServerHttpRequest request = applyJwtToRequest(token, exchange.getRequest());
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {
            logger.error("JWT Authentication failed: {}", e.getMessage());
            return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") || path.startsWith("/api/public/") || path.startsWith("/uploads/");
    }

    private ServerHttpRequest applyJwtToRequest(String token, ServerHttpRequest currentRequest) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.get("userId") != null ? claims.get("userId").toString() : claims.getSubject();
        String role = claims.get("role", String.class);
        String companyId = claims.get("company_id") != null ? claims.get("company_id").toString() : "";
        String orgId = claims.get("org_id") != null ? claims.get("org_id").toString() : "";
        String subscriptionTier = claims.get("subscription_tier") != null
                ? claims.get("subscription_tier").toString() : "FREE";

        return currentRequest.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .header("X-Company-Id", companyId)
                .header("X-Org-Id", orgId)
                .header("X-Subscription-Tier", subscriptionTier)
                .build();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Execute before routing
    }
}
