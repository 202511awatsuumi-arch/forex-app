package com.example.forex_app.mapper;

import com.example.forex_app.model.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExchangeRateMapper {

    // 最新レートを1件取得
    ExchangeRate findLatest(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency);

    // 最新のfetched_dateを取得
    LocalDate findLatestDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency);

    // 過去365日分の履歴を取得
    List<ExchangeRate> findHistory(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency);

    // 指定日付のレートが存在するか確認
    boolean existsByCurrencyPairAndDate(
        @Param("baseCurrency") String baseCurrency,
        @Param("targetCurrency") String targetCurrency,
        @Param("fetchedDate") LocalDate fetchedDate);

    // レートを保存
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

    // 365日より古いデータを削除
    void deleteOlderThan365Days();
}
