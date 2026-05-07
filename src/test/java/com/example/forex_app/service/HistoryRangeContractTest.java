package com.example.forex_app.service;

import com.example.forex_app.mapper.ExchangeRateMapper;
import com.example.forex_app.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryRangeContractTest {

    @Test
    @DisplayName("Service exposes date-range getHistory(base,target,fromDate,toDate)")
    void forexService_exposesDateRangeHistoryMethod() {
        Method method = Arrays.stream(ForexService.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("getHistory"))
            .filter(m -> Arrays.equals(
                m.getParameterTypes(),
                new Class<?>[] {String.class, String.class, LocalDate.class, LocalDate.class}
            ))
            .findFirst()
            .orElse(null);

        assertThat(method).as("missing getHistory(String,String,LocalDate,LocalDate)").isNotNull();
    }

    @Test
    @DisplayName("Mapper exposes date-range findHistory(base,target,startDate,endDate)")
    void exchangeRateMapper_exposesDateRangeFindHistoryMethod() {
        Method method = Arrays.stream(ExchangeRateMapper.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("findHistory"))
            .filter(m -> Arrays.equals(
                m.getParameterTypes(),
                new Class<?>[] {String.class, String.class, LocalDate.class, LocalDate.class}
            ))
            .findFirst()
            .orElse(null);

        assertThat(method).as("missing findHistory(String,String,LocalDate,LocalDate)").isNotNull();
    }

    @Test
    @DisplayName("Date range contract: FROM/TO specified -> start=FROM, end=TO")
    void dateRangeContract_whenBothSpecified_usesGivenBounds() throws Exception {
        ForexService service = new ForexService(new RecordingMapper());
        Method method = findDateRangeHistoryMethod();

        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        method.invoke(service, "USD", "JPY", fromDate, toDate);

        RecordingMapper mapper = (RecordingMapper) getMapperField(service);
        assertThat(mapper.startDate).isEqualTo(fromDate);
        assertThat(mapper.endDate).isEqualTo(toDate);
    }

    @Test
    @DisplayName("Date range contract: resolved range is passed through to mapper")
    void dateRangeContract_whenResolvedRangeProvided_passesBoundsToMapper() throws Exception {
        ForexService service = new ForexService(new RecordingMapper());
        Method method = findDateRangeHistoryMethod();

        LocalDate fromDate = LocalDate.of(2025, 2, 2);
        LocalDate toDate = LocalDate.of(2026, 2, 1);

        method.invoke(service, "USD", "JPY", fromDate, toDate);

        RecordingMapper mapper = (RecordingMapper) getMapperField(service);
        assertThat(mapper.startDate).isEqualTo(fromDate);
        assertThat(mapper.endDate).isEqualTo(toDate);
    }

    private Method findDateRangeHistoryMethod() {
        Method method = Arrays.stream(ForexService.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("getHistory"))
            .filter(m -> Arrays.equals(
                m.getParameterTypes(),
                new Class<?>[] {String.class, String.class, LocalDate.class, LocalDate.class}
            ))
            .findFirst()
            .orElse(null);

        assertThat(method).as("missing getHistory(String,String,LocalDate,LocalDate)").isNotNull();
        method.setAccessible(true);
        return method;
    }

    private Object getMapperField(ForexService service) throws Exception {
        var field = ForexService.class.getDeclaredField("exchangeRateMapper");
        field.setAccessible(true);
        return field.get(service);
    }

    private static class RecordingMapper implements ExchangeRateMapper {
        private LocalDate startDate;
        private LocalDate endDate;

        @Override
        public ExchangeRate findLatest(String baseCurrency, String targetCurrency) {
            return null;
        }

        @Override
        public LocalDate findLatestDate(String baseCurrency, String targetCurrency) {
            return null;
        }

        @Override
        public List<ExchangeRate> findHistory(String baseCurrency, String targetCurrency) {
            return new ArrayList<>();
        }

        @SuppressWarnings("unused")
        public List<ExchangeRate> findHistory(String baseCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return new ArrayList<>();
        }

        @Override
        public boolean existsByCurrencyPairAndDate(String baseCurrency, String targetCurrency, LocalDate fetchedDate) {
            return false;
        }

        @Override
        public void insert(ExchangeRate exchangeRate) {
        }

        @Override
        public void updateRateByCurrencyPairAndDate(String baseCurrency, String targetCurrency, LocalDate fetchedDate, java.math.BigDecimal rate, String source) {
        }

        @Override
        public List<LocalDate> findBySourceBeforeDate(String baseCurrency, String targetCurrency, String source, LocalDate beforeDate) {
            return List.of();
        }

        @Override
        public void deleteOlderThan365Days() {
        }
    }
}
