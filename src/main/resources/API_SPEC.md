# 為替アプリ API仕様書

## 1. 最新レート取得
- Method: GET
- URL: /api/rates
- Response:
  {
    "base": "USD",
    "rates": { "JPY": 149.50, "EUR": 0.92 },
    "fetchedAt": "2026-04-28T10:00:00"
  }

## 2. 通貨換算
- Method: GET
- URL: /api/convert?from=USD&to=JPY&amount=100
- Response:
  { "from": "USD", "to": "JPY", "amount": 100, "result": 14950.00 }

## 3. レート履歴
- Method: GET
- URL: /api/rates/history?from=USD&to=JPY
- Response:
  [ { "date": "2026-04-28", "rate": 149.50 } ]

## 4. アラート登録
- Method: POST
- URL: /api/alerts
- Request:
  { "baseCurrency": "USD", "targetCurrency": "JPY",
    "thresholdRate": 150.00, "alertType": "ABOVE" }

## 5. アラート削除
- Method: DELETE
- URL: /api/alerts/{id}
- Response: 204 No Content
