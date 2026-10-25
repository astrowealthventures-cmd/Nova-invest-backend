package com.novainvest.controller;

import com.novainvest.dto.*;
import com.novainvest.model.User;
import com.novainvest.model.VerificationToken;
import com.novainvest.repository.UserRepository;
import com.novainvest.repository.VerificationTokenRepository;
import com.novainvest.security.CookieUtil;
import com.novainvest.security.JwtService;
import com.novainvest.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, CookieUtil cookieUtil, VerificationTokenRepository verificationTokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
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
        user.setFirstName(body.getFirstName());
        user.setLastName(body.getLastName());
        user.setCurrency(body.getCurrency());
        user.setDateOfBirth(body.getDateOfBirth());
        user.setRole("user");
        user.setBalance(0.0);
        user.setReferralCode(generateReferralCode());
        user.setReferredBy(null);
        user.setCreatedAt(Instant.now().toString());
        user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        user.setEnabled(false);
       User savedUser =  userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token,savedUser.getId());
        verificationTokenRepository.save(verificationToken);

        try{
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        }catch (Exception e){
            log.error("could not end verification email",e);
        }


        return new AuthResponse(UserResponse.from(user), token);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token,HttpServletResponse response){
       VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("verification token could not be found"));



       if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())){
           return ResponseEntity.badRequest().body("token already expired");
       }

       User user = userRepository.findById(verificationToken.getUserId())
               .orElseThrow(() -> new  RuntimeException("user was not found"));

       user.setEnabled(true);
       userRepository.save(user);
       verificationTokenRepository.delete(verificationToken);

        String access = jwtService.createAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.createRefreshToken(user.getId());
        cookieUtil.setAuthCookies(response, access, refresh);

       return ResponseEntity.ok("verified");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        String email = body.getEmail().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(body.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Please verify your email before logging in");
        }

        String access = jwtService.createAccessToken(user.getId(), email);
        String refresh = jwtService.createRefreshToken(user.getId());
        cookieUtil.setAuthCookies(response, access, refresh);

        return new AuthResponse(UserResponse.from(user), access);
    }

    @PostMapping("/forgotten-password")
    public ResponseEntity<?> forgottenPassword(@RequestBody EmailAddress email) {
        User user = userRepository.findByEmail(email.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "email does not exist, please create an account"));

        // optional: prevent stale/duplicate tokens piling up for the same user
        verificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user.getId());
        verificationTokenRepository.save(verificationToken);

        emailService.sendForgottenPasswordOtp(user.getEmail(), token);

        // never return the token itself in the response
        return ResponseEntity.ok("If that email exists, a reset link has been sent.");
    }

    @PatchMapping("/verify-forgotten-password")
    public ResponseEntity<?> verifyForgottenPassword(
            @RequestParam("token") String token,
            @RequestBody PasswordHandler passwordHandler) {

        System.out.println("DEBUG token=" + token);
        System.out.println("DEBUG passwordHandler=" + passwordHandler);

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("verification token could not be found"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("token already expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new RuntimeException("user was not found"));

        user.setPasswordHash(passwordEncoder.encode(passwordHandler.newPassword()));
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

        return ResponseEntity.ok("password reset successful");
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

    private final Map<String, Instant> resendCooldown = new ConcurrentHashMap<>();

    @PostMapping("/resend-verification")
    @Transactional
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").toLowerCase().trim();

        if (email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Email is required."));
        }

        // 60-second cooldown per email
        Instant last = resendCooldown.get(email);
        if (last != null && last.plusSeconds(60).isAfter(Instant.now())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("detail", "Please wait a minute before requesting another email."));
        }
        resendCooldown.put(email, Instant.now());

        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEnabled()) {
                verificationTokenRepository.deleteByUserId(user.getId());

                String token = UUID.randomUUID().toString();
                verificationTokenRepository.save(new VerificationToken(token, user.getId()));

                try {
                    emailService.sendVerificationEmail(user.getEmail(), token);
                } catch (Exception e) {
                    log.error("Could not resend verification email", e);
                }
            }
        });

        // Same response whether or not the account exists, so nobody can probe for registered emails
        return ResponseEntity.ok(Map.of("message", "Verification email resent"));
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
