package com.novainvest.controller;

import com.novainvest.dto.InvestRequest;
import com.novainvest.dto.InvestmentResponse;
import com.novainvest.model.Investment;
import com.novainvest.model.Plan;
import com.novainvest.model.Transaction;
import com.novainvest.model.User;
import com.novainvest.repository.InvestmentRepository;
import com.novainvest.repository.PlanRepository;
import com.novainvest.repository.TransactionRepository;
import com.novainvest.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentRepository investmentRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public InvestmentController(InvestmentRepository investmentRepository, PlanRepository planRepository,
                                 UserRepository userRepository, TransactionRepository transactionRepository) {
        this.investmentRepository = investmentRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public Investment createInvestment(@Valid @RequestBody InvestRequest body, @AuthenticationPrincipal User user) {
        Plan plan = planRepository.findById(body.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (body.getAmountUsd() < plan.getMinDeposit() || body.getAmountUsd() > plan.getMaxDeposit()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount must be between $" + plan.getMinDeposit() + " and $" + plan.getMaxDeposit());
        }

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.getAmountUsd() > fresh.getBalance()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance. Please deposit first.");
        }

        fresh.setBalance(fresh.getBalance() - body.getAmountUsd());
        userRepository.save(fresh);

        Instant started = Instant.now();
        Instant ends = started.plus(plan.getDurationDays(), ChronoUnit.DAYS);

        Investment inv = new Investment();
        inv.setId(UUID.randomUUID().toString());
        inv.setUserId(user.getId());
        inv.setPlanId(plan.getId());
        inv.setPlanName(plan.getName());
        inv.setAmountUsd(body.getAmountUsd());
        inv.setDailyRoi(plan.getDailyRoi());
        inv.setDurationDays(plan.getDurationDays());
        inv.setStatus("active");
        inv.setStartedAt(started.toString());
        inv.setEndsAt(ends.toString());
        investmentRepository.save(inv);

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setUserId(user.getId());
        tx.setType("invest");
        tx.setAmountUsd(body.getAmountUsd());
        tx.setAsset("USD");
        tx.setStatus("completed");
        tx.setRefId(inv.getId());
        tx.setCreatedAt(Instant.now().toString());
        transactionRepository.save(tx);

        return inv;
    }

    @GetMapping
    public List<InvestmentResponse> listInvestments(@AuthenticationPrincipal User user) {
        List<Investment> items = investmentRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        return items.stream().map(this::withEarnings).collect(Collectors.toList());
    }

    private InvestmentResponse withEarnings(Investment inv) {
        Instant started = Instant.parse(inv.getStartedAt());
        double daysElapsed = Math.min(inv.getDurationDays(),
                (Instant.now().toEpochMilli() - started.toEpochMilli()) / 86400000.0);
        double earnings = inv.getAmountUsd() * (inv.getDailyRoi() / 100.0) * daysElapsed;
        double progressPct = (daysElapsed / inv.getDurationDays()) * 100.0;
        return InvestmentResponse.from(inv, daysElapsed, earnings, progressPct);
    }
}
