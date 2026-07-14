package com.novainvest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawRequest {

    @NotNull
    @Pattern(regexp = "BTC|ETH|USDT", message = "asset must be BTC, ETH or USDT")
    private String asset;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double amountUsd;

    @NotNull
    @Size(min = 6)
    private String walletAddress;
}
