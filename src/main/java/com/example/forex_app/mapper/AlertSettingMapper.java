package com.example.forex_app.mapper;

import com.example.forex_app.model.AlertSetting;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AlertSettingMapper {

    // 全アラート取得
    List<AlertSetting> findAll();

    // アラート登録
    void insert(AlertSetting alertSetting);

    // アラート削除
    void deleteById(Long id);

    // 未発火のアラートを全取得
    List<AlertSetting> findUntriggered();

    // アラートを発火済みに更新
    void updateTriggered(Long id);
}