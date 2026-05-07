package com.example.forex_app.controller;

import com.example.forex_app.model.ExchangeRate;
import com.example.forex_app.service.ForexService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RateApiController {

    private final ForexService forexService;

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
        if (eur != null) {
            rates.put("EUR", eur.getRate());
        }

        response.put("base", "USD");
        response.put("rates", rates);
        return response;
    }

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

    @GetMapping("/rates/history")
    public List<ExchangeRate> getRateHistory(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        LocalDate today = LocalDate.now();

        LocalDate endDate;
        LocalDate startDate;

        if (fromDate != null && toDate != null) {
            startDate = fromDate;
            endDate = toDate;
        } else if (fromDate != null) {
            startDate = fromDate;
            endDate = today;
        } else if (toDate != null) {
            endDate = toDate;
            startDate = toDate.minusDays(364);
        } else {
            endDate = today;
            startDate = today.minusDays(364);
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        }
        if (startDate.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must not be in the future");
        }
        if (endDate.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toDate must not be in the future");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date range must be 365 days or less");
        }

        return forexService.getHistory(from, to, startDate, endDate);
    }
}
