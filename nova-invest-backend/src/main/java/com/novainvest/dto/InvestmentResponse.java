package com.novainvest.dto;

import com.novainvest.model.Investment;
import lombok.Data;

@Data
public class InvestmentResponse {
    private String id;
    private String userId;
    private String planId;
    private String planName;
    private double amountUsd;
    private double dailyRoi;
    private int durationDays;
    private String status;
    private String startedAt;
    private String endsAt;
    private double daysElapsed;
    private double earnings;
    private double progressPct;

    public static InvestmentResponse from(Investment inv, double daysElapsed, double earnings, double progressPct) {
        InvestmentResponse r = new InvestmentResponse();
        r.setId(inv.getId());
        r.setUserId(inv.getUserId());
        r.setPlanId(inv.getPlanId());
        r.setPlanName(inv.getPlanName());
        r.setAmountUsd(inv.getAmountUsd());
        r.setDailyRoi(inv.getDailyRoi());
        r.setDurationDays(inv.getDurationDays());
        r.setStatus(inv.getStatus());
        r.setStartedAt(inv.getStartedAt());
        r.setEndsAt(inv.getEndsAt());
        r.setDaysElapsed(Math.round(daysElapsed * 100.0) / 100.0);
        r.setEarnings(Math.round(earnings * 100.0) / 100.0);
        r.setProgressPct(Math.round(progressPct * 10.0) / 10.0);
        return r;
    }
}
