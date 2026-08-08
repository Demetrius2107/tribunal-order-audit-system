# tribunal-order-audit-system

[![JDK](https://img.shields.io/badge/JDK-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.x-green)](https://spring.io/projects/spring-cloud)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-blue)](https://baomidou.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)](https://www.mysql.com/)
[![Kafka](https://img.shields.io/badge/Kafka-3.6-black)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**B2B 発注フルチェーン業務システム** — **在庫プッシュ → 注文審査 → 履行（フルフィルメント） → 金融決済** の完全な業務フローをカバー。

上流の物料/在庫プッシュ → ディーラーがオンラインで注文 → 営業担当がルールに基づき審査 → 金融請求書を生成して財務処理 → 履行・出荷、ステータスは全行程でコールバック。

> **Author: Demetrius2107** · **Repository**: https://github.com/Demetrius2107/tribunal-order-audit-system.git

---

## 機能特性

| 機能 | 説明 |
|------|------|
| フルチェーン業務ループ | 在庫プッシュ / 注文 / 審査 / 請求 / 履行 / 決済 / 通知、ステータスは全行程でコールバック |
| ドメイン駆動設計 4 層アーキテクチャ | `interfaces → application → domain ← infrastructure`、domain 層はフレームワーク依存ゼロ |
| 審査 5-in-1 オーケストレーション | 信用チェック → 価格チェック → 在庫予約 → 請求書生成 → 履行作成 → 通知をワンパスで実行 |
| 注文/請求書ステートマシン | 2 層冪等（ステートマシン + ユニークキー）、状態遷移ログを追跡可能 |
| 信用予約/解放ループ | 審査通過で予約、拒否/キャンセルで解放、売り切れ・過剰融資を防止 |
| 金融請求書フルフロー | 生成 / 審査 / 決済 / 消込、コールバックで注文ステートマシンを駆動 |
| 統一認証 | JWT デュアルトークン + RBAC インターフェースレベル認証（M5 前倒し完了） |
| サービスガバナンス予定 | Feign リトライ（Retryer 3 回 + spring-retry）、Nacos / ゲートウェイ / サーキットブレーカー |
| 単一リポジトリ・複数モジュール | 3 システム + ゲートウェイ + 共有カーネル、15 個の Maven モジュール |
| 可観測性 | 構造化ログ / TraceId フルリンク透過 / Prometheus + Grafana メトリクス |

---

## クイックスタート

### 前提条件

| 依存 | バージョン |
|------|------|
| JDK | 21 |
| Maven | 3.8+ |
| MySQL | 8.x（サービスごとに独立 DB、計 13 個） |
| Docker（任意） | ミドルウェアオーケストレーション（MySQL/Nacos/Redis/Kafka/Prometheus/Grafana） |

> ミドルウェア一括起動：`docker-compose up -d`（Prometheus 監視を含む）

### 1. DB・テーブル作成（13 個の SQL スクリプトを順に実行）

```bash
for f in docs/sql/*.sql; do mysql -uroot -p < "$f"; done
```

### 2. 各サービスの DB アカウント/パスワードを変更

各サービスの `src/main/resources/application.yml` の `datasource` セクションを編集。

### 3. サービス起動（下流サービス優先；order-service は最後）

```bash
mvn -pl tribunal-order-auth-service spring-boot:run        # 8087
mvn -pl tribunal-order-customer-service spring-boot:run    # 8081
mvn -pl tribunal-order-inventory-service spring-boot:run   # 8083
mvn -pl tribunal-order-marketing-service spring-boot:run   # 8084
mvn -pl tribunal-order-billing-service spring-boot:run     # 8082
mvn -pl tribunal-order-fulfillment-service spring-boot:run # 8085
mvn -pl tribunal-order-notification-service spring-boot:run# 8086
mvn -pl tribunal-order-task-service spring-boot:run        # 8088
mvn -pl tribunal-order-service spring-boot:run             # 8080（最後、オーケストレーションセンター）
```

---

## 使用例

完全なフロースクリプト：`docs/api-test/审单链路验证.http`（IDEA HTTP Client 形式、順にクリックして実行）：

```http
### ログインしてトークン取得
POST http://localhost:8087/api/auth/login
Content-Type: application/json

{ "username": "dealer001", "password": "123456" }

### 注文登録
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer <accessToken>

{ "customerId": "cust-001", "items": [{ "skuCode": "SKU-001", "quantity": 10 }] }

### 審査（5-in-1 オーケストレーション発動）
POST http://localhost:8080/api/orders/{id}/review

### 請求書決済（注文ステートマシンへコールバック）
POST http://localhost:8082/api/bills/{id}/settle
```

---

## アーキテクチャ概要

### モジュール構成（15 モジュール / 3 システム + ゲートウェイ）

```
tribunal-order-audit-system（親プロジェクト、packaging=pom）
│
├── 【共有カーネル】tribunal-common-*（3 モジュール、R1 統合成果物）
│   ├── tribunal-common-core/     # コア層：ApiResponse / BizException / サービス間 DTO（純 Java、Spring 依存ゼロ）
│   ├── tribunal-common-starter/  # スタート依存：JWT 認証自動構成 / MyBatis-Plus / Feign 内部トークン
│   └── tribunal-common-event/    # イベント契約層：システム間ドメインイベント（Kafka メッセージ本文）の権威定義
│
├── 【システム 1】注文審査システム（9 業務モジュール）
│   ├── tribunal-order-auth-service/       # 認証認可：ログイン/登録/デュアルトークン/RBAC   :8087
│   ├── tribunal-order-customer-service/   # 顧客信用：顧客/信用枠/予約・解放   :8081
│   ├── tribunal-order-service/            # 注文審査：登録/ステートマシン/5-in-1 オーケストレーション   :8080 ★オーケストレーションセンター
│   ├── tribunal-order-inventory-service/  # 在庫：マスタデータ/予約・解放          :8083
│   ├── tribunal-order-marketing-service/  # マーケティング価格：価格/プロモーション/割引/デポジット      :8084
│   ├── tribunal-order-billing-service/    # 金融請求書：生成/審査/決済/消込/コールバック :8082
│   ├── tribunal-order-fulfillment-service/# 履行実行：出庫/出荷/受領/工場発注   :8085
│   ├── tribunal-order-notification-service# 通知：社内メッセージ/メール/SMS/WeChat       :8086
│   └── tribunal-order-task-service/       # 定期タスク：タイムアウトクローズ/照合/アーカイブ       :8088
│
├── 【システム 2】在庫プッシュシステム（上流データ統合ゲートウェイ）
│   └── tribunal-inventory-push-service/   # 在庫プッシュ：マスタデータ/上流へのプッシュ
│
├── 【システム 3】金融決済システム（下流資金決済ハブ）
│   └── tribunal-finance-settlement-service/  # 金融決済：請求書決済/消込/資金フロー
│
└── 【ゲートウェイ】tribunal-gateway/      # M5：統一エントリ/ルーティング/Nacos ディスカバリ/JWT 事前認証
```

### 審査 5-in-1 オーケストレーション（order-service のコアフロー）

```
審査通過(order-service)
  ① 信用チェック    → customer-service（利用可能信用 ≥ 支払額）
  ② 価格チェック    → marketing-service（顧客価格→顧客グループ価格→地域価格）
  ③ 在庫予約        → inventory-service（SKU 単位で予約、売り切れ防止）
  ④ 請求書生成      → billing-service（請求書 GENERATED、引き渡し）
  ⑤ 履行作成        → fulfillment-service（履行 GENERATED、出庫/出荷待ち）
  ⑥ 通知送信        → notification-service（ディーラーへ社内メッセージ）
  ⑦ 状態遷移        → order ステートマシン：確認待ち → 確認済み
```

**ステータスコールバックループ**：billing 決済/消込 → order へコールバック → 注文ステートマシン進行；fulfillment 出荷/受領 → order へコールバック → 終端状態。

### 技術スタック

JDK 21 / Spring Boot 3.2.x / Spring Cloud 2023.0.x + OpenFeign / MyBatis-Plus 3.5.7 / MySQL 8.x（サービスごとに独立 DB）/ Kafka / Redis / Nacos / Docker Compose

---

## ロードマップ

### マイルストーン（M0~M7）

| マイルストーン | テーマ | 状態 |
|--------|------|------|
| M0 | アーキテクチャ基盤（ドメイン駆動設計 4 層 + 統一レスポンス/例外） | ✅ 完了 |
| M1 | 注文コアループ（登録/ステートマシン/遷移ログ） | ✅ 完了 |
| M2 | 審査オーケストレーション + 仕上げ（信用/在庫/請求/履行 + Feign リトライ） | ✅ 完了 |
| M3 | 非同期とイベント（Kafka + ローカルメッセージテーブル / 在庫フロー / 収入フロー / 照合） | ⬜ 次ステップ |
| M4 | 業務ルールエンジン（プロモーション/割引/デポジット設定化、注文分割結合、倉庫ソーシング） | ⬜ |
| M5 | マイクロサービスガバナンス（Nacos / ゲートウェイ / サーキットブレーカー / Redis 冪等） | 🟡 認証部分は前倒し完了 |
| M6 | 可観測性と運用（ログ/トレース/監視/アラート/CI-CD/負荷テスト） | ⬜ |
| M7 | 本番強化（シャーディング / アーカイブ / 災害対策 / セキュリティ強化） | ⬜ |

### リファクタリングフェーズ（R1~R6）

| フェーズ | テーマ | 期間 | コア目標 |
|------|------|------|----------|
| R1 | 共有カーネル統合 | 1 週間 | 3 つの common を統合（order/inventory-push/finance-settlement → tribunal-common-core/starter/event） |
| R2 | OMS 内部サブドメイン統合 | 2 週間 | customer/inventory/marketing を独立サービスから OMS サブモジュールへ降格、Feign → プロセス内呼び出し |
| R3 | 非同期イベント化 | 2 週間 | billing/fulfillment を Feign 同期から Kafka 非同期 + ローカルメッセージテーブルで原子性確保 |
| R4 | 業務能力補完 | 3 週間 | 注文分割/結合/倉庫ソーシング/プロモーションエンジン/クーポン/デポジットエンジン/アフターセールス返品 |
| R5 | マイクロサービスガバナンス | 2 週間 | Nacos/Resilience4j サーキットブレーカー/Redis 冪等/Spring Cloud Gateway |
| R6 | 可観測性と強化 | 2 週間 | 構造化ログ/フルリンクトレーシング/Prometheus+Grafana/アラート/負荷テスト |

> 目標アーキテクチャ：**4 システム + 共有カーネル**（inventory-center 在庫センター / oms 注文ミドルウェア / fulfillment 履行 / settlement 金融決済）、31 個の Maven モジュール。詳細は「目標システムアーキテクチャ設計文書」「リファクタリングアーキテクチャ — モジュール分割案」参照。

---

## ドキュメントインデックス（docs/ 計 18 点、タイプ別に分類）

| カテゴリ | ドキュメント | 用途 |
|------|------|------|
| 要件 | 需求/需求规格说明书-生产级目标.md | 機能/非機能要件 + マイルストーン（M0~M7） |
| 要件 | 需求/需求编号与代码实现映射表.md | 要件番号 → コード位置 → 実装状態（✅/🟡/⬜） |
| 要件 | 需求/订单业务全系统功能清单.md | 19 システム全景 + 機能リスト |
| 要件 | 需求/金融结算模块需求规格说明书.md | 金融決済モジュール要件 |
| 要件 | 需求/库存推送模块需求规格说明书.md | 在庫プッシュモジュール要件 |
| アーキテクチャ | 架构/架构总览.md | 現在のアーキテクチャモジュール全景（15 モジュール/ポート/DB） |
| アーキテクチャ | 架构/目标系统架构设计文档.md | 目標レイヤードアーキテクチャ + 4 システムモジュール分割 |
| アーキテクチャ | 架构/重构架构-模块分包方案.md | 31 モジュール Maven ツリー + 移行マッピング |
| アーキテクチャ | 架构/认证授权链路设计.md | JWT + RBAC フルフロー |
| 設計 | 设计/数据库设计文档.md | 全システムテーブル構造 + インデックス + シャーディング |
| 設計 | 设计/API接口设计规范.md | REST 規約 + エラーコード + API リスト |
| 設計 | 设计/开发规范与工程约定.md | ドメイン駆動設計レイヤリング + 命名 + Git + テスト |
| ガイド | 指南/开发计划与里程碑.md | フェーズ別リファクタリング計画（R1~R6） |
| ガイド | 指南/业务名词与业务处理解析.md | 業務用語辞書 |
| ガイド | 指南/OMS核心业务开发指南-拆单寻源状态机.md | 分割/結合/ソーシング/ステートマシン補完の施工図 |
| ガイド | 指南/数据流转验证指南.md | 4 層検証戦略 + .http フロースクリプト |
| ガイド | 指南/M4业务补全与M6可观测性-开发记录.md | M4 分割/結合/アフターセールス + M6 可観測性実装ログ |
| ガイド | 指南/0806-执行计划.md | メインライン開発手順 + 受入基準（コードの実状態から） |
| スクリプト | sql/*.sql | 13 個の DDL スクリプト（docs/sql/、順に実行、クイックスタート参照） |
| 検証 | api-test/审单链路验证.http | IDEA HTTP Client フロー検証スクリプト（docs/api-test/、注文→審査→請求→履行） |

---

## コントリビューションガイド

1. **地図に従う**：すべての要件番号（F-/N-/Q-）は「要件番号とコード実装マッピング表」でコード位置と実装状態を確認できる
2. **レイヤリング制約を守る**：domain 層は Spring/MyBatis クラスを一切 import しない（`interfaces → application → domain ← infrastructure`）
3. **サービス境界では DTO を使う**：Feign は common の DTO を返す。ドメインクラスをサービス間で渡さない
4. **テストを書く**：ステートマシン/金額計算にはユニットテスト必須（合法/不正/重複遷移）。現状の 46 ケースが下限
5. **検証は .http スクリプトで**：「データフロー検証ガイド」に従い IDEA HTTP Client で各ロールを演じ、手動の Postman 操作を避ける
6. Conventional Commits に従う（`feat`/`fix`/`docs`/`refactor`/`test`/`build`/`ci`）

---

## License

[MIT](LICENSE) © 2026 Demetrius2107
