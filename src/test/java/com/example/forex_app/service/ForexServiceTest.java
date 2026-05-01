package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForexServiceTest {

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ForexService forexService;

    private ExchangeRate rate(String base, String target, double r) {
        ExchangeRate e = new ExchangeRate();
        e.setBaseCurrency(base);
        e.setTargetCurrency(target);
        e.setRate(new BigDecimal(String.valueOf(r)));
        e.setFetchedDate(LocalDate.now());
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    @Test
    @DisplayName("USD→JPY 直接換算が正しい")
    void convert_usdToJpy_direct() {
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));

        BigDecimal result = forexService.convert("USD", "JPY", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("15921.0000");
    }

    @Test
    @DisplayName("JPY→USD 逆算が正しい")
    void convert_jpyToUsd_reverse() {
        when(exchangeRateMapper.findLatest("JPY", "USD")).thenReturn(null);
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));

        BigDecimal result = forexService.convert("JPY", "USD", new BigDecimal("15921"));

        assertThat(result.doubleValue())
            .isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("同じ通貨は同額を返す")
    void convert_sameCurrency_returnsSame() {
        BigDecimal result = forexService.convert("USD", "USD", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("EUR→JPY クロスレート計算が正しい")
    void convert_eurToJpy_crossRate() {
        when(exchangeRateMapper.findLatest("EUR", "JPY")).thenReturn(null);
        when(exchangeRateMapper.findLatest("JPY", "EUR")).thenReturn(null);
        when(exchangeRateMapper.findLatest("USD", "JPY"))
            .thenReturn(rate("USD", "JPY", 159.21));
        when(exchangeRateMapper.findLatest("USD", "EUR"))
            .thenReturn(rate("USD", "EUR", 0.85114));

        BigDecimal result = forexService.convert("EUR", "JPY", new BigDecimal("100"));

        assertThat(result.doubleValue())
            .isCloseTo(18704.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    @DisplayName("異常：DBが空の時は0を返す")
    void convert_whenNoRate_returnsZero() {
        when(exchangeRateMapper.findLatest("USD", "JPY")).thenReturn(null);
        when(exchangeRateMapper.findLatest("JPY", "USD")).thenReturn(null);

        BigDecimal result = forexService.convert("USD", "JPY", new BigDecimal("100"));

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("最新日付が今日でもFXAPIで当日レートを取得して更新する")
    void fillMissing_whenLatestDateIsToday_doesNotInsert() {
        LocalDate today = LocalDate.now();
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(today);
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(true);

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 160.55,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        verify(exchangeRateMapper, never()).insert(any(ExchangeRate.class));
        verify(exchangeRateMapper).updateRateByCurrencyPairAndDate(
            eq("USD"), eq("JPY"), eq(today), eq(new BigDecimal("160.55")), eq("FXAPI")
        );
    }

    @Test
    @DisplayName("過去未保存レートはFrankfurterでinsertされsource=FRANKFURTERになる")
    void fillMissing_rangeResponse_parsesListEntries() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.minusDays(1);

        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(targetDate.minusDays(1));
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(false);
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", targetDate)).thenReturn(false);

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 160.55,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> responseBody = List.of(
            Map.of(
                "date", targetDate.toString(),
                "base", "USD",
                "quote", "JPY",
                "rate", 159.21
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(responseBody));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(exchangeRateMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
            .anySatisfy(rate -> {
                assertThat(rate.getFetchedDate()).isEqualTo(targetDate);
                assertThat(rate.getRate()).isEqualByComparingTo("159.21");
                assertThat(rate.getSource()).isEqualTo("FRANKFURTER");
            });
    }

    @Test
    @DisplayName("今日のUSD/JPYをFXAPIレスポンスから保存しsource=FXAPIになる")
    void fillMissing_singleResponse_parsesTargetRateNotAmount() {
        LocalDate today = LocalDate.now();
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(today.minusDays(1));
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(false);

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 160.55,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> frankfurterResponse = List.of(
            Map.of(
                "date", today.minusDays(0).toString(),
                "base", "USD",
                "quote", "JPY",
                "rate", 160.55,
                "amount", 107.36
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(frankfurterResponse));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(exchangeRateMapper).insert(captor.capture());
        assertThat(captor.getValue().getFetchedDate()).isEqualTo(today);
        assertThat(captor.getValue().getRate()).isEqualByComparingTo("160.55");
        assertThat(captor.getValue().getSource()).isEqualTo("FXAPI");
    }

    @Test
    @DisplayName("既存日付は保存しない")
    void fillMissing_whenDateAlreadyExists_doesNotInsert() {
        LocalDate today = LocalDate.now();
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(today.minusDays(1));
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(true);

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 159.21,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> responseBody = List.of(
            Map.of(
                "date", today.toString(),
                "base", "USD",
                "quote", "JPY",
                "rate", 159.21,
                "amount", 1.0
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(responseBody));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        verify(exchangeRateMapper, never()).insert(any(ExchangeRate.class));
        verify(exchangeRateMapper).updateRateByCurrencyPairAndDate(
            eq("USD"), eq("JPY"), eq(today), eq(new BigDecimal("159.21")), eq("FXAPI")
        );
    }

    @Test
    @DisplayName("異常値は保存しない")
    void fillMissing_whenRateOutOfRange_doesNotInsert() {
        LocalDate today = LocalDate.now();
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(today.minusDays(1));

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 10.0,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> responseBody = List.of(
            Map.of(
                "date", today.toString(),
                "base", "USD",
                "quote", "JPY",
                "rate", 10.0,
                "amount", 1.0
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(responseBody));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        verify(exchangeRateMapper, never()).insert(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("quoteがtargetCurrencyと不一致のentryは保存しない")
    void fillMissing_whenQuoteIsDifferent_doesNotInsert() {
        LocalDate today = LocalDate.now();
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(today.minusDays(1));
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(false);

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 160.55,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> responseBody = List.of(
            Map.of(
                "date", today.toString(),
                "base", "USD",
                "quote", "EUR",
                "rate", 0.85114
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(responseBody));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        verify(exchangeRateMapper, org.mockito.Mockito.times(1)).insert(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("昨日のsource=FXAPIレコードはFrankfurterで上書きしsource=FRANKFURTERに更新")
    void fillMissing_whenYesterdayFxapiExists_updatesByFrankfurter() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        when(exchangeRateMapper.findLatestDate("USD", "JPY")).thenReturn(yesterday.minusDays(1));
        when(exchangeRateMapper.existsByCurrencyPairAndDate("USD", "JPY", today)).thenReturn(false);
        when(exchangeRateMapper.findBySourceBeforeDate("USD", "JPY", "FXAPI", today))
            .thenReturn(List.of(yesterday));

        ForexService serviceSpy = spy(forexService);
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(serviceSpy).createRestTemplate();

        Map<String, Object> fxapiResponse = Map.of(
            "base", "USD",
            "target", "JPY",
            "rate", 160.55,
            "timestamp", today.toString() + "T12:00:00Z"
        );

        List<Map<String, Object>> responseBody = List.of(
            Map.of(
                "date", yesterday.toString(),
                "base", "USD",
                "quote", "JPY",
                "rate", 159.21
            )
        );

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class),
            any(Object[].class)
        )).thenReturn(ResponseEntity.ok(fxapiResponse), ResponseEntity.ok(responseBody));

        serviceSpy.fillMissingRatesUntilToday("USD", "JPY");

        verify(exchangeRateMapper).updateRateByCurrencyPairAndDate(
            eq("USD"), eq("JPY"), eq(yesterday), eq(new BigDecimal("159.21")), eq("FRANKFURTER")
        );
    }
}
