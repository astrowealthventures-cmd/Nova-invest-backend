package com.novainvest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudinary.*;
import java.util.HashMap;
import java.util.Map;

import com.cloudinary.Cloudinary;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    private final Cloudinary cloudinary;

    public CloudinaryController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @GetMapping("/signature")
    public Map<String, Object> getSignature() {
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", "astrowealthventures/deposit-receipts");

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("apiKey", cloudinary.config.apiKey);
        response.put("cloudName", cloudinary.config.cloudName);
        response.put("folder", "astrowealthventures/deposit-receipts");
        return response;
    }
}
