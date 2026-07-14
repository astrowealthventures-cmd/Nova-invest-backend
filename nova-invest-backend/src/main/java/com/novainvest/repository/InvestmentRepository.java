package com.novainvest.repository;

import com.novainvest.model.Investment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InvestmentRepository extends MongoRepository<Investment, String> {
    List<Investment> findByUserIdOrderByStartedAtDesc(String userId);
    List<Investment> findByUserId(String userId);
}
