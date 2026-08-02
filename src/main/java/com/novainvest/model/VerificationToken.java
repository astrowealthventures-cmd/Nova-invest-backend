package com.novainvest.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "verification_tokens")
public class VerificationToken {

    @Id
    private String id;

    private String token;

    private String userId; // reference to User's id

    private LocalDateTime expiryDate;

    public VerificationToken() {
        // required by Spring Data MongoDB
    }

    public VerificationToken(String token, String userId) {
        this.token = token;
        this.userId = userId;
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    // getters and setters
    public String getId() { return id; }
    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
}
