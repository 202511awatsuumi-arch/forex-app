package com.example.forex_app.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExchangeRate {
    private Long id;
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private LocalDate fetchedDate;
    private LocalDateTime fetchedAt;
}