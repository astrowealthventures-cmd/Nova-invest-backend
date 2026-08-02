package com.novainvest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify?token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", brevoApiKey);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of(
                "name", "AstroWealthVentures",
                "email", "astrowealthventures@gmail.com"
        ));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", "Verify your AstroWealthVentures account");
        body.put("htmlContent",
                "<p>Please confirm your email by clicking the link below:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<p>This link expires in 24 hours.</p>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Failed to send verification email: " + e.getResponseBodyAsString(), e);
        }
    }

    public void sendForgottenPasswordOtp(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", brevoApiKey);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of(
                "name", "AstroWealthVentures",
                "email", "astrowealthventures@gmail.com"
        ));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", "Reset your AstroWealthVentures password");
        body.put("htmlContent",
                "<p>Create a new password by clicking the link below:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<p>This link expires in 15 minutes.</p>");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Failed to send new password email: " + e.getResponseBodyAsString(), e);
        }
    }
}