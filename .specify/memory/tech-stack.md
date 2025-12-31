# 技術スタック詳細

**Version**: 1.0.0 | **最終更新**: 2025-12-31

このドキュメントは [constitution.md](constitution.md) の「技術スタック制約」セクションの詳細仕様です。

---

## フロントエンド

### コア技術

| カテゴリ | 技術 | バージョン | 用途 |
|---------|------|----------|------|
| 言語 | TypeScript | 5.x | 型安全なコード記述 |
| フレームワーク | React | 18.x | UIコンポーネント構築 |
| ビルドツール | Vite | 5.x | 高速開発サーバー・バンドル |
| UIライブラリ | Material-UI | 5.x | コンポーネント・デザインシステム |
| ルーティング | React Router | 6.x | SPA画面遷移 |
| 状態管理 | （未定） | - | 必要に応じてRedux/Zustand導入検討 |

### テスト・品質

| カテゴリ | 技術 | 用途 |
|---------|------|------|
| 単体テスト | Jest | コンポーネント・ロジックテスト |
| E2Eテスト | Playwright | ユーザーフロー検証 |
| 静的解析 | ESLint | コード品質チェック |
| フォーマッター | Prettier | コードフォーマット統一 |

### ディレクトリ構造（推奨）

```
frontend/
├── src/
│   ├── components/      # 再利用可能UIコンポーネント
│   ├── pages/           # 画面単位コンポーネント
│   ├── services/        # API通信ロジック
│   ├── hooks/           # カスタムReact Hooks
│   ├── types/           # TypeScript型定義
│   └── utils/           # 汎用ユーティリティ
├── tests/
│   ├── unit/            # Jest単体テスト
│   └── e2e/             # Playwrightシナリオ
└── public/              # 静的ファイル
```

---

## バックエンド

### コア技術

| カテゴリ | 技術 | バージョン | 用途 |
|---------|------|----------|------|
| 言語 | Java | 21 (LTS) | 堅牢なバックエンド実装 |
| フレームワーク | Spring Boot | 3.x | REST API・DIコンテナ |
| ビルドツール | Gradle | 8.x | 依存管理・ビルド |
| ORM | Spring Data DynamoDB | 最新 | DynamoDBアクセス抽象化 |

### Spring Boot主要依存

```gradle
dependencies {
    // Web API
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // DynamoDB
    implementation 'io.github.boostchicken:spring-data-dynamodb:5.2.5'
    implementation 'software.amazon.awssdk:dynamodb:2.x'
    
    // セキュリティ（Google OAuth2）
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    
    // バリデーション
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // テスト
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.x'
}
```

### DDDレイヤー構造（厳守）

```
backend/
├── src/main/java/com/swiss_stage/
│   ├── application/          # アプリケーション層
│   │   ├── service/          # ユースケース実装
│   │   └── dto/              # データ転送オブジェクト
│   ├── domain/               # ドメイン層
│   │   ├── model/            # エンティティ・値オブジェクト
│   │   ├── repository/       # リポジトリインターフェース
│   │   └── service/          # ドメインサービス
│   ├── infrastructure/       # インフラ層
│   │   ├── repository/       # DynamoDB実装
│   │   └── config/           # AWS設定
│   └── presentation/         # プレゼンテーション層
│       ├── controller/       # REST APIコントローラー
│       └── filter/           # リクエストフィルター
└── src/test/java/com/swiss_stage/
    ├── unit/                 # 単体テスト（層ごと）
    ├── integration/          # 統合テスト（層間連携）
    └── contract/             # APIコントラクトテスト
```

### テスト戦略

| テストタイプ | フレームワーク | カバレッジ目標 | 対象 |
|------------|--------------|--------------|------|
| 単体テスト | JUnit 5 + Mockito | domain層: 90%以上 | ドメインロジック・サービス |
| 統合テスト | Spring Boot Test | application層: 80%以上 | ユースケース・リポジトリ連携 |
| APIテスト | MockMvc | presentation層: 70%以上 | REST APIエンドポイント |

---

## データベース

### DynamoDB設計原則

| 項目 | 設計方針 |
|------|---------|
| テーブル設計 | 単一テーブル設計（Single Table Design）検討 |
| パーティションキー | アクセスパターンに基づき設計（例: `TOURNAMENT#{tournamentId}`) |
| ソートキー | 階層データ管理用（例: `PARTICIPANT#{participantId}`) |
| GSI | クエリパターンごとに最大2-3個まで |
| スループット | オンデマンドモード（予算重視） |

### 主要テーブル（想定）

```
swiss_stage_table (メインテーブル)
  PK: EntityType#EntityId (例: TOURNAMENT#t001)
  SK: SubEntityType#SubEntityId (例: ROUND#r002)
  Attributes: JSON形式で柔軟に格納
  
GSI1: 参加者名検索用
  GSI1PK: PARTICIPANT
  GSI1SK: ParticipantName
```

---

## インフラ（AWS）

### 構成概要

```
[ ユーザー ] 
    ↓
[ Route53 ] → [ ALB ] → [ EC2 (t3.micro) ]
                            ↓
                        [ DynamoDB ]
                            ↓
                    [ CloudWatch Logs/Alarms ]
```

### リソース仕様

| サービス | 仕様 | 理由 |
|---------|------|------|
| EC2 | t3.micro (2vCPU, 1GB) | 予算制約下で300名対応 |
| DynamoDB | オンデマンドモード | 大会時のみトラフィック増のため従量課金が有利 |
| CloudWatch | Logs + Alarms | 構造化ログ保存・異常検知 |
| Route53 | ホストゾーン1個 | 独自ドメイン運用 |
| VPC | デフォルトVPC利用 | コスト削減 |

### コスト見積もり（月額）

- EC2 t3.micro: ~$8 (750時間無料枠)
- DynamoDB: ~$5 (書き込み少量想定)
- CloudWatch: ~$3 (ログ保存5GB)
- Route53: ~$1
- **合計**: ~$17/月（無料枠適用後）

### スケーリング戦略

| 規模 | 対応策 |
|------|--------|
| 16-100名 | 単一EC2で十分 |
| 100-300名 | ALB + EC2オートスケーリング（最大3台） |
| 300名以上 | 将来検討（現在は対象外） |

---

## CI/CD（将来導入）

### 推奨ツール

- **GitHub Actions**: PRテスト・デプロイ自動化
- **AWS CodeDeploy**: EC2へのゼロダウンタイムデプロイ

### パイプライン（MVP後）

```yaml
on: [pull_request]
jobs:
  frontend-test:
    - npm test (Jest)
    - npm run lint
  backend-test:
    - ./gradlew test
    - ./gradlew build
  e2e-test:
    - playwright test
```

---

## セキュリティ

### 認証・認可

- **Google OAuth2**: 大会運営者ログイン
- **JWT**: セッション管理（Spring Security利用）
- **参照URLトークン**: 参加者向け時限付きアクセストークン

### 脆弱性対策

- **依存パッケージ**: Dependabot自動更新
- **入力検証**: Bean Validation (JSR 380)
- **HTTPS**: ALBでSSL/TLS終端

---

## 開発環境

### 必須ツール

| ツール | バージョン | 用途 |
|-------|----------|------|
| Node.js | 20.x LTS | フロントエンドビルド |
| Java JDK | 21 (Temurin推奨) | バックエンド実行 |
| Gradle | 8.x | バックエンドビルド |
| Docker | 24.x | DynamoDB Local実行 |
| AWS CLI | 2.x | AWSリソース操作 |

### ローカル開発手順

```bash
# DynamoDB Local起動
docker run -p 8000:8000 amazon/dynamodb-local

# バックエンド起動
cd backend
./gradlew bootRun

# フロントエンド起動
cd frontend
npm install
npm run dev
```

---

## 変更履歴

| バージョン | 日付 | 変更内容 |
|----------|------|---------|
| 1.0.0 | 2025-12-31 | 初版作成（憲章v1.0.0対応） |
