package com.don.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        logger.debug("JwtUtil initialized with secret length {} and expirationMs={}", secret.length(), expirationMs);
    }

    public String generateToken(UserDetails userDetails) {
        // delegate to overloaded method by deriving roles from authorities
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        String roles = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        logger.debug("JwtUtil.generateToken - generating token for user {} with roles {}", userDetails.getUsername(), roles);
        return generateToken(userDetails, roles);
    }

    // Overloaded: generate token using explicit role string (from DB). This ensures the
    // role claim reflects the stored user role when provided.
    public String generateToken(UserDetails userDetails, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        logger.debug("JwtUtil.generateToken(UserDetails, role) - issuing token for {} role={}", userDetails.getUsername(), role);
        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        // Do not log the token itself in production. We log issuance metadata only.
        logger.debug("JwtUtil.generateToken - token issued for {} expires at {}", userDetails.getUsername(), expiry);
        return token;
    }

    public String getUsernameFromToken(String token) {
        try {
            String username = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getSubject();
            logger.debug("JwtUtil.getUsernameFromToken - parsed username {} from token", username);
            return username;
        } catch (Exception ex) {
            logger.debug("JwtUtil.getUsernameFromToken - failed to parse token: {}", ex.getMessage());
            throw ex;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            boolean ok = (username.equals(userDetails.getUsername()));
            logger.debug("JwtUtil.validateToken - token username={} expected={} valid={}", username, userDetails.getUsername(), ok);
            return ok;
        } catch (Exception ex) {
            logger.debug("JwtUtil.validateToken - exception during validation: {}", ex.getMessage());
            return false;
        }
    }
}
