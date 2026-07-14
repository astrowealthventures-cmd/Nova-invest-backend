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
@Document(collection = "withdrawals")
public class Withdrawal {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String asset; // BTC | ETH | USDT
    private double amountUsd;
    private String walletAddress;
    private String status; // pending | approved | rejected
    private String createdAt;
    private String processedAt;
}
