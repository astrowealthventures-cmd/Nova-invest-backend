package com.novainvest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvestRequest {

    @NotBlank
    private String planId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double amountUsd;
}
