package com.example.forex_app.service;

import com.example.forex_app.mapper.AlertSettingMapper;
import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.AlertSetting;
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
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertSettingMapper alertSettingMapper;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private AlertService alertService;

    private ExchangeRate rate(double r) {
        ExchangeRate e = new ExchangeRate();
        e.setBaseCurrency("USD");
        e.setTargetCurrency("JPY");
        e.setRate(new BigDecimal(String.valueOf(r)));
        e.setFetchedDate(LocalDate.now());
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    private AlertSetting alert(double threshold, String type) {
        AlertSetting a = new AlertSetting();
        a.setId(1L);
        a.setBaseCurrency("USD");
        a.setTargetCurrency("JPY");
        a.setThresholdRate(new BigDecimal(String.valueOf(threshold)));
        a.setAlertType(type);
        a.setTriggered(false);
        return a;
    }

    @Test
    @DisplayName("ABOVE：レートが閾値以上なら発火する")
    void checkAlerts_above_triggers() {
        when(alertSettingMapper.findUntriggered())
            .thenReturn(List.of(alert(150.0, "ABOVE")));
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(159.21));

        alertService.checkAlerts();

        verify(alertSettingMapper, times(1)).updateTriggered(1L);
    }

    @Test
    @DisplayName("ABOVE：レートが閾値未満なら発火しない")
    void checkAlerts_above_notTrigger() {
        when(alertSettingMapper.findUntriggered())
            .thenReturn(List.of(alert(160.0, "ABOVE")));
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(159.21));

        alertService.checkAlerts();

        verify(alertSettingMapper, never()).updateTriggered(any());
    }

    @Test
    @DisplayName("BELOW：レートが閾値以下なら発火する")
    void checkAlerts_below_triggers() {
        when(alertSettingMapper.findUntriggered())
            .thenReturn(List.of(alert(160.0, "BELOW")));
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(159.21));

        alertService.checkAlerts();

        verify(alertSettingMapper, times(1)).updateTriggered(1L);
    }

    @Test
    @DisplayName("BELOW：レートが閾値超なら発火しない")
    void checkAlerts_below_notTrigger() {
        when(alertSettingMapper.findUntriggered())
            .thenReturn(List.of(alert(150.0, "BELOW")));
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(159.21));

        alertService.checkAlerts();

        verify(alertSettingMapper, never()).updateTriggered(any());
    }

    @Test
    @DisplayName("異常：レートがnullなら発火しない")
    void checkAlerts_nullRate_notTrigger() {
        when(alertSettingMapper.findUntriggered())
            .thenReturn(List.of(alert(150.0, "ABOVE")));
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(null);

        alertService.checkAlerts();

        verify(alertSettingMapper, never()).updateTriggered(any());
    }

    @Test
    void createAlert_ABOVE_whenCurrentRateIsGreaterThanOrEqualThreshold_triggersImmediately() {
        AlertSetting newAlert = alert(150.0, "ABOVE");
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(157.165935));

        alertService.createAlert(newAlert);

        verify(alertSettingMapper, times(1)).insert(newAlert);
        verify(alertSettingMapper, times(1)).updateTriggered(1L);
    }

    @Test
    void createAlert_ABOVE_whenCurrentRateIsBelowThreshold_doesNotTrigger() {
        AlertSetting newAlert = alert(160.0, "ABOVE");
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(157.165935));

        alertService.createAlert(newAlert);

        verify(alertSettingMapper, times(1)).insert(newAlert);
        verify(alertSettingMapper, never()).updateTriggered(any());
    }

    @Test
    void createAlert_BELOW_whenCurrentRateIsLessThanOrEqualThreshold_triggersImmediately() {
        AlertSetting newAlert = alert(160.0, "BELOW");
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate(157.165935));

        alertService.createAlert(newAlert);

        verify(alertSettingMapper, times(1)).insert(newAlert);
        verify(alertSettingMapper, times(1)).updateTriggered(1L);
    }

    @Test
    void createAlert_whenLatestRateIsNull_doesNotTrigger() {
        AlertSetting newAlert = alert(150.0, "ABOVE");
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(null);

        alertService.createAlert(newAlert);

        verify(alertSettingMapper, times(1)).insert(newAlert);
        verify(alertSettingMapper, never()).updateTriggered(any());
    }
}
