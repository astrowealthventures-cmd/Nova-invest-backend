package com.novainvest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String role; // "user" | "admin"
    private double balance;
    private String referralCode;
    private String referredBy;
    private String createdAt;

    // never serialized to the client - stripped in the controller layer
    private String passwordHash;
}
