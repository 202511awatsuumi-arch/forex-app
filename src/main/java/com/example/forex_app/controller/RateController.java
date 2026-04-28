package com.example.forex_app.controller;

import com.example.forex_app.model.AlertSetting;
import com.example.forex_app.model.ExchangeRate;
import com.example.forex_app.service.AlertService;
import com.example.forex_app.service.ForexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RateController {

    private final ForexService forexService;
    private final AlertService alertService;

    // 最新レート取得（本物）
    @GetMapping("/rates")
    public Map<String, Object> getLatestRates() {
        Map<String, Object> response = new HashMap<>();
        ExchangeRate jpy = forexService.getLatestRate("USD", "JPY");
        ExchangeRate eur = forexService.getLatestRate("USD", "EUR");

        Map<String, Object> rates = new HashMap<>();
        if (jpy != null) {
            rates.put("JPY", jpy.getRate());
            response.put("fetchedAt", jpy.getFetchedAt());
        }
        if (eur != null) rates.put("EUR", eur.getRate());

        response.put("base", "USD");
        response.put("rates", rates);
        return response;
    }

    // 通貨換算（本物）
    @GetMapping("/convert")
    public Map<String, Object> convertCurrency(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        BigDecimal result = forexService.convert(from, to, amount);
        Map<String, Object> response = new HashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("amount", amount);
        response.put("result", result);
        return response;
    }

    // レート履歴（本物）
    @GetMapping("/rates/history")
    public List<ExchangeRate> getRateHistory(
            @RequestParam String from,
            @RequestParam String to) {
        return forexService.getHistory(from, to);
    }

    // アラート一覧
    @GetMapping("/alerts")
    public List<AlertSetting> getAlerts() {
        return alertService.getAllAlerts();
    }

    // アラート登録（本物）
    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> createAlert(
            @RequestBody AlertSetting alertSetting) {
        alertService.createAlert(alertSetting);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "アラートを登録しました");
        return ResponseEntity.ok(response);
    }

    // アラート削除（本物）
    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}