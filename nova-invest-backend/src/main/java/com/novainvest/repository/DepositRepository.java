package com.novainvest.repository;

import com.novainvest.model.Deposit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DepositRepository extends MongoRepository<Deposit, String> {
    List<Deposit> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Deposit> findAllByOrderByCreatedAtDesc();
    List<Deposit> findByUserIdAndStatus(String userId, String status);
}
