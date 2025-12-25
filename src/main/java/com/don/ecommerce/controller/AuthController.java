package com.don.ecommerce.controller;

import com.don.config.JwtUtil;
import com.don.ecommerce.dto.LoginRequest;
import com.don.ecommerce.dto.RegisterRequest;
import com.don.ecommerce.model.User;
import com.don.ecommerce.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }

        String hashed = passwordEncoder.encode(req.getPassword());
        User user = new User(
                req.getFirstName(),
                req.getLastName(),
                req.getEmail(),
                hashed,
                req.getPhone(),
                req.getStreetAddress(),
                req.getCity(),
                req.getState(),
                req.getZip(),
                req.getCountry()
        );
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.getEmail());
        // load full user from DB to read stored role and include it in the token
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(userDetails, user.getRole());

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("email", req.getEmail());
        resp.put("role", user.getRole());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthenticated"));
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Map<String, Object> out = new HashMap<>();
        out.put("id", user.getId());
        out.put("firstName", user.getFirstName());
        out.put("lastName", user.getLastName());
        out.put("email", user.getEmail());
        out.put("phone", user.getPhone());
        out.put("streetAddress", user.getStreetAddress());
        out.put("city", user.getCity());
        out.put("state", user.getState());
        out.put("zip", user.getZip());
        out.put("country", user.getCountry());
        out.put("role", user.getRole());

        return ResponseEntity.ok(out);
    }

    @GetMapping("/count")
    public ResponseEntity<?> countUsers() {
        long count = userRepository.count();
        return ResponseEntity.ok(Map.of("count", count));
    }

}
