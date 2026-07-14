package com.novainvest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/")
    public Map<String, String> root() {
        return Map.of("service", "Nova Invest", "status", "online");
    }
}
