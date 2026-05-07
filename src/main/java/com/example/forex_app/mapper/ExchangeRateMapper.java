package com.example.forex_app.mapper;

import com.example.forex_app.model.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExchangeRateMapper {

    ExchangeRate findLatest(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency);

    LocalDate findLatestDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency);

    default List<ExchangeRate> findHistory(
        String baseCurrency,
        String targetCurrency) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(364);
        return findHistory(baseCurrency, targetCurrency, startDate, endDate);
    }

    List<ExchangeRate> findHistory(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    boolean existsByCurrencyPairAndDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency,
        @Param("fetchedDate") LocalDate fetchedDate);

    void insert(ExchangeRate exchangeRate);

    void updateRateByCurrencyPairAndDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency,
        @Param("fetchedDate") LocalDate fetchedDate,
        @Param("rate") java.math.BigDecimal rate,
        @Param("source") String source);

    List<LocalDate> findBySourceBeforeDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency,
        @Param("source") String source,
        @Param("beforeDate") LocalDate beforeDate);

    void deleteOlderThan365Days();
}
