package com.novainvest.repository;

import com.novainvest.model.Screenshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenshotRepository extends MongoRepository<Screenshot, String> {
}