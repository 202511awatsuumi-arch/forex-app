package com.example.forex_app.controller;

import com.example.forex_app.model.AlertSetting;
import com.example.forex_app.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlertApiController {

    private final AlertService alertService;

    @GetMapping("/alerts")
    public List<AlertSetting> getAlerts() {
        return alertService.getAllAlerts();
    }

    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> createAlert(
            @RequestBody AlertSetting alertSetting) {
        alertService.createAlert(alertSetting);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "アラートを登録しました");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}
