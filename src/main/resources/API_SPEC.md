# 為替アプリ API仕様書
バージョン: 2.0
最終更新: 2026-04-30

---

## 共通仕様
- Base URL: /api
- Content-Type: application/json
- 文字コード: UTF-8
- 対応通貨: USD（米ドル）/ JPY（日本円）/ EUR（ユーロ）

---

## 1. 最新レート取得

- Method: GET
- URL: /api/rates
- Request: なし
- Response（正常）:
  {
    "base": "USD",
    "rates": {
      "JPY": 159.21,
      "EUR": 0.85114
    },
    "fetchedAt": "2026-04-30T09:00:00"
  }
- Response（DBが空の場合）:
  {
    "base": "USD",
    "rates": {},
    "fetchedAt": null
  }

---

## 2. 通貨換算

- Method: GET
- URL: /api/convert
- Request Parameters:
  - from: 変換元通貨コード（USD / JPY / EUR）
  - to: 変換先通貨コード（USD / JPY / EUR）
  - amount: 換算金額（数値）
- Response（正常）:
  {
    "from": "USD",
    "to": "JPY",
    "amount": 100,
    "result": 15921.0000
  }
- Response（レートなし）:
  {
    "from": "USD",
    "to": "JPY",
    "amount": 100,
    "result": 0
  }
- 換算ロジック:
  - 直接レートあり: amount × rate
  - 逆レートあり: amount ÷ rate
  - クロスレート: JPY↔EUR は USD経由で計算
  - 同通貨: amount をそのまま返す

---

## 3. レート履歴取得

- Method: GET
- URL: /api/rates/history
- Request Parameters:
  - from: 基準通貨（USD）
  - to: 対象通貨（JPY / EUR）
- Response（正常）:
  [
    {
      "id": 1,
      "baseCurrency": "USD",
      "targetCurrency": "JPY",
      "rate": 159.21,
      "fetchedDate": "2026-04-30",
      "fetchedAt": "2026-04-30T09:00:00"
    }
  ]
- Response（データなし）: []
- 仕様:
  - 過去365日分を返す
  - fetchedDate昇順で返す

---

## 4. アラート一覧取得

- Method: GET
- URL: /api/alerts
- Request: なし
- Response:
  [
    {
      "id": 1,
      "baseCurrency": "USD",
      "targetCurrency": "JPY",
      "thresholdRate": 150.00,
      "alertType": "ABOVE",
      "triggered": false,
      "createdAt": "2026-04-30T09:00:00"
    }
  ]
- Response（登録なし）: []

---

## 5. アラート登録

- Method: POST
- URL: /api/alerts
- Request Body:
  {
    "baseCurrency": "USD",
    "targetCurrency": "JPY",
    "thresholdRate": 150.00,
    "alertType": "ABOVE"
  }
- alertType: "ABOVE"（以上）または "BELOW"（以下）
- Response（正常）:
  {
    "message": "アラートを登録しました"
  }

---

## 6. アラート削除

- Method: DELETE
- URL: /api/alerts/{id}
- Request: なし
- Response: 204 No Content

---

## アラート発火仕様

- Schedulerが毎朝9時・起動時に自動チェック
- ABOVE: 最新レート >= 閾値 のとき発火
- BELOW: 最新レート <= 閾値 のとき発火
- 発火済み（triggered=true）のアラートは再チェックしない
- 発火結果は画面上の状態表示（⏳待機中 / ✅発火済）で通知

---

## レート補完仕様

- 起動時・毎朝9時にFrankfurter APIから取得
- DBの最新fetched_dateの翌日から今日までを補完
- 土日・祝日はFrankfurterがデータなし → スキップ（正常）
- 異常値（JPY: 50未満または500超、EUR: 0.5未満または2.0超）はスキップ
- 1日1件（base_currency + target_currency + fetched_date）で保証
- 保持期間: 365日

---

## エラー仕様

| 状況 | 動作 |
|---|---|
| DBが空 | ratesを空で返す（エラーにしない） |
| レートなし | result: 0 を返す |
| Frankfurter API障害 | ログ出力・前回値を保持 |
| 異常値 | ログ出力・スキップ |