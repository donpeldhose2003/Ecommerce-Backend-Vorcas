package com.don.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.don.ecommerce.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // New detailed request-level logging
        String path = request.getRequestURI();
        String method = request.getMethod();
        logger.debug("JwtRequestFilter - incoming request: method={} path={}", method, path);

        // Skip JWT validation for auth endpoints and CORS preflight
        if (method != null && "OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("JwtRequestFilter - skipping JWT validation for OPTIONS request");
            chain.doFilter(request, response);
            return;
        }
        // Only skip public auth endpoints (login/register). Do NOT skip /auth/me etc.
        if (path != null) {
            String lower = path.toLowerCase();
            if (lower.equals("/auth/login") || lower.equals("/auth/register") || lower.equals("/api/auth/login") || lower.equals("/api/auth/register")) {
                logger.debug("JwtRequestFilter - skipping JWT validation for public auth path: {}", path);
                chain.doFilter(request, response);
                return;
            }
        }

        final String authHeader = request.getHeader("Authorization");
        logger.debug("JwtRequestFilter - Authorization header present: {}", authHeader != null);

        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            // don't log the full token; log a masked/shortened version
            String masked = jwt.length() > 10 ? jwt.substring(0, 6) + "..." + jwt.substring(jwt.length() - 4) : jwt;
            logger.debug("JwtRequestFilter - extracted JWT (masked): {}", masked);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                logger.debug("JwtRequestFilter - extracted username from token: {}", username);
            } catch (Exception ex) {
                logger.debug("JwtRequestFilter - failed to parse token: {}", ex.getMessage(), ex);
            }
        } else {
            if (authHeader == null) {
                logger.debug("JwtRequestFilter - no Authorization header provided");
            } else {
                logger.debug("JwtRequestFilter - Authorization header does not start with 'Bearer '");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("JwtRequestFilter - attempting to load user details for username: {}", username);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            try {
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    String authorities = userDetails.getAuthorities().stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","));
                    logger.debug("JwtRequestFilter - authentication set for user: {} authorities: {}", username, authorities);
                } else {
                    logger.debug("JwtRequestFilter - token validation failed for user: {}", username);
                }
            } catch (Exception ex) {
                logger.debug("JwtRequestFilter - exception during token validation: {}", ex.getMessage(), ex);
            }
        } else {
            if (username == null) {
                logger.debug("JwtRequestFilter - username resolved from token is null; request will remain anonymous");
            } else {
                logger.debug("JwtRequestFilter - SecurityContext already contains authentication: {}", SecurityContextHolder.getContext().getAuthentication());
            }
        }

        chain.doFilter(request, response);
    }
}
