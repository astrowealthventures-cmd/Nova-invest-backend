package com.novainvest.controller;

import com.novainvest.dto.*;
import com.novainvest.model.User;
import com.novainvest.repository.UserRepository;
import com.novainvest.security.CookieUtil;
import com.novainvest.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, CookieUtil cookieUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest body, HttpServletResponse response) {
        String email = body.getEmail().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setName(body.getName());
        user.setRole("user");
        user.setBalance(0.0);
        user.setReferralCode(generateReferralCode());
        user.setReferredBy(null);
        user.setCreatedAt(Instant.now().toString());
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        userRepository.save(user);

        String access = jwtService.createAccessToken(user.getId(), email);
        String refresh = jwtService.createRefreshToken(user.getId());
        cookieUtil.setAuthCookies(response, access, refresh);

        return new AuthResponse(UserResponse.from(user), access);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        String email = body.getEmail().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(body.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String access = jwtService.createAccessToken(user.getId(), email);
        String refresh = jwtService.createRefreshToken(user.getId());
        cookieUtil.setAuthCookies(response, access, refresh);

        return new AuthResponse(UserResponse.from(user), access);
    }

    @PostMapping("/logout")
    public java.util.Map<String, Boolean> logout(HttpServletResponse response, @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        cookieUtil.clearAuthCookies(response);
        return java.util.Map.of("ok", true);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return UserResponse.from(fresh);
    }

    private String generateReferralCode() {
        String chars = "0123456789ABCDEF";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
