package com.example.forex_app.controller;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateController {

    @GetMapping("/rates")
    public Map<String, Object> getLatestRates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("JPY", new BigDecimal("149.50"));
        rates.put("EUR", new BigDecimal("0.92"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("base", "USD");
        response.put("rates", rates);
        response.put("fetchedAt", "2026-04-28T10:00:00");
        return response;
    }

    @GetMapping("/convert")
    public Map<String, Object> convertCurrency(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam int amount) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("amount", amount);
        response.put("result", new BigDecimal("14950.00"));
        return response;
    }

    @GetMapping("/rates/history")
    public List<Map<String, Object>> getRateHistory(
            @RequestParam String from,
            @RequestParam String to) {
        Map<String, Object> day1 = new LinkedHashMap<>();
        day1.put("date", "2026-04-27");
        day1.put("rate", new BigDecimal("148.80"));

        Map<String, Object> day2 = new LinkedHashMap<>();
        day2.put("date", "2026-04-28");
        day2.put("rate", new BigDecimal("149.50"));

        return List.of(day1, day2);
    }

    @PostMapping("/alerts")
    public Map<String, Object> createAlert() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", 1);
        response.put("message", "アラートを登録しました");
        return response;
    }

    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable int id) {
        return ResponseEntity.noContent().build();
    }
}
