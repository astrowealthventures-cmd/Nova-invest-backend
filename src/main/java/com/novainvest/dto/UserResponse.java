package com.novainvest.dto;

import com.novainvest.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String name;
    private String role;
    private double balance;
    private String referralCode;
    private String referredBy;
    private String createdAt;

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getRole(),
                u.getBalance(), u.getReferralCode(), u.getReferredBy(), u.getCreatedAt()
        );
    }
}
