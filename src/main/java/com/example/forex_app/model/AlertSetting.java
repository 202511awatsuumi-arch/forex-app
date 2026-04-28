package com.example.forex_app.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertSetting {
    private Long id;
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal thresholdRate;
    private String alertType;
    private boolean triggered;
    private LocalDateTime createdAt;
}