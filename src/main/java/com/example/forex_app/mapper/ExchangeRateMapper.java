package com.example.forex_app.mapper;

import com.example.forex_app.model.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ExchangeRateMapper {

    // 最新レートを1件取得
    ExchangeRate findLatest(String baseCurrency, String targetCurrency);

    // 過去30日分の履歴を取得
    List<ExchangeRate> findHistory(String baseCurrency, String targetCurrency);

    // レートを保存
    void insert(ExchangeRate exchangeRate);

    // 30日より古いデータを削除
    void deleteOlderThan30Days();
}