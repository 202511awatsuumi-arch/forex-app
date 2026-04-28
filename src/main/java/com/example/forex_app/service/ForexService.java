package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForexService {

    private final ExchangeRateMapper exchangeRateMapper;

    // 最新レート取得（DBから）
    public ExchangeRate getLatestRate(String baseCurrency, String targetCurrency) {
        return exchangeRateMapper.findLatest(baseCurrency, targetCurrency);
    }

    // 通貨換算
    public BigDecimal convert(String from, String to, BigDecimal amount) {
        ExchangeRate rate = exchangeRateMapper.findLatest(from, to);
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    // レート履歴取得
    public List<ExchangeRate> getHistory(String baseCurrency, String targetCurrency) {
        return exchangeRateMapper.findHistory(baseCurrency, targetCurrency);
    }
}