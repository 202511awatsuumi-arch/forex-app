package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForexService {

    private final ExchangeRateMapper exchangeRateMapper;

    private static final String FRANKFURTER_RANGE_URL =
        "https://api.frankfurter.dev/v2/rates?from={from}&to={to}&base={base}&quotes={quotes}";

    private static final String FRANKFURTER_DATE_URL =
        "https://api.frankfurter.dev/v2/rates?date={date}&base={base}&quotes={quotes}";

    // レート異常値チェック用（1USD基準の妥当な範囲）
    private static final BigDecimal MIN_JPY = new BigDecimal("50");
    private static final BigDecimal MAX_JPY = new BigDecimal("500");
    private static final BigDecimal MIN_EUR = new BigDecimal("0.5");
    private static final BigDecimal MAX_EUR = new BigDecimal("2.0");

    public ExchangeRate getLatestRate(String baseCurrency, String targetCurrency) {
        return exchangeRateMapper.findLatest(baseCurrency, targetCurrency);
    }

    public List<ExchangeRate> getHistory(String baseCurrency, String targetCurrency) {
        return exchangeRateMapper.findHistory(baseCurrency, targetCurrency);
    }

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        if (from.equals(to)) return amount;

        ExchangeRate direct = exchangeRateMapper.findLatest(from, to);
        if (direct != null) {
            return amount.multiply(direct.getRate())
                         .setScale(4, RoundingMode.HALF_UP);
        }

        ExchangeRate reverse = exchangeRateMapper.findLatest(to, from);
        if (reverse != null) {
            return amount.divide(reverse.getRate(), 4, RoundingMode.HALF_UP);
        }

        ExchangeRate usdJpy = exchangeRateMapper.findLatest("USD", "JPY");
        ExchangeRate usdEur = exchangeRateMapper.findLatest("USD", "EUR");

        if (usdJpy != null && usdEur != null) {
            if (from.equals("JPY") && to.equals("EUR")) {
                BigDecimal usd = amount.divide(
                    usdJpy.getRate(), 10, RoundingMode.HALF_UP);
                return usd.multiply(usdEur.getRate())
                           .setScale(4, RoundingMode.HALF_UP);
            }
            if (from.equals("EUR") && to.equals("JPY")) {
                BigDecimal usd = amount.divide(
                    usdEur.getRate(), 10, RoundingMode.HALF_UP);
                return usd.multiply(usdJpy.getRate())
                           .setScale(4, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * DBの最新fetched_dateの翌日から今日までのレートを補完する
     * レスポンスはList形式:
     * [{"date":"2026-04-25","base":"USD","quote":"JPY","rate":159.51}, ...]
     * 土日・祝日はFrankfurterがデータを返さないためスキップ（正常）
     */
    public void fillMissingRatesUntilToday(
            String baseCurrency, String targetCurrency) {

        LocalDate today = LocalDate.now();
        LocalDate latestDate = exchangeRateMapper.findLatestDate(
            baseCurrency, targetCurrency);

        LocalDate startDate = (latestDate != null)
            ? latestDate.plusDays(1)
            : today.minusDays(30);

        if (!startDate.isBefore(today.plusDays(1))) {
            log.info("[{}→{}] 補完不要: 最新={}",
                baseCurrency, targetCurrency, latestDate);
            return;
        }

        log.info("[{}→{}] 補完開始: {} ～ {}",
            baseCurrency, targetCurrency, startDate, today);

        try {
            RestTemplate restTemplate = new RestTemplate();

            List<Map<String, Object>> rateList;

            if (startDate.equals(today)) {
                // 単日取得
                rateList = restTemplate.exchange(
                    FRANKFURTER_DATE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    today.toString(), baseCurrency, targetCurrency
                ).getBody();
            } else {
                // 期間取得
                rateList = restTemplate.exchange(
                    FRANKFURTER_RANGE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    startDate.toString(), today.toString(),
                    baseCurrency, targetCurrency
                ).getBody();
            }

            if (rateList == null || rateList.isEmpty()) {
                log.warn("[{}→{}] レートデータなし",
                    baseCurrency, targetCurrency);
                return;
            }

            for (Map<String, Object> entry : rateList) {
                LocalDate rateDate = LocalDate.parse(
                    entry.get("date").toString());
                BigDecimal rateValue = new BigDecimal(
                    entry.get("rate").toString());

                // 異常値チェック
                if (!isValidRate(targetCurrency, rateValue)) {
                    log.warn("異常値スキップ: {}/{} {} = {}",
                        baseCurrency, targetCurrency, rateDate, rateValue);
                    continue;
                }

                // 重複チェック
                if (exchangeRateMapper.existsByCurrencyPairAndDate(
                        baseCurrency, targetCurrency, rateDate)) {
                    log.info("保存済みスキップ: {}/{} {}",
                        baseCurrency, targetCurrency, rateDate);
                    continue;
                }

                ExchangeRate rate = new ExchangeRate();
                rate.setBaseCurrency(baseCurrency);
                rate.setTargetCurrency(targetCurrency);
                rate.setRate(rateValue);
                rate.setFetchedDate(rateDate);
                rate.setFetchedAt(LocalDateTime.now());
                exchangeRateMapper.insert(rate);
                log.info("保存完了: {}/{} {} = {}",
                    baseCurrency, targetCurrency, rateDate, rateValue);
            }

            exchangeRateMapper.deleteOlderThan365Days();

        } catch (Exception e) {
            log.error("[{}→{}] レート補完エラー: {}",
                baseCurrency, targetCurrency, e.getMessage());
        }
    }

    // 異常値チェック（通貨ごとの妥当な範囲を検証）
    private boolean isValidRate(String targetCurrency, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return false;
        return switch (targetCurrency) {
            case "JPY" -> rate.compareTo(MIN_JPY) >= 0
                       && rate.compareTo(MAX_JPY) <= 0;
            case "EUR" -> rate.compareTo(MIN_EUR) >= 0
                       && rate.compareTo(MAX_EUR) <= 0;
            default -> rate.compareTo(BigDecimal.ZERO) > 0;
        };
    }
}