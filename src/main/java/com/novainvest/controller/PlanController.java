package com.novainvest.controller;

import com.novainvest.model.Plan;
import com.novainvest.repository.PlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<Plan> listPlans() {
        List<Plan> plans = planRepository.findAll();
        plans.sort(Comparator.comparingDouble(Plan::getMinDeposit));
        return plans;
    }
}
