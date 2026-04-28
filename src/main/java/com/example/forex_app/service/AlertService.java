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

    // アラート全件取得
    public List<AlertSetting> getAllAlerts() {
        return alertSettingMapper.findAll();
    }

    // アラート登録
    public void createAlert(AlertSetting alertSetting) {
        alertSettingMapper.insert(alertSetting);
    }

    // アラート削除
    public void deleteAlert(Long id) {
        alertSettingMapper.deleteById(id);
    }

    // アラートチェック（Schedulerから呼ばれる）
    public void checkAlerts() {
        List<AlertSetting> alerts = alertSettingMapper.findUntriggered();
        for (AlertSetting alert : alerts) {
            ExchangeRate rate = exchangeRateMapper.findLatest(
                alert.getBaseCurrency(), alert.getTargetCurrency());
            if (rate == null) continue;

            boolean triggered = false;
            if ("ABOVE".equals(alert.getAlertType()) &&
                rate.getRate().compareTo(alert.getThresholdRate()) >= 0) {
                triggered = true;
            } else if ("BELOW".equals(alert.getAlertType()) &&
                rate.getRate().compareTo(alert.getThresholdRate()) <= 0) {
                triggered = true;
            }

            if (triggered) {
                alertSettingMapper.updateTriggered(alert.getId());
                log.info("アラート発火！ {}/{} レート:{} 閾値:{}",
                    alert.getBaseCurrency(), alert.getTargetCurrency(),
                    rate.getRate(), alert.getThresholdRate());
            }
        }
    }
}