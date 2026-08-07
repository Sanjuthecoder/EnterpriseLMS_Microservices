package com.edtech.lms.identity.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.edtech.lms.identity.config.JwtConfig;
import com.edtech.lms.identity.models.enums.UserRole;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtTokenProvider - Generates and validates JWT tokens
 * 
 * JWT Structure:
 * Header.Payload.Signature
 * 
 * Example token payload:
 * {
 *   "sub": "user@company.com",
 *   "org_id": 123,
 *   "company_id": 456,
 *   "role": "COMPANY_ADMIN",
 *   "permissions": ["VIEW_EMPLOYEES", "ASSIGN_COURSES"],
 *   "iat": 1623000000,
 *   "exp": 1623086400
 * }
 * 
 * How it works:
 * 1. Generate: Create token with claims + sign with secret
 * 2. Send: Client stores in localStorage/sessionStorage
 * 3. Request: Client sends token in Authorization header
 * 4. Validate: Server checks signature (proves token wasn't tampered)
 * 5. Extract: Server reads claims (org_id, company_id, role)
 * 
 * Security:
 * - Signature prevents tampering (if token modified, signature won't match)
 * - Expiration prevents token reuse after logout
 * - Secret key stored in .env (never in git)
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfig jwtConfig;
    private final Key key;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Create signing key from secret (must be at least 256 bits)
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    /**
     * Generate JWT token.
     *
     * @param userId           User ID
     * @param username         Username/email
     * @param orgId            Organization ID (for multi-tenancy)
     * @param companyId        Company ID (for multi-tenancy)
     * @param role             User role
     * @param subscriptionTier Company subscription tier: FREE | PREMIUM
     * @return JWT token string
     */
    public String generateToken(
            final Long userId,
            final String username,
            final Long orgId,
            final Long companyId,
            final UserRole role,
            final String subscriptionTier) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("org_id", orgId);
        claims.put("company_id", companyId);
        claims.put("role", role.toString());
        // subscription_tier is read by API Gateway and forwarded as X-Subscription-Tier header
        claims.put("subscription_tier", subscriptionTier != null ? subscriptionTier : "FREE");

        return createToken(claims, username);
    }

    /**
     * Create and sign the token
     * 
     * Process:
     * 1. Set issue time (iat) = now
     * 2. Set expiration time (exp) = now + configured expiration
     * 3. Add claims (payload data)
     * 4. Sign with secret key using HMAC-SHA256
     * 5. Serialize to compact JWT string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate JWT token
     * 
     * Checks:
     * 1. Signature is valid (not tampered)
     * 2. Token hasn't expired
     * 3. Payload is well-formed
     * 
     * @param token JWT token string
     * @return true if valid, false if invalid/expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Get org_id from JWT token
     * Used for multi-tenancy filtering
     */
    public Long getOrgIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object orgIdObj = claims.get("org_id");
        if (orgIdObj == null) return null;
        return ((Number) orgIdObj).longValue();
    }

    /**
     * Get company_id from JWT token
     * Used for multi-tenancy filtering
     */
    public Long getCompanyIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object companyIdObj = claims.get("company_id");
        if (companyIdObj == null) return null;
        return ((Number) companyIdObj).longValue();
    }

    /**
     * Get user role from JWT token
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return (String) claims.get("role");
    }
}
