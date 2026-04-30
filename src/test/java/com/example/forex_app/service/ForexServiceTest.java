package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForexServiceTest {

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ForexService forexService;

    private ExchangeRate rate(String base, String target, double r) {
        ExchangeRate e = new ExchangeRate();
        e.setBaseCurrency(base);
        e.setTargetCurrency(target);
        e.setRate(new BigDecimal(String.valueOf(r)));
        e.setFetchedDate(LocalDate.now());
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    @Test
    @DisplayName("USD→JPY 直接換算が正しい")
    void convert_usdToJpy_direct() {
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));

        BigDecimal result = forexService.convert(
            "USD", "JPY", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("15921.0000");
    }

    @Test
    @DisplayName("JPY→USD 逆算が正しい")
    void convert_jpyToUsd_reverse() {
        when(exchangeRateMapper.findLatest("JPY", "USD")).thenReturn(null);
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));

        BigDecimal result = forexService.convert(
            "JPY", "USD", new BigDecimal("15921"));

        assertThat(result.doubleValue())
            .isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("同じ通貨は同額を返す")
    void convert_sameCurrency_returnsSame() {
        BigDecimal result = forexService.convert(
            "USD", "USD", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("EUR→JPY クロスレート計算が正しい")
    void convert_eurToJpy_crossRate() {
        when(exchangeRateMapper.findLatest("EUR", "JPY")).thenReturn(null);
        when(exchangeRateMapper.findLatest("JPY", "EUR")).thenReturn(null);
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));
        when(exchangeRateMapper.findLatest("USD", "EUR"))
            .thenReturn(rate("USD", "EUR", 0.85114));

        BigDecimal result = forexService.convert(
            "EUR", "JPY", new BigDecimal("100"));

        // 100EUR ÷ 0.85114 × 159.21 ≒ 18704円
        assertThat(result.doubleValue())
            .isCloseTo(18704.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    @DisplayName("異常：DBが空の時は0を返す")
    void convert_whenNoRate_returnsZero() {
        when(exchangeRateMapper.findLatest("USD", "JPY")).thenReturn(null);
        when(exchangeRateMapper.findLatest("JPY", "USD")).thenReturn(null);

        BigDecimal result = forexService.convert(
            "USD", "JPY", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("0");
    }
}