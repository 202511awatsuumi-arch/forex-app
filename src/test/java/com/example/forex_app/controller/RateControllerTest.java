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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("GET /api/rates - latest rates are returned")
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
    @DisplayName("GET /api/rates - empty rates when no latest data")
    void getLatestRates_whenEmpty_returnsEmptyRates() throws Exception {
        when(forexService.getLatestRate("USD", "JPY")).thenReturn(null);
        when(forexService.getLatestRate("USD", "EUR")).thenReturn(null);

        mockMvc.perform(get("/api/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.base").value("USD"))
            .andExpect(jsonPath("$.rates").isEmpty());
    }

    @Test
    @DisplayName("GET /api/convert - USD to JPY returns expected result")
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
    @DisplayName("GET /api/convert - result=0 when no rate exists")
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
    @DisplayName("GET /api/rates/history - history is returned")
    void getRateHistory_returnsHistory() throws Exception {
        List<ExchangeRate> history = List.of(
            dummyRate("USD", "JPY", 158.80),
            dummyRate("USD", "JPY", 159.21)
        );
        when(forexService.getHistory(eq("USD"), eq("JPY"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(history);

        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/rates/history - empty list when no data")
    void getRateHistory_whenEmpty_returnsEmptyList() throws Exception {
        when(forexService.getHistory(eq("USD"), eq("JPY"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/rates/history - date range parameters are accepted and service is called")
    void getRateHistory_withDateRangeParams_returnsHistory() throws Exception {
        List<ExchangeRate> history = List.of(
            dummyRate("USD", "JPY", 158.80),
            dummyRate("USD", "JPY", 159.21)
        );
        when(forexService.getHistory(eq("USD"), eq("JPY"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(history);

        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY")
                .param("fromDate", "2026-04-01")
                .param("toDate", "2026-04-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        verify(forexService).getHistory(eq("USD"), eq("JPY"), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /api/rates/history - FROM>TO returns 400 and service is not called")
    void getRateHistory_whenFromAfterTo_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY")
                .param("fromDate", "2026-05-01")
                .param("toDate", "2026-04-30"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(forexService);
    }

    @Test
    @DisplayName("GET /api/rates/history - future FROM date returns 400 and service is not called")
    void getRateHistory_whenFromDateInFuture_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY")
                .param("fromDate", "2999-01-01"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(forexService);
    }

    @Test
    @DisplayName("GET /api/rates/history - future TO date returns 400 and service is not called")
    void getRateHistory_whenToDateInFuture_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY")
                .param("toDate", "2999-01-01"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(forexService);
    }

    @Test
    @DisplayName("GET /api/rates/history - 366 days range returns 400 and service is not called")
    void getRateHistory_whenRangeExceeds365Days_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/rates/history")
                .param("from", "USD")
                .param("to", "JPY")
                .param("fromDate", "2025-01-01")
                .param("toDate", "2026-01-01"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(forexService);
    }
}
