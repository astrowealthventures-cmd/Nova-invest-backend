package com.novainvest.controller;

import com.novainvest.dto.ScrenshotRequest;
import com.novainvest.model.Screenshot;
import com.novainvest.repository.ScreenshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/screenshots")
public class ScreenshotController {

    @Autowired
    private ScreenshotRepository repository;

    @PostMapping
    public Screenshot save(@RequestBody ScrenshotRequest request) {
        Screenshot screenshot = new Screenshot();
        screenshot.setImageUrl(request.imageUrl());
        screenshot.setPublicId(request.publicId());
        screenshot.setUploadedAt(LocalDateTime.now());
        return repository.save(screenshot);
    }

    @GetMapping
    public List<Screenshot> getAll() {
        return repository.findAll();
    }
}
