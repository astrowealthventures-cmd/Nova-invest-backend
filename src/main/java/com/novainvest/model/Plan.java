package com.novainvest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "plans")
public class Plan {

    @Id
    private String id;

    private String name;
    private double minDeposit;
    private double maxDeposit;
    private double dailyRoi;
    private int durationDays;
    private boolean highlight;
    private String tagline;
}
