package com.novainvest.controller;

import com.novainvest.model.Investment;
import com.novainvest.model.User;
import com.novainvest.repository.DepositRepository;
import com.novainvest.repository.InvestmentRepository;
import com.novainvest.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final UserRepository userRepository;
    private final InvestmentRepository investmentRepository;
    private final DepositRepository depositRepository;

    public PortfolioController(UserRepository userRepository, InvestmentRepository investmentRepository,
                                DepositRepository depositRepository) {
        this.userRepository = userRepository;
        this.investmentRepository = investmentRepository;
        this.depositRepository = depositRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal User user) {
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Investment> investments = investmentRepository.findByUserId(user.getId());

        double invested = investments.stream()
                .filter(i -> "active".equals(i.getStatus()))
                .mapToDouble(Investment::getAmountUsd)
                .sum();

        double earnings = 0.0;
        for (Investment inv : investments) {
            Instant started = Instant.parse(inv.getStartedAt());
            double daysElapsed = Math.min(inv.getDurationDays(),
                    (Instant.now().toEpochMilli() - started.toEpochMilli()) / 86400000.0);
            earnings += inv.getAmountUsd() * (inv.getDailyRoi() / 100.0) * daysElapsed;
        }

        double totalDeposits = depositRepository.findByUserIdAndStatus(user.getId(), "approved")
                .stream().mapToDouble(d -> d.getAmountUsd()).sum();

        long activeCount = investments.stream().filter(i -> "active".equals(i.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", round2(fresh.getBalance()));
        result.put("invested", round2(invested));
        result.put("earnings", round2(earnings));
        result.put("total_deposits", round2(totalDeposits));
        result.put("active_investments", activeCount);
        return result;
    }

    @GetMapping("/chart")
    public List<Map<String, Object>> chart(@AuthenticationPrincipal User user) {
        List<Investment> investments = investmentRepository.findByUserId(user.getId());
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        double balance = fresh.getBalance();
        double invested = investments.stream()
                .filter(i -> "active".equals(i.getStatus()))
                .mapToDouble(Investment::getAmountUsd)
                .sum();
        double base = balance + invested;

        List<Map<String, Object>> points = new java.util.ArrayList<>();
        Instant today = Instant.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneOffset.UTC);

        for (int i = 30; i >= 0; i--) {
            Instant d = today.minus(i, java.time.temporal.ChronoUnit.DAYS);
            double factor = 1 + (30 - i) * 0.006 + (0.02 * ((i % 5) - 2) / 10.0);
            double val = base > 0 ? Math.max(0, base * factor * 0.85) : 250 + (30 - i) * 12;
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", fmt.format(d));
            point.put("value", round2(val));
            points.add(point);
        }
        return points;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
