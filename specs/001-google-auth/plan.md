# 実装計画: Google OAuth2 ログイン認証

**Branch**: `001-google-auth` | **日付**: 2025-12-31 | **仕様**: [spec.md](spec.md)
**入力**: `/specs/001-google-auth/spec.md` からの機能仕様

**注意**: このテンプレートは `/speckit.plan` コマンドによって記入されます。実行ワークフローについては `.specify/templates/commands/plan.md` を参照してください。

## 概要

Google OAuth2を用いたログイン認証機能を実装し、大会運営者がパスワード管理を必要とせずにGoogleアカウントでシステムにアクセスできるようにする。認証後はJWTトークンを使用したセッション管理（24時間有効）を行い、アカウント削除機能を提供する。進行中トーナメントがある場合は削除を禁止し、データ整合性を保証する。

**技術的アプローチ**:
- Spring Security OAuth2 ClientでGoogle OAuth2フローを実装
- JWT（HS256）でステートレス認証を実現
- DynamoDBにユーザー情報を保存（userIdをPKとする単一テーブル設計）
- ReactでGoogleログインボタンとアカウント設定画面を実装
- 個人情報保護: ログにuserIdのみ記録、email/displayNameはマスキング

## 技術的コンテキスト

**Language/Version**: Java 21 (LTS) / TypeScript 5.x  
**Primary Dependencies**: 
- Backend: Spring Boot 3.x, Spring Security OAuth2 Client, Spring Data DynamoDB 5.2.5, AWS SDK for Java 2.x
- Frontend: React 18.x, Material-UI 5.x, Vite 5.x, React Router 6.x

**Storage**: AWS DynamoDB (単一テーブル設計, オンデマンドモード)  
**Testing**: 
- Backend: JUnit 5 + Mockito (unit), Spring Boot Test (integration)
- Frontend: Jest (unit), Playwright (E2E)

**Target Platform**: 
- Backend: Linux server (AWS EC2 t3.micro)
- Frontend: Modern browsers (Chrome/Firefox/Safari latest)

**Project Type**: Webアプリケーション (Backend + Frontend 分離構成)  
**Performance Goals**: 
- Google OAuth2認証画面への遷移: 3秒以内
- 認証コールバック処理: 2秒以内
- JWT検証: 100ms以内
- アカウント削除処理: 5秒以内

**Constraints**: 
- 同時認証処理: 300ユーザーまで対応
- 予算: AWS無料枠/低コストインスタンス優先
- HTTPS必須 (OAuth2認証のため)
- 個人情報保護: email/displayName等をログに出力禁止 (userIdのみ記録)

**Scale/Scope**: 
- MVP機能としてP1優先度
- 想定ユーザー: 16-300名の大会運営者
- ユーザーストーリー: 3件 (ログイン、エラーハンドリング、アカウント削除)
- 機能要件: 17件 (FR-001 ~ FR-017)

## 憲章チェック

*GATE:フェーズ0の調査前に合格する必要があります。フェーズ1の設計後に再確認してください。*

### 原則I: ドメイン駆動設計 (DDD)
- [x] バックエンドはDDDレイヤー構造（application/domain/infrastructure/presentation）に従っているか
  - **検証**: プロジェクト構造セクションで4層構造を定義済み
- [x] ドメインロジックがdomain層に集約されているか
  - **検証**: User/AuthSessionエンティティをdomain/modelに配置、ビジネスロジック（ユーザー名検証、トークン有効性検証）を含む設計
- [x] infrastructure層への依存が逆転していないか
  - **検証**: domain/repository/UserRepository.javaはインターフェースのみ、実装はinfrastructure/repository/DynamoDbUserRepository.javaに配置

### 原則II: テスト駆動開発 (TDD)【非交渉】
- [x] 受け入れテストシナリオが仕様書に明記されているか
  - **検証**: spec.mdに3つのユーザーストーリーと15の受け入れシナリオを記載済み
- [x] テストコード作成 → 実装の順序が守られているか
  - **計画**: Phase 2 (tasks.md生成時) にTDD手順を明記予定
- [x] テストカバレッジ目標（domain層90%以上、application層80%以上）を満たせるか
  - **検証**: プロジェクト構造でunit/domain, unit/application, contract/apiのテスト配置を定義済み

### 原則III: AIペアプログラミング
- [x] AIに実施させる範囲（日本語記述、コード生成、設計提案、UI記述）が明確か
  - **検証**: この計画書自体がAIによる日本語記述、DDD設計提案を実施
- [x] 技術スタック変更の独断決定を防ぐ仕組みがあるか
  - **検証**: 技術的コンテキストでSpring Boot/React/DynamoDBを明記、constitution.mdで変更不可を規定

### 原則IV: 段階的機能実装
- [x] 機能に優先度（P1/P2/P3）がついているか
  - **検証**: spec.mdで全ユーザーストーリーがP1優先度として明記済み
- [x] MVP機能（P1）が明確に定義されているか
  - **検証**: ログイン認証はconstitution.mdのP1リストに含まれる基盤機能

### 原則V: スケーラビリティと可観測性
- [x] パフォーマンス目標（300名対応、5秒以内マッチング）を満たせるか
  - **検証**: 技術的コンテキストで300ユーザー対応を明記、認証処理は2秒以内目標
- [x] 構造化ログ（JSON）をCloudWatch Logsに出力する設計か
  - **検証**: FR-008/FR-017でJSON形式のCloudWatch Logs出力を要件化
- [x] 主要APIエンドポイントのレスポンスタイム監視が含まれているか
  - **計画**: quickstart.mdでCloudWatch Metricsによる認証成功率/失敗率監視を記載予定

### 原則VI: 個人情報保護とプライバシー【非交渉】
- [x] 個人識別情報（PII）をログに出力しない設計か
  - **検証**: FR-008/FR-017でuserIdのみ記録、email/displayNameはマスキングを要件化
- [x] マスキングルール（userId記録、email/name→[MASKED_*]）が適用されているか
  - **検証**: constitution.md v1.1.0の原則VIに準拠した設計
- [x] Debugログを含む全ログが対象か
  - **検証**: spec.mdのセキュリティ考慮事項で全ログ種別への適用を明記

**GATE判定**: ✅ **合格** - すべての憲章原則を満たしています。Phase 0調査に進めます。

## プロジェクト構造

### ドキュメント (この機能)

```text
specs/001-google-auth/
├── plan.md              # このファイル (/speckit.planコマンド出力)
├── research.md          # Phase 0出力 (/speckit.planコマンド)
├── data-model.md        # Phase 1出力 (/speckit.planコマンド)
├── quickstart.md        # Phase 1出力 (/speckit.planコマンド)
├── contracts/           # Phase 1出力 (/speckit.planコマンド)
│   ├── auth-api.yaml    # 認証関連API定義 (OpenAPI 3.0)
│   └── user-api.yaml    # ユーザー/アカウントAPI定義 (OpenAPI 3.0)
└── tasks.md             # Phase 2出力 (/speckit.tasksコマンド - /speckit.planでは作成されない)
```

### ソースコード (リポジトリのルート)

```text
backend/
├── src/main/java/com/swiss_stage/
│   ├── application/          # アプリケーション層
│   │   ├── service/
│   │   │   ├── AuthService.java           # 認証ユースケース
│   │   │   └── UserService.java           # ユーザー管理ユースケース
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       └── UserDto.java
│   ├── domain/                   # ドメイン層
│   │   ├── model/
│   │   │   ├── User.java                  # ユーザーエンティティ
│   │   │   └── AuthSession.java           # 認証セッション値オブジェクト
│   │   └── repository/
│   │       └── UserRepository.java        # リポジトリインターフェース
│   ├── infrastructure/           # インフラ層
│   │   ├── repository/
│   │   │   └── DynamoDbUserRepository.java # DynamoDB実装
│   │   └── config/
│   │       ├── DynamoDbConfig.java
│   │       ├── SecurityConfig.java         # Spring Security + OAuth2
│   │       └── JwtConfig.java
│   └── presentation/             # プレゼンテーション層
│       ├── controller/
│       │   ├── AuthController.java        # /api/auth/**
│       │   └── UserController.java        # /api/users/**
│       └── filter/
│           └── JwtAuthenticationFilter.java
└── src/test/java/com/swiss_stage/
    ├── unit/
    │   ├── domain/                   # ドメインロジックテスト (90%)
    │   └── application/              # ユースケーステスト (80%)
    ├── integration/
    │   └── repository/               # DynamoDB連携テスト
    └── contract/
        └── api/                      # APIコントラクトテスト (70%)

frontend/
├── src/
│   ├── components/
│   │   ├── auth/
│   │   │   ├── GoogleLoginButton.tsx
│   │   │   └── LogoutButton.tsx
│   │   └── account/
│   │       └── DeleteAccountDialog.tsx
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   └── AccountSettingsPage.tsx
│   ├── services/
│   │   ├── authService.ts            # API通信ロジック
│   │   └── userService.ts
│   ├── hooks/
│   │   └── useAuth.ts                # 認証状態管理
│   ├── types/
│   │   ├── User.ts
│   │   └── AuthSession.ts
│   └── utils/
│       └── apiClient.ts              # Axiosインスタンス
└── tests/
    ├── unit/
    │   ├── components/               # コンポーネントテスト (Jest)
    │   └── services/                 # サービステスト (Jest)
    └── e2e/
        ├── login.spec.ts             # ログインフロー (Playwright)
        ├── logout.spec.ts            # ログアウトフロー
        └── delete-account.spec.ts    # アカウント削除フロー
```

**構造の決定**: Webアプリケーション構成。バックエンドはDDDレイヤー構造（application/domain/infrastructure/presentation）を厳守。フロントエンドは機能ごとのコンポーネント分割とページベースのルーティングを採用。

## 複雑さの追跡

**憲法チェック違反なし**: すべての憲章原則を満たしており、正当化が必要な違反はありません。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| なし | - | - |

---

## Phase 0: 調査完了サマリー

✅ **research.md生成完了**

主要な技術的決定:
1. Spring Security OAuth2 Client（宣言的設定）
2. JWT (HS256)（24時間有効期限）
3. HTTP-only Cookie（XSS対策）
4. DynamoDB単一テーブル（findOrCreateパターン）
5. 個人情報マスキング（Logback + 構造化ログ）
6. エラーハンドリング（ユーザーフレンドリーなメッセージ）
7. 物理削除（GDPR準拠、進行中トーナメントチェック）

すべての決定は憲章原則に準拠しています。

---

## Phase 1: 設計完了サマリー

✅ **data-model.md生成完了**
- User（ユーザー）エンティティ: domain/model/User.java
- AuthSession（認証セッション）値オブジェクト: domain/model/AuthSession.java
- DynamoDB単一テーブル設計: PK=`USER#{userId}`, SK=`METADATA`
- リポジトリインターフェース: domain/repository/UserRepository.java
- DynamoDB実装: infrastructure/repository/DynamoDbUserRepository.java

✅ **contracts/生成完了**
- auth-api.yaml: Google OAuth2認証エンドポイント（OpenAPI 3.0）
  - GET /api/auth/google: 認証開始
  - GET /api/auth/google/callback: コールバック処理
  - GET /api/auth/me: 現在のユーザー情報取得
  - POST /api/auth/logout: ログアウト
- user-api.yaml: ユーザー管理エンドポイント（OpenAPI 3.0）
  - GET /api/users/{userId}: ユーザー情報取得
  - DELETE /api/users/{userId}: アカウント削除

✅ **quickstart.md生成完了**
- Google Cloud Console設定手順
- DynamoDB テーブル作成（ローカル/本番）
- バックエンド起動手順（環境変数、ビルド）
- フロントエンド起動手順（npm install, npm run dev）
- 動作確認（ログインフロー、API動作確認、アカウント削除）
- テスト実行（JUnit 5, Jest, Playwright）
- トラブルシューティング（OAuth2エラー、DynamoDB接続、JWT検証、CORS）
- ログ確認とメトリクス監視（CloudWatch Logs/Metrics）

✅ **Agent context更新完了**
- .github/agents/copilot-instructions.md更新
- 言語: Java 21 (LTS) / TypeScript 5.x
- データベース: AWS DynamoDB (単一テーブル設計)
- プロジェクトタイプ: Webアプリケーション (Backend + Frontend 分離構成)

---

## Phase 1後の憲章チェック（再評価）

### 原則I: ドメイン駆動設計 (DDD)
- [x] data-model.mdでドメインエンティティ（User/AuthSession）を定義
- [x] ドメインロジック（バリデーション、状態更新）をdomain/modelに配置
- [x] リポジトリインターフェース（UserRepository）をdomain層に、実装をinfrastructure層に配置
- [x] 4層構造（application/domain/infrastructure/presentation）を厳守

### 原則II: テスト駆動開発 (TDD)【非交渉】
- [x] spec.mdに受け入れテストシナリオ記載済み
- [x] quickstart.mdでテスト実行手順を記載（JUnit 5, Jest, Playwright）
- [x] テストカバレッジ目標（domain層90%以上）を明記

### 原則III: AIペアプログラミング
- [x] research.md, data-model.md, contracts/をAIが生成
- [x] 技術スタック（Spring Boot/React/DynamoDB）を憲章に従って固定

### 原則IV: 段階的機能実装
- [x] ログイン認証はMVP（P1）機能として実装
- [x] spec.mdで優先度P1を明記

### 原則V: スケーラビリティと可観測性
- [x] パフォーマンス目標（300ユーザー、2秒以内認証）を満たす設計
- [x] 構造化ログ（JSON）でCloudWatch Logs出力を設計
- [x] quickstart.mdでCloudWatch Metricsによる監視手順を記載

### 原則VI: 個人情報保護とプライバシー【非交渉】
- [x] research.mdでLoggingUtil.maskEmail/maskName関数を定義
- [x] contracts/でUserResponseにemail/googleIdを含めない設計
- [x] data-model.mdでログ出力時のマスキング要件を明記

**GATE判定**: ✅ **合格** - Phase 1設計後も憲章原則を満たしています。

---

## 次のステップ

**Phase 2: タスク分割**

`/speckit.tasks`コマンドを実行してタスク分割を行い、TDD手順（テスト作成 → 実装）に従った実装計画を生成してください。

**推奨タスク順序**:
1. domain層（User/AuthSessionエンティティ、単体テスト）
2. infrastructure層（DynamoDbUserRepository、統合テスト）
3. application層（AuthService/UserService、ユースケーステスト）
4. presentation層（AuthController/UserController、APIテスト）
5. frontend層（LoginPage/GoogleLoginButton、E2Eテスト）
