package com.example.forex_app.scheduler;

import com.example.forex_app.service.AlertService;
import com.example.forex_app.service.ForexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForexScheduler {

    private final ForexService forexService;
    private final AlertService alertService;

    // 起動時・毎朝9時で同じServiceメソッドを呼ぶ
    private void runFill() {
        forexService.fillMissingRatesUntilToday("USD", "JPY");
        forexService.fillMissingRatesUntilToday("USD", "EUR");
        alertService.checkAlerts();
    }

    // アプリ起動時
    @PostConstruct
    public void init() {
        log.info("起動時レート補完開始");
        runFill();
    }

    // 毎朝9時
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledFill() {
        log.info("定期レート補完開始");
        runFill();
    }
}