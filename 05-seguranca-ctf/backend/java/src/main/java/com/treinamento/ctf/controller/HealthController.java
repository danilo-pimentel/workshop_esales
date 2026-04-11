package com.treinamento.ctf.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "SecureShop API");
        result.put("version", "1.0.0");
        result.put("status", "running");

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("auth", "/api/auth/login  /api/auth/register  /api/auth/forgot-password");
        endpoints.put("users", "/api/users/me  /api/users/:id");
        endpoints.put("products", "/api/products  /api/products/search?q=  /api/products/:id/reviews");
        endpoints.put("orders", "/api/orders  /api/orders/:id  /api/orders/:id/apply-coupon");
        endpoints.put("export", "/api/export/:format");
        endpoints.put("admin", "/api/admin/users");
        endpoints.put("logs", "/api/logs");
        endpoints.put("monitor", "/monitor");
        result.put("endpoints", endpoints);

        return ResponseEntity.ok(result);
    }
}
