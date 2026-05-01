package com.example.forex_app.service;

import com.example.forex_app.mapper.AlertSettingMapper;
import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.AlertSetting;
import com.example.forex_app.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertSettingMapper alertSettingMapper;
    private final ExchangeRateMapper exchangeRateMapper;

    public List<AlertSetting> getAllAlerts() {
        return alertSettingMapper.findAll();
    }

    public void createAlert(AlertSetting alertSetting) {
        alertSettingMapper.insert(alertSetting);
        evaluateAndTriggerIfNeeded(alertSetting);
    }

    public void deleteAlert(Long id) {
        alertSettingMapper.deleteById(id);
    }

    public void checkAlerts() {
        List<AlertSetting> alerts = alertSettingMapper.findUntriggered();
        for (AlertSetting alert : alerts) {
            evaluateAndTriggerIfNeeded(alert);
        }
    }

    private void evaluateAndTriggerIfNeeded(AlertSetting alert) {
        ExchangeRate latestRate = exchangeRateMapper.findLatest(
            alert.getBaseCurrency(), alert.getTargetCurrency());
        if (latestRate == null || latestRate.getRate() == null) {
            return;
        }

        boolean triggered = false;
        if ("ABOVE".equals(alert.getAlertType())
            && latestRate.getRate().compareTo(alert.getThresholdRate()) >= 0) {
            triggered = true;
        } else if ("BELOW".equals(alert.getAlertType())
            && latestRate.getRate().compareTo(alert.getThresholdRate()) <= 0) {
            triggered = true;
        }

        if (triggered) {
            alertSettingMapper.updateTriggered(alert.getId());
            log.info("Alert triggered: {}/{} rate={} threshold={}",
                alert.getBaseCurrency(), alert.getTargetCurrency(),
                latestRate.getRate(), alert.getThresholdRate());
        }
    }
}
