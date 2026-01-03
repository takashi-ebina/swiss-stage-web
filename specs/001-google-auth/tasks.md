# Tasks: Google OAuth2ログイン認証

**入力**: `/specs/001-google-auth/` にある設計ドキュメント  
**Branch**: `001-google-auth`  
**作成日**: 2025-12-31

**前提条件**:
- ✅ plan.md（実装計画完了）
- ✅ spec.md（3つのユーザーストーリー: US1=ログイン、US2=エラーハンドリング、US3=アカウント削除）
- ✅ research.md（技術調査完了）
- ✅ data-model.md（User/AuthSessionエンティティ設計完了）
- ✅ contracts/（auth-api.yaml, user-api.yaml）

**TDD要件**: 
憲章原則II「テスト駆動開発【非交渉】」により、**全タスクでテストファースト（Red-Green-Refactor）を厳守**。

**整理方針**:
タスクはユーザーストーリーごとにグループ化され、各ストーリーは独立して実装・テスト可能。

---

## フォーマット

`- [ ] [ID] [P?] [Story?] 説明文`

- **[P]**: 並行実行可能（異なるファイル、依存なし）
- **[Story]**: ユーザーストーリーラベル（US1, US2, US3）
- 説明には**正確なファイルパスを含める**

---

## Phase 1: セットアップ（共有インフラ）🔧

**目的**: プロジェクト初期化と基本構造の準備

- [X] T001 プロジェクト構造をplan.mdに基づいて作成（backend/frontend分離、DDDレイヤー構造）
- [X] T002 [P] バックエンド: Gradle 8.x + Java 21 + Spring Boot 3プロジェクトを初期化（backend/build.gradle）
- [X] T003 [P] フロントエンド: Vite + React 18 + TypeScript 5プロジェクトを初期化（frontend/package.json）
- [X] T004 [P] バックエンド依存関係追加: Spring Security OAuth2 Client, JJWT, AWS SDK DynamoDB（backend/build.gradle + .env.local対応）
- [X] T005 [P] フロントエンド依存関係追加: Material-UI 5, React Router 6, Axios（frontend/package.json + @mui/icons-material）
- [X] T006 [P] Lint設定: ESLint（フロントエンド）、Checkstyle（バックエンド）
- [X] T007 シークレット管理セットアップ: backend/.env.example作成、.gitignoreに.envを追加
- [X] T008 [P] Logback設定: logback-spring.xml作成（JSON形式出力、CloudWatch Logs統合）

---

## Phase 2: 基盤（Blocking Prerequisites）⚠️

**目的**: 全ユーザーストーリーの前提となる中核インフラ

**🚨 CRITICAL**: このフェーズ完了まで、ユーザーストーリー実装は禁止

- [X] T009 DynamoDBテーブル作成: swiss_stage_table（PK/SK、オンデマンドモード、ポイントインタイムリカバリ有効化） ※SETUP.mdに手順記載
- [X] T010 [P] Spring Security基本設定: SecurityConfig.java作成（CSRF保護、HTTPS設定）
- [X] T011 [P] 個人情報マスキングユーティリティ作成: backend/src/main/java/com/swiss_stage/common/util/LoggingUtil.java（maskEmail, maskName関数）
- [X] T012 [P] 共通例外クラス作成: backend/src/main/java/com/swiss_stage/common/exception/（UnauthorizedException, BusinessException）
- [X] T013 [P] 共通DTOクラス作成: backend/src/main/java/com/swiss_stage/common/dto/ErrorResponse.java
- [X] T014 [P] CORS設定: SecurityConfig.javaにCORS設定追加（http://localhost:3000を許可）
- [X] T015 [P] 環境変数読み込み設定: application.yml, application-local.yml, application-prod.yml作成

**チェックポイント**: ✅ 基盤準備完了 → ユーザーストーリー実装を並行開始可能

---

## Phase 3: ユーザーストーリー 1 - 大会運営者のログイン（P1）🎯 MVP

**目的**: Google OAuth2認証でログインし、JWTトークンを取得してダッシュボードにアクセスする

**独立テスト**: 「Googleログインボタンクリック → Google認証画面 → 認証成功 → ダッシュボードにリダイレクト、ユーザー名表示」フローで完全にテスト可能

### ユーザーストーリー 1 のテスト【TDD厳守】⚠️

> **【憲章原則II: TDD厳守】**  
> テストは**最初に書くこと（MUST）**  
> 実装前に**FAIL（Red）することを確認する（MUST）**  
> 実装後テストが通過（Green）したら、リファクタリング

- [X] T016 [P] [US1] ドメイン層単体テスト: backend/src/test/java/com/swiss_stage/unit/domain/UserTest.java（User.create, updateLastLoginAt, バリデーション）
- [X] T017 [P] [US1] ドメイン層単体テスト: backend/src/test/java/com/swiss_stage/unit/domain/AuthSessionTest.java（AuthSession.create, isExpired, isValid）
- [X] T018 [P] [US1] アプリケーション層単体テスト: backend/src/test/java/com/swiss_stage/unit/application/UserServiceTest.java（findOrCreateUser）
- [X] T019 [P] [US1] アプリケーション層単体テスト: backend/src/test/java/com/swiss_stage/unit/application/JwtServiceTest.java（generateToken, validateTokenAndGetUserId）
- [X] T020 [US1] リポジトリ統合テスト: backend/src/test/java/com/swiss_stage/integration/repository/DynamoDbUserRepositoryTest.java（save, findById, DynamoDB Local使用）
- [ ] T021 [US1] OAuth2認証フローE2Eテスト: frontend/tests/e2e/login.spec.ts（Playwright: ログイン → ダッシュボードリダイレクト → ユーザー名表示）
- [ ] T022 [US1] ログアウトフローE2Eテスト: frontend/tests/e2e/logout.spec.ts（Playwright: ログアウト → ログイン画面リダイレクト → Cookie削除確認）

### ユーザーストーリー 1 の実装

#### バックエンド（domain層）

- [X] T023 [P] [US1] Userエンティティ作成: backend/src/main/java/com/swiss_stage/domain/model/User.java（create, updateLastLoginAt, バリデーション）
- [X] T024 [P] [US1] AuthSession値オブジェクト作成: backend/src/main/java/com/swiss_stage/domain/model/AuthSession.java（create, isExpired, isValid）
- [X] T025 [P] [US1] UserRepositoryインターフェース作成: backend/src/main/java/com/swiss_stage/domain/repository/UserRepository.java（findById, findByGoogleId, save, deleteById）

#### バックエンド（infrastructure層）

- [X] T026 [US1] DynamoDbUserRepository実装: backend/src/main/java/com/swiss_stage/infrastructure/repository/DynamoDbUserRepository.java（UserRepositoryの実装、PK/SK設計準拠）
- [X] T027 [P] [US1] DynamoDB設定クラス作成: backend/src/main/java/com/swiss_stage/infrastructure/config/DynamoDbConfig.java（DynamoDbClientビルド、エンドポイント設定）

#### バックエンド（application層）

- [X] T028 [US1] UserService作成: backend/src/main/java/com/swiss_stage/application/service/UserService.java（findOrCreateUser、既存ユーザーはlastLoginAt更新、新規ユーザーは自動登録）
- [X] T029 [P] [US1] JwtService作成: backend/src/main/java/com/swiss_stage/application/service/JwtService.java（generateToken, validateTokenAndGetUserId、HS256署名）
- [X] T030 [P] [US1] UserDto作成: backend/src/main/java/com/swiss_stage/application/dto/UserDto.java（userId, displayName, createdAt, lastLoginAt）

#### バックエンド（presentation層）

- [X] T031 [US1] OAuth2AuthenticationSuccessHandler作成: backend/src/main/java/com/swiss_stage/presentation/handler/OAuth2AuthenticationSuccessHandler.java（JWT生成、HTTP-only Cookie設定、ダッシュボードリダイレクト）
- [X] T032 [P] [US1] AuthController作成: backend/src/main/java/com/swiss_stage/presentation/controller/AuthController.java（GET /api/auth/me、POST /api/auth/logout）
- [X] T033 [P] [US1] JwtAuthenticationFilter作成: backend/src/main/java/com/swiss_stage/presentation/filter/JwtAuthenticationFilter.java（Cookie → JWT検証 → SecurityContext設定）
- [X] T034 [US1] SecurityConfig完成: OAuth2Login設定追加（successHandler, loginPage, defaultSuccessUrl）、JwtAuthenticationFilterをフィルターチェーンに追加

#### フロントエンド

- [X] T035 [P] [US1] Axiosインスタンス作成: frontend/src/utils/apiClient.ts（baseURL設定、withCredentials: true）
- [X] T036 [P] [US1] User型定義: frontend/src/types/User.ts（userId, displayName, createdAt, lastLoginAt）
- [X] T037 [P] [US1] authServiceモジュール作成: frontend/src/services/authService.ts（getCurrentUser, logout API呼び出し）
- [X] T038 [P] [US1] useAuthフック作成: frontend/src/hooks/useAuth.ts（認証状態管理、ログイン/ログアウト処理）
- [X] T039 [P] [US1] GoogleLoginButtonコンポーネント作成: frontend/src/components/auth/GoogleLoginButton.tsx（Material-UI Button、/oauth2/authorization/googleへリンク）
- [X] T040 [P] [US1] LogoutButtonコンポーネント作成: frontend/src/components/auth/LogoutButton.tsx（Material-UI Button、authService.logout呼び出し）
- [X] T041 [US1] LoginPage作成: frontend/src/pages/LoginPage.tsx（中央配置、GoogleLoginButton表示、エラーメッセージ表示）
- [X] T042 [US1] DashboardPage作成: frontend/src/pages/DashboardPage.tsx（ヘッダーにユーザー名表示、LogoutButton配置）
- [X] T043 [US1] ルーティング設定: frontend/src/App.tsx（React Router: /login, /dashboard, 未認証時は/loginへリダイレクト）

#### ログ・監視

- [X] T044 [P] [US1] ログ追加（バックエンド）: OAuth2AuthenticationSuccessHandler、UserService、JwtServiceにuserIdのみ記録（個人情報マスキング適用）
- [X] T045 [P] [US1] ログ追加（フロントエンド）: authService.tsにconsole.info（userId記録、email/displayNameは出力しない）

**チェックポイント**: ✅ ユーザーストーリー1は**単体で完全に動作しテスト可能**

---

## Phase 4: ユーザーストーリー 2 - 認証エラーハンドリング（P1）⚠️

**目的**: Google認証失敗時にユーザーフレンドリーなエラーメッセージを表示する

**独立テスト**: 「Google認証でキャンセル/拒否 → エラーメッセージ表示 → ログイン画面に戻る」フローで独立してテスト可能

### ユーザーストーリー 2 のテスト【TDD厳守】⚠️

- [ ] T046 [P] [US2] OAuth2エラーハンドリング単体テスト: backend/src/test/java/com/swiss_stage/unit/presentation/OAuth2AuthenticationFailureHandlerTest.java（access_denied, network_error, invalid_client）
- [ ] T047 [US2] 認証エラーE2Eテスト: frontend/tests/e2e/login-error.spec.ts（Playwright: 認証キャンセル → エラーメッセージ表示確認）

### ユーザーストーリー 2 の実装

- [X] T048 [US2] OAuth2AuthenticationFailureHandler作成: backend/src/main/java/com/swiss_stage/presentation/handler/OAuth2AuthenticationFailureHandler.java（エラーコード判定、ユーザーフレンドリーなメッセージ生成、ログイン画面リダイレクト）
- [X] T049 [US2] SecurityConfig更新: OAuth2Login設定にfailureHandler追加
- [X] T050 [P] [US2] LoginPage更新: frontend/src/pages/LoginPage.tsx（URLクエリパラメータerrorを取得、エラーメッセージ表示）
- [X] T051 [P] [US2] ログ追加: OAuth2AuthenticationFailureHandlerにerrorCode記録（CloudWatch Logs）

**チェックポイント**: ✅ ユーザーストーリー2は**単体で完全に動作しテスト可能**

---

## Phase 5: ユーザーストーリー 3 - アカウント削除（P1）🗑️

**目的**: ユーザーが自身のアカウントと全トーナメントデータを完全に削除できるようにする（GDPR準拠）

**独立テスト**: 「アカウント設定 → 削除ボタン → 2段階確認 → 削除実行 → ログイン画面リダイレクト」フローで独立してテスト可能

### ユーザーストーリー 3 のテスト【TDD厳守】⚠️

- [X] T052 [P] [US3] アプリケーション層単体テスト: backend/src/test/java/com/swiss_stage/unit/application/UserServiceTest.java（deleteAccount: 正常系、進行中トーナメント存在時エラー）
- [X] T053 [P] [US3] プレゼンテーション層単体テスト: backend/src/test/java/com/swiss_stage/unit/presentation/UserControllerTest.java（DELETE /api/users/{userId}: 正常系、メール不一致、進行中トーナメント存在）
- [X] T054 [US3] アカウント削除E2Eテスト: frontend/tests/e2e/delete-account.spec.ts（Playwright: アカウント設定 → メールアドレス再入力 → 削除確認 → ログイン画面リダイレクト）
- [X] T055 [P] [US3] 進行中トーナメント存在時エラーE2Eテスト: frontend/tests/e2e/delete-account-error.spec.ts（Playwright: 進行中トーナメント存在 → エラーメッセージ表示）

### ユーザーストーリー 3 の実装

#### バックエンド（application層）

- [X] T056 [US3] UserService更新: backend/src/main/java/com/swiss_stage/application/service/UserService.java（deleteAccountメソッド追加: 進行中トーナメントチェック、ユーザー削除、トーナメントデータaカスケード削除）
- [X] T057 [P] [US3] DeleteAccountRequest DTO作成: backend/src/main/java/com/swiss_stage/application/dto/DeleteAccountRequest.java（email, confirmation）

#### バックエンド（presentation層）

- [X] T058 [US3] UserController作成: backend/src/main/java/com/swiss_stage/presentation/controller/UserController.java（GET /api/users/{userId}、DELETE /api/users/{userId}）
- [X] T059 [US3] SecurityConfig更新: DELETE /api/users/{userId}を認証必須に設定、自分のuserIdのみ削除可能なアクセス制御

#### フロントエンド

- [X] T060 [P] [US3] userServiceモジュール作成: frontend/src/services/userService.ts（deleteAccount API呼び出し）
- [X] T061 [P] [US3] DeleteAccountDialogコンポーネント作成: frontend/src/components/account/DeleteAccountDialog.tsx（Material-UI Dialog、2段階確認、メールアドレス再入力、削除実行ボタン）
- [X] T062 [US3] AccountSettingsPage作成: frontend/src/pages/AccountSettingsPage.tsx（アカウント削除ボタン、DeleteAccountDialog表示）
- [X] T063 [US3] ルーティング更新: frontend/src/App.tsx（/account-settingsルート追加）

#### ログ・監視

- [X] T064 [P] [US3] ログ追加: UserService.deleteAccountにuserId記録（監査ログ、CloudWatch Logs）、削除成功/失敗イベント記録

**チェックポイント**: ✅ ユーザーストーリー3は**単体で完全に動作しテスト可能**

---

## Phase 6: Polish & Cross-Cutting Concerns✨

**目的**: 横断的関心事とユーザー体験の向上

- [ ] T065 [P] E2Eテスト統合実行スクリプト作成: frontend/package.jsonにtest:e2e:allコマンド追加（全E2Eテストを順次実行）
- [X] T066 [P] ローディングインジケーター追加: frontend/src/components/common/LoadingIndicator.tsx（Material-UI CircularProgress、API呼び出し中に表示）
- [X] T067 [P] Toastメッセージ統合: frontend/src/contexts/ToastContext.tsx（成功/エラーメッセージ表示、Material-UI Snackbar使用）
- [X] T068 [P] セッションタイムアウトハンドリング: frontend/src/hooks/useAuth.ts（JWT有効期限切れ時にログイン画面リダイレクト）
- [ ] T069 [P] CloudWatch Metricsカスタムメトリクス追加: backend/src/main/java/com/swiss_stage/infrastructure/metrics/（認証成功率、JWT検証エラー数）
- [X] T070 [P] README.md更新: quickstart.mdの内容を反映（セットアップ手順、テスト実行、トラブルシューティング）
- [ ] T071 統合動作確認: ローカル環境でログイン → ダッシュボード → アカウント削除フローを手動テスト

---

## 依存関係グラフ（User Story完了順序）

```
Phase 1 (Setup) 
    ↓
Phase 2 (Foundational)
    ↓
    ├─→ Phase 3 (US1: ログイン) ✅ 最優先
    │       ↓
    ├─→ Phase 4 (US2: エラーハンドリング) ✅ US1に依存
    │       ↓
    └─→ Phase 5 (US3: アカウント削除) ✅ US1に依存
            ↓
        Phase 6 (Polish)
```

**並行実行可能な箇所**:
- Phase 2内の[P]タスク（T010-T015）
- Phase 3内の[P]タスク（T016-T045、ただしドメイン層 → インフラ層 → アプリケーション層の順序は守る）
- Phase 4と Phase 5は US1完了後に並行実行可能

---

## 実装戦略

### MVP First（推奨スコープ）
**Phase 1 → Phase 2 → Phase 3（US1: ログイン）まで実装**でMVP完成。
US2とUS3は次のイテレーションで追加可能。

### TDD Workflow（全タスク共通）
1. **Red**: テストコード作成 → テスト実行 → FAIL確認
2. **Green**: 最小限の実装でテスト通過
3. **Refactor**: コード改善、リファクタリング
4. **Repeat**: 次のテストに進む

### パラレル実装例（US1内）
```
開発者A: T023-T025（domain層） → T026-T027（infrastructure層）
開発者B: T035-T040（フロントエンド共通） → T041-T043（ページ実装）
開発者C: T016-T020（テスト作成） → T021-T022（E2Eテスト）
```

---

## タスク完了基準

各タスクは以下を満たした時点で完了とする:
- ✅ コードがplan.mdのDDD構造に準拠
- ✅ テストが全て通過（Green）
- ✅ 個人情報マスキングが適用（email/displayName出力禁止）
- ✅ ログにuserIdのみ記録
- ✅ コミットメッセージが明確（Conventional Commits形式推奨）

---

## 参考資料

- [仕様書](spec.md): 3つのユーザーストーリーと17の機能要件
- [実装計画](plan.md): 技術スタック、憲章チェック、プロジェクト構造
- [技術調査](research.md): Spring Security OAuth2、JWT、シークレット管理
- [データモデル](data-model.md): User/AuthSessionエンティティ、DynamoDBスキーマ
- [API仕様](contracts/): auth-api.yaml, user-api.yaml
- [クイックスタート](quickstart.md): セットアップ手順、テスト実行
- [憲章](../../.specify/memory/constitution.md): DDD、TDD、個人情報保護原則
