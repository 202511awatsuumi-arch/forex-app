# 為替アプリ

## 1. アプリ概要
Spring Boot + MyBatis + Thymeleaf + H2 で構成した為替レート管理アプリです。  
USD/JPY・USD/EUR を中心に、レート取得・換算・履歴参照・アラート管理を行います。

## 2. 使用技術
- Java 17
- Spring Boot 3.x
- MyBatis
- Thymeleaf
- H2 Database
- JUnit 5 / Mockito / MockMvc
- Maven Wrapper (`mvnw`)

## 3. 主な機能
- 最新レート表示（USD基準）
- 通貨換算（USD / JPY / EUR）
- レート履歴表示（過去365日）
- アラート登録・一覧・削除（ABOVE / BELOW）
- 起動時・定期実行でのレート補完（不足日を補完）

## 4. 画面一覧
- `/` : ホーム
- `/convert` : 通貨換算画面（`/api/convert` 連携）
- `/history` : 履歴画面（`/api/rates/history` 連携）
- `/alerts` : アラート画面（`/api/alerts` GET/POST/DELETE 連携）

## 5. API一覧
詳細は [API_SPEC.md](src/main/resources/API_SPEC.md)（v2.0）を参照。

- `GET /api/rates` : 最新レート取得
- `GET /api/convert` : 通貨換算
- `GET /api/rates/history` : レート履歴取得
- `GET /api/alerts` : アラート一覧取得
- `POST /api/alerts` : アラート登録
- `DELETE /api/alerts/{id}` : アラート削除

## 6. DB設計概要
主テーブルは以下の2つです。

- `exchange_rate`
  - レート履歴を日次で保持
  - `base_currency + target_currency + fetched_date` に UNIQUE 制約（1日1件保証）
- `alert_setting`
  - 閾値アラート設定を保持
  - `triggered` で発火済み状態を管理

### DB運用方針
- ローカル開発は H2 Database を利用
- 本番アプリ公開は Render を利用
- 本番DBは Neon PostgreSQL を利用
- Render Free PostgreSQL は30日で期限切れになるため、本番データ保持には利用しない
- DB接続情報は Render の環境変数で管理し、ソースコードに直書きしない

### レート取得・補完仕様
- 起動時に不足分を補完
- 毎朝9時に定期取得
- 当日レートが既にDBにある場合は再保存しない
- 前回取得日から空いた日付分を補完
- `base_currency + target_currency + fetched_date` の UNIQUE 制約で1日1レートをDBで保証

DDLは [schema.sql](src/main/resources/schema.sql) を参照。

## 7. 起動方法
1. リポジトリルートへ移動
2. 以下を実行

```bash
./mvnw spring-boot:run
```

Windows PowerShell の場合:

```powershell
.\mvnw spring-boot:run
```

起動後、ブラウザで `http://localhost:8080` を開いてください。

## 8. テスト実行方法
```bash
./mvnw test
```

Windows PowerShell の場合:

```powershell
.\mvnw test
```

## 9. TDDで実装した内容
- Service層
  - `ForexServiceTest`: 換算ロジック（直接・逆・クロス・同通貨・レートなし）
  - `AlertServiceTest`: 発火ロジック（ABOVE/BELOW・非発火・nullレート）
  - DB運用仕様の追加テスト
    - 最新日が当日の場合は補完不要
    - 既存日付は保存スキップ
    - 異常値は保存スキップ
- Controller層
  - MockMvc による API 契約テスト（`/api/rates`, `/api/convert`, `/api/rates/history`, `/api/alerts`）
  - HTTPステータス・JSON項目・代表ケースを検証

## 10. 今後の改善点
- 入力バリデーション強化（API/画面の両面）
- 例外ハンドリングの共通化とエラーレスポンス標準化
- テストの拡充（異常系・境界値・E2E）
- UI/UX 改善（操作フィードバック、ローディング表示）
- 外部API障害時のリトライや監視強化
- CI での自動テスト実行と品質ゲート整備
## Render + Neon 本番デプロイ手順（追記）
Render の Web Service 環境変数に以下を設定します。

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

`DB_URL` は Neon の PostgreSQL 接続URLを **JDBC形式** で設定してください。  
例: `jdbc:postgresql://<host>/<database>?sslmode=require`

本番DB接続情報はソースコードに直書きせず、Render の環境変数で管理します。

ローカル起動はこれまで通り以下で `dev` / H2 を利用します。

```bash
./mvnw spring-boot:run
```