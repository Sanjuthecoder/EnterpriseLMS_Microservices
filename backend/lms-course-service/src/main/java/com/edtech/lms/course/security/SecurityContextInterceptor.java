package com.edtech.lms.course.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class SecurityContextInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String COMPANY_ID_HEADER = "X-Company-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);
        String companyId = request.getHeader(COMPANY_ID_HEADER);

        // Store these in a ThreadLocal or RequestContext if needed by the service layer
        if (userId != null && role != null) {
            log.debug("Authenticated Request from API Gateway: User={}, Role={}, Company={}", userId, role, companyId);
            request.setAttribute("userId", userId);
            request.setAttribute("userRole", role);
            request.setAttribute("companyId", companyId);
            return true;
        }

        // If headers are missing, the Gateway didn't inject them, meaning unauthorized bypass
        log.warn("Missing security headers. Rejecting request.");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
