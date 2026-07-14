package com.novainvest.controller;

import com.novainvest.dto.AdminActionRequest;
import com.novainvest.dto.DepositRequest;
import com.novainvest.model.Deposit;
import com.novainvest.model.Transaction;
import com.novainvest.model.User;
import com.novainvest.repository.DepositRepository;
import com.novainvest.repository.TransactionRepository;
import com.novainvest.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DepositController {

    private final DepositRepository depositRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${app.wallets.btc}")
    private String btcWallet;
    @Value("${app.wallets.eth}")
    private String ethWallet;
    @Value("${app.wallets.usdt}")
    private String usdtWallet;

    public DepositController(DepositRepository depositRepository, TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.depositRepository = depositRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/deposits/wallets")
    public Map<String, String> getWallets() {
        Map<String, String> wallets = new LinkedHashMap<>();
        wallets.put("BTC", btcWallet);
        wallets.put("ETH", ethWallet);
        wallets.put("USDT", usdtWallet);
        return wallets;
    }

    @PostMapping("/deposits")
    public Deposit createDeposit(@Valid @RequestBody DepositRequest body, @AuthenticationPrincipal User user) {
        Deposit deposit = new Deposit();
        deposit.setId(UUID.randomUUID().toString());
        deposit.setUserId(user.getId());
        deposit.setAsset(body.getAsset());
        deposit.setAmountUsd(body.getAmountUsd());
        deposit.setTxHash(body.getTxHash());
        deposit.setStatus("pending");
        deposit.setCreatedAt(Instant.now().toString());
        depositRepository.save(deposit);

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setUserId(user.getId());
        tx.setType("deposit");
        tx.setAmountUsd(body.getAmountUsd());
        tx.setAsset(body.getAsset());
        tx.setStatus("pending");
        tx.setRefId(deposit.getId());
        tx.setCreatedAt(Instant.now().toString());
        transactionRepository.save(tx);

        return deposit;
    }

    @GetMapping("/deposits")
    public List<Deposit> listDeposits(@AuthenticationPrincipal User user) {
        if ("admin".equals(user.getRole())) {
            return depositRepository.findAllByOrderByCreatedAtDesc();
        }
        return depositRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @PostMapping("/admin/deposits/{depositId}")
    public Map<String, Object> adminDepositAction(@PathVariable String depositId,
                                                    @Valid @RequestBody AdminActionRequest body) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));

        if (!"pending".equals(deposit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already processed");
        }

        String newStatus = "approve".equals(body.getAction()) ? "approved" : "rejected";
        deposit.setStatus(newStatus);
        deposit.setProcessedAt(Instant.now().toString());
        depositRepository.save(deposit);

        transactionRepository.findByRefId(depositId).ifPresent(tx -> {
            tx.setStatus(newStatus);
            transactionRepository.save(tx);
        });

        if ("approve".equals(body.getAction())) {
            User u = userRepository.findById(deposit.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            u.setBalance(u.getBalance() + deposit.getAmountUsd());
            userRepository.save(u);
        }

        return Map.of("ok", true, "status", newStatus);
    }
}
