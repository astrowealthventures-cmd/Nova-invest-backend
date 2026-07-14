package com.novainvest.repository;

import com.novainvest.model.Withdrawal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WithdrawalRepository extends MongoRepository<Withdrawal, String> {
    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Withdrawal> findAllByOrderByCreatedAtDesc();
}
