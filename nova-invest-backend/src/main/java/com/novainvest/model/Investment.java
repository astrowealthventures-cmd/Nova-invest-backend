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
@Document(collection = "investments")
public class Investment {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String planId;
    private String planName;
    private double amountUsd;
    private double dailyRoi;
    private int durationDays;
    private String status; // active | completed
    private String startedAt;
    private String endsAt;
}
