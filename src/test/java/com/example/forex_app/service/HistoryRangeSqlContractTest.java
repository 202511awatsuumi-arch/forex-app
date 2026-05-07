package com.example.forex_app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryRangeSqlContractTest {

    @Test
    @DisplayName("findHistory SQL uses startDate/endDate placeholders and bounded range predicates")
    void findHistorySql_usesParameterizedDateRange() throws IOException {
        String xml = Files.readString(
            Path.of("src/main/resources/mapper/ExchangeRateMapper.xml"),
            StandardCharsets.UTF_8
        );

        String findHistorySql = xml.substring(
            xml.indexOf("<select id=\"findHistory\""),
            xml.indexOf("</select>", xml.indexOf("<select id=\"findHistory\"")) + "</select>".length()
        );

        assertThat(findHistorySql).contains("#{startDate}");
        assertThat(findHistorySql).contains("#{endDate}");
        assertThat(findHistorySql).contains("fetched_date >= #{startDate}");
        assertThat(findHistorySql).contains("fetched_date &lt;= #{endDate}");
        assertThat(findHistorySql).doesNotContain("DATEADD(DAY, -365, CURRENT_DATE)");
    }
}