package com.edtech.lms.ai.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PremiumTierFilter — Servlet filter that guards ALL /api/v1/ai/** endpoints.
 *
 * Reads the {@code X-Subscription-Tier} header forwarded by the API Gateway.
 * The header is populated by the Gateway's JwtAuthenticationFilter from the
 * {@code subscription_tier} JWT claim, which is set by the Identity Service on login.
 *
 * If the header is absent or not "PREMIUM", returns HTTP 403 with a structured
 * error body that the frontend can use to show an appropriate upgrade prompt.
 *
 * Permitted paths (bypassed even for FREE tier):
 * - /actuator/** — health checks for Docker/Kubernetes probes
 *
 * This is the last line of defense — the Gateway also provides routing-level
 * gating, but this filter ensures no direct-call bypass is possible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumTierFilter extends OncePerRequestFilter {

    private static final String HEADER_SUBSCRIPTION_TIER = "X-Subscription-Tier";
    private static final String TIER_PREMIUM             = "PREMIUM";
    private static final String ACTUATOR_PREFIX          = "/actuator";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String path = request.getRequestURI();

        // Allow actuator endpoints through for health checks
        if (path.startsWith(ACTUATOR_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String subscriptionTier = request.getHeader(HEADER_SUBSCRIPTION_TIER);
        // Allow only premium tier – other roles are handled elsewhere (e.g., gateway)
        if (TIER_PREMIUM.equalsIgnoreCase(subscriptionTier)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject with a structured 403 that frontend toasts can display
        log.warn("Access denied to AI endpoint [{}] — subscription tier: '{}'",
                path, subscriptionTier != null ? subscriptionTier : "missing");

        rejectWithPremiumRequired(response, subscriptionTier);
    }

    /**
     * Writes a JSON 403 response with a message the frontend can display in a toast.
     * The {@code upgradeRequired: true} flag lets the frontend distinguish this 403
     * from a regular permission error and show the correct upgrade CTA.
     */
    private void rejectWithPremiumRequired(
            final HttpServletResponse response,
            final String actualTier) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Map<String, Object> body = Map.of(
            "status",          403,
            "error",           "Premium Subscription Required",
            "message",         "This feature is available exclusively for premium subscribers. "
                               + "Upgrade your plan to unlock AI-powered personalized learning.",
            "upgradeRequired", true,
            "currentTier",     actualTier != null ? actualTier : "NONE",
            "timestamp",       LocalDateTime.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
