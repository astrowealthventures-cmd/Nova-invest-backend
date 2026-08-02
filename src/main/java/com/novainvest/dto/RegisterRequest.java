package com.novainvest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    private String firstName;

    private String lastName;


    @Email
    private String email;

    private String phoneNumber;

    private String currency;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
