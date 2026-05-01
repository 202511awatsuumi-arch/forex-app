package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    public void fillMissingRatesUntilToday(String baseCurrency, String targetCurrency) {
        LocalDate today = LocalDate.now();
        LocalDate latestDate = exchangeRateMapper.findLatestDate(baseCurrency, targetCurrency);

        LocalDate startDate = (latestDate != null)
            ? latestDate.plusDays(1)
            : today.minusDays(30);

        if (!startDate.isBefore(today.plusDays(1))) {
            log.info("[{}→{}] 補完不要: 最新={}", baseCurrency, targetCurrency, latestDate);
            return;
        }

        log.info("[{}→{}] 補完開始: {} ～ {}", baseCurrency, targetCurrency, startDate, today);

        try {
            RestTemplate restTemplate = createRestTemplate();
            Map<String, Object> response;

            if (startDate.equals(today)) {
                response = restTemplate.exchange(
                    FRANKFURTER_DATE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    today.toString(), baseCurrency, targetCurrency
                ).getBody();
            } else {
                response = restTemplate.exchange(
                    FRANKFURTER_RANGE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    startDate.toString(), today.toString(), baseCurrency, targetCurrency
                ).getBody();
            }

            if (response == null || response.isEmpty()) {
                log.warn("[{}→{}] レートデータなし", baseCurrency, targetCurrency);
                return;
            }

            int processed = startDate.equals(today)
                ? saveFromSingleResponse(response, baseCurrency, targetCurrency)
                : saveFromRangeResponse(response, baseCurrency, targetCurrency);

            if (processed == 0) {
                log.warn("[{}→{}] レートデータなし", baseCurrency, targetCurrency);
            }

            exchangeRateMapper.deleteOlderThan365Days();

        } catch (Exception e) {
            log.error("[{}→{}] レート補完エラー: {}", baseCurrency, targetCurrency, e.getMessage());
        }
    }

    private int saveFromSingleResponse(Map<String, Object> response, String baseCurrency, String targetCurrency) {
        Object dateObj = response.get("date");
        Map<String, Object> rates = asStringObjectMap(response.get("rates"));
        if (dateObj == null || rates == null) {
            return 0;
        }

        Object rateObj = rates.get(targetCurrency);
        if (rateObj == null) {
            return 0;
        }

        LocalDate rateDate = LocalDate.parse(dateObj.toString());
        BigDecimal rateValue = new BigDecimal(rateObj.toString());
        saveRateIfNeeded(baseCurrency, targetCurrency, rateDate, rateValue);
        return 1;
    }

    private int saveFromRangeResponse(Map<String, Object> response, String baseCurrency, String targetCurrency) {
        Map<String, Object> ratesByDate = asStringObjectMap(response.get("rates"));
        if (ratesByDate == null || ratesByDate.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (Map.Entry<String, Object> dayEntry : ratesByDate.entrySet()) {
            Map<String, Object> rates = asStringObjectMap(dayEntry.getValue());
            if (rates == null) {
                continue;
            }
            Object rateObj = rates.get(targetCurrency);
            if (rateObj == null) {
                continue;
            }

            LocalDate rateDate = LocalDate.parse(dayEntry.getKey());
            BigDecimal rateValue = new BigDecimal(rateObj.toString());
            saveRateIfNeeded(baseCurrency, targetCurrency, rateDate, rateValue);
            processed++;
        }
        return processed;
    }

    private void saveRateIfNeeded(String baseCurrency, String targetCurrency,
                                  LocalDate rateDate, BigDecimal rateValue) {
        if (!isValidRate(targetCurrency, rateValue)) {
            log.warn("異常値スキップ: {}/{} {} = {}",
                baseCurrency, targetCurrency, rateDate, rateValue);
            return;
        }

        if (exchangeRateMapper.existsByCurrencyPairAndDate(baseCurrency, targetCurrency, rateDate)) {
            log.info("保存済みスキップ: {}/{} {}", baseCurrency, targetCurrency, rateDate);
            return;
        }

        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(baseCurrency);
        rate.setTargetCurrency(targetCurrency);
        rate.setRate(rateValue);
        rate.setFetchedDate(rateDate);
        rate.setFetchedAt(LocalDateTime.now());
        exchangeRateMapper.insert(rate);
        log.info("保存完了: {}/{} {} = {}", baseCurrency, targetCurrency, rateDate, rateValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

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