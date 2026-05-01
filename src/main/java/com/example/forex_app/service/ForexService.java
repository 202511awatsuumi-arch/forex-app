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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForexService {

    private final ExchangeRateMapper exchangeRateMapper;

    private static final String FRANKFURTER_RANGE_URL =
        "https://api.frankfurter.dev/v2/rates?from={from}&to={to}&base={base}&quotes={quotes}";

    private static final String FXAPI_TODAY_URL =
        "https://fxapi.app/api/{base}/{target}.json";

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
        LocalDate startDate = (latestDate != null) ? latestDate.plusDays(1) : today.minusDays(30);

        try {
            RestTemplate restTemplate = createRestTemplate();

            fetchAndSaveTodayFromFxapi(restTemplate, baseCurrency, targetCurrency, today);

            List<LocalDate> fxapiPastDates = exchangeRateMapper.findBySourceBeforeDate(
                baseCurrency, targetCurrency, "FXAPI", today
            );
            if (fxapiPastDates == null) {
                fxapiPastDates = List.of();
            }
            LocalDate frankfurterStartDate = startDate;
            if (!fxapiPastDates.isEmpty()) {
                LocalDate minFxapiDate = fxapiPastDates.stream().min(LocalDate::compareTo).orElse(startDate);
                if (minFxapiDate.isBefore(frankfurterStartDate)) {
                    frankfurterStartDate = minFxapiDate;
                }
            }

            if (frankfurterStartDate.isBefore(today)) {
                List<Map<String, Object>> response = restTemplate.exchange(
                    FRANKFURTER_RANGE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    frankfurterStartDate.toString(), today.minusDays(1).toString(), baseCurrency, targetCurrency
                ).getBody();

                if (response != null && !response.isEmpty()) {
                    saveFromFrankfurterListResponse(
                        response, baseCurrency, targetCurrency, today, new HashSet<>(fxapiPastDates)
                    );
                }
            }

            exchangeRateMapper.deleteOlderThan365Days();
        } catch (Exception e) {
            log.error("[{}->{}] fill error: {}", baseCurrency, targetCurrency, e.getMessage());
        }
    }

    private void fetchAndSaveTodayFromFxapi(
        RestTemplate restTemplate, String baseCurrency, String targetCurrency, LocalDate today
    ) {
        Map<String, Object> response = restTemplate.exchange(
            FXAPI_TODAY_URL,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {},
            baseCurrency, targetCurrency
        ).getBody();

        if (response == null || response.isEmpty()) {
            return;
        }

        Object baseObj = response.get("base");
        Object targetObj = response.get("target");
        Object rateObj = response.get("rate");
        Object timestampObj = response.get("timestamp");
        if (baseObj == null || targetObj == null || rateObj == null || timestampObj == null) {
            return;
        }
        if (!baseCurrency.equals(baseObj.toString())) {
            return;
        }
        if (!targetCurrency.equals(targetObj.toString())) {
            return;
        }

        LocalDate rateDate = Instant.parse(timestampObj.toString())
            .atZone(ZoneOffset.UTC)
            .toLocalDate();
        if (!today.equals(rateDate)) {
            return;
        }

        BigDecimal rateValue = new BigDecimal(rateObj.toString());
        saveTodayRateFromFxapi(baseCurrency, targetCurrency, rateDate, rateValue);
    }

    private int saveFromFrankfurterListResponse(
        List<Map<String, Object>> responses,
        String baseCurrency,
        String targetCurrency,
        LocalDate today,
        Set<LocalDate> fxapiPastDates
    ) {
        int processed = 0;
        for (Map<String, Object> entry : responses) {
            if (entry == null) {
                continue;
            }

            Object dateObj = entry.get("date");
            Object baseObj = entry.get("base");
            Object quoteObj = entry.get("quote");
            Object rateObj = entry.get("rate");
            if (dateObj == null || baseObj == null || quoteObj == null || rateObj == null) {
                continue;
            }
            if (!baseCurrency.equals(baseObj.toString())) {
                continue;
            }
            if (!targetCurrency.equals(quoteObj.toString())) {
                continue;
            }

            LocalDate rateDate = LocalDate.parse(dateObj.toString());
            if (!rateDate.isBefore(today)) {
                continue;
            }

            BigDecimal rateValue = new BigDecimal(rateObj.toString());
            saveHistoricalRateFromFrankfurter(baseCurrency, targetCurrency, rateDate, rateValue, fxapiPastDates);
            processed++;
        }
        return processed;
    }

    private void saveTodayRateFromFxapi(String baseCurrency, String targetCurrency,
                                        LocalDate rateDate, BigDecimal rateValue) {
        if (!isValidRate(targetCurrency, rateValue)) {
            return;
        }

        if (exchangeRateMapper.existsByCurrencyPairAndDate(baseCurrency, targetCurrency, rateDate)) {
            exchangeRateMapper.updateRateByCurrencyPairAndDate(
                baseCurrency, targetCurrency, rateDate, rateValue, "FXAPI"
            );
            return;
        }

        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(baseCurrency);
        rate.setTargetCurrency(targetCurrency);
        rate.setSource("FXAPI");
        rate.setRate(rateValue);
        rate.setFetchedDate(rateDate);
        rate.setFetchedAt(LocalDateTime.now());
        exchangeRateMapper.insert(rate);
    }

    private void saveHistoricalRateFromFrankfurter(String baseCurrency, String targetCurrency,
                                                   LocalDate rateDate, BigDecimal rateValue,
                                                   Set<LocalDate> fxapiPastDates) {
        if (!isValidRate(targetCurrency, rateValue)) {
            return;
        }

        if (fxapiPastDates.contains(rateDate)) {
            exchangeRateMapper.updateRateByCurrencyPairAndDate(
                baseCurrency, targetCurrency, rateDate, rateValue, "FRANKFURTER"
            );
            return;
        }

        if (exchangeRateMapper.existsByCurrencyPairAndDate(baseCurrency, targetCurrency, rateDate)) {
            return;
        }

        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(baseCurrency);
        rate.setTargetCurrency(targetCurrency);
        rate.setSource("FRANKFURTER");
        rate.setRate(rateValue);
        rate.setFetchedDate(rateDate);
        rate.setFetchedAt(LocalDateTime.now());
        exchangeRateMapper.insert(rate);
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
