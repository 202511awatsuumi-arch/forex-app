package com.example.forex_app.controller;

import com.example.forex_app.model.ExchangeRate;
import com.example.forex_app.service.ForexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateApiController.class)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForexService forexService;

    private ExchangeRate dummyRate(String base, String target, double rate) {
        ExchangeRate r = new ExchangeRate();
        r.setId(1L);
        r.setBaseCurrency(base);
        r.setTargetCurrency(target);
        r.setRate(new BigDecimal(String.valueOf(rate)));
        r.setFetchedDate(LocalDate.of(2026, 4, 30));
        r.setFetchedAt(LocalDateTime.of(2026, 4, 30, 9, 0));
        return r;
    }

    @Test
    @DisplayName("GET /api/rates - 正常：最新レートが返る")
    void getLatestRates_returnsRates() throws Exception {
        when(forexService.getLatestRate("USD", "JPY"))
            .thenReturn(dummyRate("USD", "JPY", 159.21));
        when(forexService.getLatestRate("USD", "EUR"))
            .thenReturn(dummyRate("USD", "EUR", 0.85114));

        mockMvc.perform(get("/api/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.base").value("USD"))
            .andExpect(jsonPath("$.rates.JPY").value(159.21))
            .andExpect(jsonPath("$.rates.EUR").value(0.85114));
    }

    @Test
    @DisplayName("GET /api/rates - 異常：DBが空の時はratesが空で返る")
    void getLatestRates_whenEmpty_returnsEmptyRates() throws Exception {
        when(forexService.getLatestRate("USD", "JPY")).thenReturn(null);
        when(forexService.getLatestRate("USD", "EUR")).thenReturn(null);

        mockMvc.perform(get("/api/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.base").value("USD"))
            .andExpect(jsonPath("$.rates").isEmpty());
    }

    @Test
    @DisplayName("GET /api/convert - 正常：USD→JPY換算が正しい")
    void convert_usdToJpy_returnsCorrectResult() throws Exception {
        when(forexService.convert(eq("USD"), eq("JPY"), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("15921.0000"));

        mockMvc.perform(get("/api/convert")
                .param("from", "USD")
                .param("to", "JPY")
                .param("amount", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value("USD"))
            .andExpect(jsonPath("$.to").value("JPY"))
            .andExpect(jsonPath("$.amount").value(100))
            .andExpect(jsonPath("$.result").value(15921.0000));
    }

    @Test
    @DisplayName("GET /api/convert - 異常：レートなしの時はresult=0を返す")
    void convert_whenNoRate_returnsZero() throws Exception {
        when(forexService.convert(eq("USD"), eq("JPY"), any(BigDecimal.class)))
            .thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/api/convert")
                .param("from", "USD")
                .param("to", "JPY")
                .param("amount", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value(0));
    }

    @Test
    @DisplayName("GET /api/rates/history - 正常：履歴が返る")
    void getRateHistory_returnsHistory() throws Exception {
        List<ExchangeRate> history = List.of(
            dummyRate("USD", "JPY", 158.80),
            dummyRate("USD", "JPY", 159.21)
        );
        when(forexService.getHistory("USD", "JPY")).thenReturn(history);

        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/rates/history - 異常：データなしは空配列を返す")
    void getRateHistory_whenEmpty_returnsEmptyList() throws Exception {
        when(forexService.getHistory("USD", "JPY")).thenReturn(List.of());

        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}