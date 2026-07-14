package com.novainvest.controller;

import com.novainvest.dto.WithdrawRequest;
import com.novainvest.model.Transaction;
import com.novainvest.model.User;
import com.novainvest.model.Withdrawal;
import com.novainvest.repository.TransactionRepository;
import com.novainvest.repository.UserRepository;
import com.novainvest.repository.WithdrawalRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WithdrawalController(WithdrawalRepository withdrawalRepository, TransactionRepository transactionRepository,
                                 UserRepository userRepository) {
        this.withdrawalRepository = withdrawalRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Withdrawal createWithdrawal(@Valid @RequestBody WithdrawRequest body, @AuthenticationPrincipal User user) {
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.getAmountUsd() > fresh.getBalance()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        // lock funds by decrementing balance immediately (matches FastAPI behavior)
        fresh.setBalance(fresh.getBalance() - body.getAmountUsd());
        userRepository.save(fresh);

        Withdrawal wd = new Withdrawal();
        wd.setId(UUID.randomUUID().toString());
        wd.setUserId(user.getId());
        wd.setAsset(body.getAsset());
        wd.setAmountUsd(body.getAmountUsd());
        wd.setWalletAddress(body.getWalletAddress());
        wd.setStatus("pending");
        wd.setCreatedAt(Instant.now().toString());
        withdrawalRepository.save(wd);

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setUserId(user.getId());
        tx.setType("withdraw");
        tx.setAmountUsd(body.getAmountUsd());
        tx.setAsset(body.getAsset());
        tx.setStatus("pending");
        tx.setRefId(wd.getId());
        tx.setCreatedAt(Instant.now().toString());
        transactionRepository.save(tx);

        return wd;
    }

    @GetMapping
    public List<Withdrawal> listWithdrawals(@AuthenticationPrincipal User user) {
        if ("admin".equals(user.getRole())) {
            return withdrawalRepository.findAllByOrderByCreatedAtDesc();
        }
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
