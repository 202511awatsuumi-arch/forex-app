# Forex App

Spring Boot + MyBatis + Thymeleaf で作成した為替レート確認アプリです。  
通貨レートの参照、換算、履歴確認、アラート登録を行えます。

## 1. 主な機能
- 最新為替レート表示（例: USD/JPY, USD/EUR）
- 通貨換算
- レート履歴表示
- アラート条件登録・一覧・削除（`ABOVE` / `BELOW`）

## 2. 技術スタック
- Java 17
- Spring Boot 3.x
- MyBatis
- Thymeleaf
- Maven Wrapper（`mvnw`, `mvnw.cmd`）
- H2（ローカル開発）
- PostgreSQL（本番環境）

## 3. ディレクトリと主要ファイル
- `src/main/java` : アプリケーションコード
- `src/main/resources/templates` : 画面テンプレート
- `src/main/resources/mapper` : MyBatis Mapper XML
- `src/main/resources/application.properties` : 共通設定
- `src/main/resources/application-dev.properties` : 開発（H2）設定
- `src/main/resources/application-prod.properties` : 本番（PostgreSQL）設定
- `src/main/resources/schema-h2.sql` : H2 用スキーマ
- `src/main/resources/schema-postgres.sql` : PostgreSQL 用スキーマ
- `Dockerfile` : Render Docker デプロイ用

## 4. 前提条件
- JDK 17
- Git
- インターネット接続（依存ライブラリ取得用）

Maven はローカルインストール不要です（Maven Wrapper を使用）。

## 5. ローカル開発（H2）
### 5.1 プロファイル
`application.properties` で以下が設定されています。

- `spring.profiles.default=dev`

そのため、特に指定しなければ `dev` プロファイルで起動します。

### 5.2 dev プロファイルの内容
`application-dev.properties`:
- `spring.datasource.url=jdbc:h2:mem:forexdb...`
- `spring.datasource.driver-class-name=org.h2.Driver`
- `spring.h2.console.enabled=true`
- `spring.h2.console.path=/h2-console`
- `spring.sql.init.schema-locations=classpath:schema-h2.sql`

つまりローカルでは:
- インメモリ H2 DB を利用
- 起動時に `schema-h2.sql` が適用
- H2 Console が有効

### 5.3 起動手順
#### Windows PowerShell
```powershell
.\mvnw spring-boot:run
```

#### macOS / Linux
```bash
./mvnw spring-boot:run
```

起動後:
- アプリ: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

## 6. 本番運用（Render + PostgreSQL）
### 6.1 prod プロファイルの内容
`application-prod.properties`:
- `spring.datasource.url=${DB_URL}`
- `spring.datasource.username=${DB_USERNAME}`
- `spring.datasource.password=${DB_PASSWORD}`
- `spring.datasource.driver-class-name=org.postgresql.Driver`
- `spring.sql.init.mode=always`
- `spring.sql.init.schema-locations=classpath:schema-postgres.sql`

本番では環境変数から DB 接続情報を受け取り、`schema-postgres.sql` を使用します。

### 6.2 Render に設定する環境変数
最低限以下を設定してください。

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

`DB_URL` の例:
`jdbc:postgresql://<host>/<database>?sslmode=require`

## 7. Docker / Render デプロイ
このリポジトリは Docker デプロイ前提です。

- ルート直下に `Dockerfile` を配置
- Render の `Dockerfile Path` は空欄（ルート `Dockerfile` を使う）

Dockerfile は multi-stage build で:
1. `eclipse-temurin:17-jdk` で Maven build（`./mvnw clean package -DskipTests`）
2. `eclipse-temurin:17-jre` で jar 実行

## 8. テスト
### Windows PowerShell
```powershell
.\mvnw test
```

### macOS / Linux
```bash
./mvnw test
```

## 9. API
詳細仕様: [src/main/resources/API_SPEC.md](src/main/resources/API_SPEC.md)

主なエンドポイント:
- `GET /api/rates`
- `GET /api/convert`
- `GET /api/rates/history`
- `GET /api/alerts`
- `POST /api/alerts`
- `DELETE /api/alerts/{id}`

## 10. DBスキーマ
- H2 用: [src/main/resources/schema-h2.sql](src/main/resources/schema-h2.sql)
- PostgreSQL 用: [src/main/resources/schema-postgres.sql](src/main/resources/schema-postgres.sql)
- 互換用途: [src/main/resources/schema.sql](src/main/resources/schema.sql)

主なテーブル:
- `exchange_rate`
- `alert_setting`

## 11. よくある確認ポイント
- `prod` で起動しているか（`SPRING_PROFILES_ACTIVE=prod`）
- `DB_URL` が JDBC 形式か
- Render で Docker ビルド時にルート `Dockerfile` を参照しているか
- アプリ起動ログに DB 接続エラーが出ていないか
