package com.novainvest.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminActionRequest {

    @Pattern(regexp = "approve|reject", message = "action must be approve or reject")
    private String action;
}
