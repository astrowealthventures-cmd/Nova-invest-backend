package com.novainvest.controller;

import com.novainvest.model.Transaction;
import com.novainvest.model.User;
import com.novainvest.repository.TransactionRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<Transaction> listTransactions(@AuthenticationPrincipal User user) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
