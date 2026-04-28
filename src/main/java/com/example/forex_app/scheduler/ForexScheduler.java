package com.example.forex_app.scheduler;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import com.example.forex_app.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForexScheduler {

    private final ExchangeRateMapper exchangeRateMapper;
    private final AlertService alertService;

    private static final String FRANKFURTER_URL =
        "https://api.frankfurter.app/latest?from=USD&to=JPY,EUR";

    // 30分ごとに実行
    @Scheduled(fixedRate = 1800000)
    public void fetchAndSaveRates() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(
                FRANKFURTER_URL, Map.class);

            if (response == null || !response.containsKey("rates")) {
                log.warn("Frankfurter APIからレートを取得できませんでした");
                return;
            }

            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            String base = (String) response.get("base");

            for (Map.Entry<String, Object> entry : rates.entrySet()) {
                ExchangeRate rate = new ExchangeRate();
                rate.setBaseCurrency(base);
                rate.setTargetCurrency(entry.getKey());
                rate.setRate(new BigDecimal(entry.getValue().toString()));
                rate.setFetchedAt(LocalDateTime.now());
                exchangeRateMapper.insert(rate);
            }

            // 30日より古いデータを削除
            exchangeRateMapper.deleteOlderThan30Days();

            // アラートチェック
            alertService.checkAlerts();

            log.info("レート取得完了: {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error("レート取得エラー: {}", e.getMessage());
        }
    }
}