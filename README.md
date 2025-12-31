# Swiss Stage Web

囲碁・将棋のスイス方式トーナメント運営を効率化するWebアプリケーション

---

## プロジェクト概要

16名から300名規模の囲碁・将棋大会における**対戦組合せ自動化**と**進行状況のリアルタイム共有**を実現します。

### 解決する課題

#### 大会運営者向け
- 2回戦以降の対戦相手マッチングを手動で行う煩雑さ（勝ち点差0.5点以内・未対戦者同士の条件判定）
- 紙への対戦結果転記の手間
- マッチング生成に時間がかかることによる大会進行の遅延

#### 大会参加者向け
- 紙のトーナメント表を確認するための移動の手間
- 対戦結果報告時の受付混雑

---

## 主要機能

### MVP機能（優先度P1）
- ✅ **ログイン**: Google OAuth2認証
- ✅ **トーナメント一覧/作成**: 大会の新規作成・一覧表示
- ✅ **参加者管理**: CSV入出力、奇数時のダミーユーザー自動追加
- ✅ **対戦表**: 自動マッチング、対戦結果入力、参照URL発行
- ✅ **ランキング**: リアルタイム順位表示
- ✅ **設定**: 大会タイトル、グループ数（1-8）、対戦回数（2-5）

### 将来機能（優先度P2/P3）
- ⏳ 団体戦対応
- ⏳ スマホレスポンシブ対応
- ⏳ 対戦結果送信機能（参加者側から）

### 対象外
- ❌ 課金・決済機能
- ❌ ネイティブアプリ（iOS/Android/Windows）
- ❌ エントリーフォーム（外部フォーム連携を想定）

---

## 技術スタック

| レイヤー | 技術 |
|---------|------|
| **フロントエンド** | React 18 + TypeScript + Vite + Material-UI |
| **バックエンド** | Spring Boot 3 + Java 21 + DDD設計 |
| **データベース** | AWS DynamoDB |
| **インフラ** | AWS (EC2, Route53, CloudWatch) |
| **テスト** | JUnit 5 + Jest + Playwright |

詳細は [.specify/memory/tech-stack.md](.specify/memory/tech-stack.md) を参照。

---

## プロジェクト憲章

このプロジェクトは以下の5原則に基づき開発されます：

1. **ドメイン駆動設計 (DDD)** - バックエンドは4層構造（application/domain/infrastructure/presentation）
2. **テスト駆動開発 (TDD)** - テスト → 実装の順序厳守【非交渉】
3. **AIペアプログラミング** - AI活用の明確な役割分担
4. **段階的機能実装** - MVP優先の開発フロー
5. **スケーラビリティと可観測性** - 300名規模対応・CloudWatch監視

詳細は [.specify/memory/constitution.md](.specify/memory/constitution.md) を参照。

---

## 開発環境セットアップ

### 必須ツール

- Node.js 20.x LTS
- Java 21 (Temurin推奨)
- Gradle 8.x
- Docker 24.x（DynamoDB Local用）
- AWS CLI 2.x

### ローカル起動手順

```bash
# 1. DynamoDB Localを起動
docker run -p 8000:8000 amazon/dynamodb-local

# 2. バックエンド起動
cd backend
./gradlew bootRun

# 3. フロントエンド起動（別ターミナル）
cd frontend
npm install
npm run dev
```

起動後、ブラウザで `http://localhost:5173` にアクセス。

---

## ディレクトリ構造

```
swiss-stage-web/
├── .github/
│   └── prompts/                  # AI連携プロンプト
├── .specify/
│   ├── memory/                   # プロジェクト憲章・ガイドライン
│   │   ├── constitution.md       # プロジェクト憲章
│   │   ├── tech-stack.md         # 技術スタック詳細
│   │   └── ai-collaboration-guide.md  # AI連携ガイド
│   └── templates/                # 仕様・計画テンプレート
├── backend/
│   ├── src/main/java/com/swiss_stage/
│   │   ├── application/          # ユースケース層
│   │   ├── domain/               # ドメイン層
│   │   ├── infrastructure/       # インフラ層
│   │   └── presentation/         # プレゼンテーション層
│   └── src/test/java/            # テストコード
├── frontend/
│   ├── src/
│   │   ├── components/           # UIコンポーネント
│   │   ├── pages/                # 画面
│   │   └── services/             # API通信
│   └── tests/                    # Jest/Playwright
└── specs/                        # 機能仕様書
```

---

## テスト戦略

### カバレッジ目標

| 層 | フレームワーク | 目標 |
|----|-------------|------|
| domain層 | JUnit 5 | 90%以上 |
| application層 | JUnit 5 + Mockito | 80%以上 |
| presentation層 | MockMvc | 70%以上 |
| E2E | Playwright | 主要フロー全網羅 |

### テスト実行

```bash
# バックエンド単体テスト
cd backend
./gradlew test

# フロントエンド単体テスト
cd frontend
npm test

# E2Eテスト
cd frontend
npm run test:e2e
```

---

## デプロイ

### 本番環境（AWS）

```
[ Route53 ] → [ ALB ] → [ EC2 (t3.micro) ]
                            ↓
                        [ DynamoDB ]
                            ↓
                    [ CloudWatch Logs/Alarms ]
```

**コスト見積もり**: ~$17/月（無料枠適用後）

### デプロイ手順（手動）

```bash
# 1. フロントエンドビルド
cd frontend
npm run build

# 2. バックエンドビルド
cd backend
./gradlew bootJar

# 3. EC2にデプロイ
scp -i key.pem backend/build/libs/*.jar ec2-user@<EC2-IP>:/home/ec2-user/
ssh -i key.pem ec2-user@<EC2-IP>
java -jar app.jar
```

CI/CD（GitHub Actions）は将来導入予定。

---

## AI連携について

このプロジェクトではAIを**開発パートナー**として活用します。

### AIに実施させること
- 日本語での仕様書・ドキュメント記述
- TDD手順に従うコード生成（テスト → 実装）
- DDD設計提案（レイヤー分割・エンティティ設計）
- UIコンポーネント生成（Material-UI使用）

### AIに実施させないこと
- 技術スタック変更の独断決定
- テスト省略の提案
- セキュリティ要件の緩和

詳細は [.specify/memory/ai-collaboration-guide.md](.specify/memory/ai-collaboration-guide.md) を参照。

---

## 貢献ガイドライン

### ブランチ戦略

```
main (本番環境)
  └── develop (開発環境)
       └── feature/###-feature-name (機能開発)
```

### コミットメッセージ規約

```
<type>: <subject>

type:
  feat: 新機能
  fix: バグ修正
  docs: ドキュメント更新
  test: テスト追加
  refactor: リファクタリング
```

例: `feat: 対戦組合せ自動マッチング機能を実装`

### プルリクエスト

1. 機能ブランチで開発
2. 憲章の品質ゲートを通過確認
3. PRを作成（テンプレートに従う）
4. レビュー後にマージ

---

## ライセンス

MIT License（予定）

---

## お問い合わせ

プロジェクトオーナー: takashi-ebina

---

## 変更履歴

| バージョン | 日付 | 変更内容 |
|----------|------|---------|
| 0.1.0 | 2025-12-31 | プロジェクト初期化・憲章策定 |
