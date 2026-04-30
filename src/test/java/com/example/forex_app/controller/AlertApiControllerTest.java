package com.example.forex_app.controller;

import com.example.forex_app.model.AlertSetting;
import com.example.forex_app.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertApiController.class)
class AlertApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Test
    @DisplayName("GET /api/alerts - 正常：アラート一覧が返る")
    void getAlerts_returnsAlertList() throws Exception {
        AlertSetting alert = new AlertSetting();
        alert.setId(1L);
        alert.setBaseCurrency("USD");
        alert.setTargetCurrency("JPY");
        alert.setThresholdRate(new BigDecimal("150.00"));
        alert.setAlertType("ABOVE");
        alert.setTriggered(false);

        org.mockito.Mockito.when(alertService.getAllAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].baseCurrency").value("USD"))
            .andExpect(jsonPath("$[0].targetCurrency").value("JPY"))
            .andExpect(jsonPath("$[0].alertType").value("ABOVE"))
            .andExpect(jsonPath("$[0].triggered").value(false));
    }

    @Test
    @DisplayName("GET /api/alerts - 正常：登録なしは空配列を返す")
    void getAlerts_whenEmpty_returnsEmptyList() throws Exception {
        org.mockito.Mockito.when(alertService.getAllAlerts()).thenReturn(List.of());

        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/alerts - 正常：アラートが登録できる")
    void createAlert_returnsSuccess() throws Exception {
        AlertSetting alert = new AlertSetting();
        alert.setBaseCurrency("USD");
        alert.setTargetCurrency("JPY");
        alert.setThresholdRate(new BigDecimal("160.00"));
        alert.setAlertType("ABOVE");

        mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alert)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("アラートを登録しました"));
    }

    @Test
    @DisplayName("DELETE /api/alerts/{id} - 正常：204が返る")
    void deleteAlert_returns204() throws Exception {
        mockMvc.perform(delete("/api/alerts/1"))
            .andExpect(status().isNoContent());
    }
}