# クイックスタートガイド: Google OAuth2ログイン認証

**対象**: 開発者  
**前提条件**: Java 21, Node.js 18+, AWS CLI設定済み, Docker（オプション）

---

## セットアップ手順

### 1. Google Cloud Console設定

#### 1.1 プロジェクト作成

```bash
# ブラウザでGoogle Cloud Consoleにアクセス
# https://console.cloud.google.com/

# 1. 新規プロジェクト作成: "swiss-stage-web"
# 2. プロジェクトを選択
```

#### 1.2 OAuth 2.0認証情報の取得

```bash
# APIとサービス > 認証情報 > 認証情報を作成 > OAuth 2.0 クライアントID

# アプリケーションの種類: Webアプリケーション
# 名前: swiss-stage-web-oauth
# 承認済みのリダイレクトURI:
#   - http://localhost:8080/login/oauth2/code/google （開発環境）
#   - https://your-domain.com/login/oauth2/code/google （本番環境）

# クライアントIDとクライアントシークレットをメモ
```

#### 1.3 OAuth同意画面の設定

```bash
# APIとサービス > OAuth同意画面

# ユーザータイプ: 外部
# アプリ名: Swiss Stage Web
# ユーザーサポートメール: your-email@example.com
# スコープ: 
#   - openid
#   - email
#   - profile
# テストユーザー: 開発用Googleアカウントを追加
```

---

### 2. シークレット管理（重要）

#### 2.1 環境ごとの管理方針

**重要**: `client-id`と`client-secret`は環境ごとに異なる値を使用し、**Gitリポジトリには含めません**。

| 環境 | 管理方法 | コスト | セキュリティ |
|------|----------|--------|-------------|
| ローカル開発 | `.env`ファイル（.gitignoreに追加） | 無料 | ファイルシステム保護 |
| 本番環境 | AWS Systems Manager Parameter Store | 無料 | IAM + KMS暗号化 |
| CI/CD | GitHub Secrets | 無料 | GitHub暗号化ストレージ |

#### 2.2 ローカル開発環境のセットアップ

```bash
cd backend

# .gitignoreに追加（まだ追加されていない場合）
cat >> .gitignore << 'EOF'
# Environment variables
.env
.env.local
.env.*.local
EOF

# .envファイルを作成
cat > .env << EOF
# Google OAuth2（開発環境用）
GOOGLE_CLIENT_ID=your-dev-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-dev-client-secret

# JWT
JWT_SECRET_KEY=$(openssl rand -base64 32)
JWT_EXPIRATION=86400000

# DynamoDB (ローカル開発)
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_REGION=ap-northeast-1
AWS_ACCESS_KEY_ID=dummy
AWS_SECRET_ACCESS_KEY=dummy

# Spring Boot
SPRING_PROFILES_ACTIVE=local
EOF

# .env.exampleファイルを作成（チーム共有用、Gitにコミット可能）
cat > .env.example << 'EOF'
# Google OAuth2（開発環境用）
# Google Cloud Consoleで開発用OAuth 2.0クライアントIDを作成してください
# リダイレクトURI: http://localhost:8080/login/oauth2/code/google
GOOGLE_CLIENT_ID=your-dev-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-dev-client-secret

# JWT
# 以下のコマンドで生成: openssl rand -base64 32
JWT_SECRET_KEY=your-generated-secret-key
JWT_EXPIRATION=86400000

# DynamoDB (ローカル開発)
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_REGION=ap-northeast-1
AWS_ACCESS_KEY_ID=dummy
AWS_SECRET_ACCESS_KEY=dummy

# Spring Boot
SPRING_PROFILES_ACTIVE=local
EOF

echo "✅ .env と .env.example を作成しました"
echo "📝 .envファイルのGOOGLE_CLIENT_IDとGOOGLE_CLIENT_SECRETを実際の値に置き換えてください"
```

#### 2.3 本番環境のシークレット設定

**AWS Systems Manager Parameter Storeを使用**（完全無料、IAM統合）

```bash
# 本番環境用のシークレットをAWS Parameter Storeに保存

# Google OAuth2（本番環境用）
aws ssm put-parameter \
  --name /swiss-stage-web/prod/google-client-id \
  --value "prod-client-id-xxx.apps.googleusercontent.com" \
  --type String \
  --region ap-northeast-1 \
  --description "Google OAuth2 Client ID for production"

aws ssm put-parameter \
  --name /swiss-stage-web/prod/google-client-secret \
  --value "prod-client-secret-xxx" \
  --type SecureString \
  --region ap-northeast-1 \
  --description "Google OAuth2 Client Secret for production (encrypted)"

# JWT秘密鍵
aws ssm put-parameter \
  --name /swiss-stage-web/prod/jwt-secret-key \
  --value "$(openssl rand -base64 32)" \
  --type SecureString \
  --region ap-northeast-1 \
  --description "JWT signing key for production (encrypted)"

# 確認
aws ssm get-parameters \
  --names \
    /swiss-stage-web/prod/google-client-id \
    /swiss-stage-web/prod/google-client-secret \
    /swiss-stage-web/prod/jwt-secret-key \
  --with-decryption \
  --region ap-northeast-1
```

**Spring Boot設定（本番環境）**:

```yaml
# backend/src/main/resources/application-prod.yml
spring:
  config:
    import: "aws-parameterstore:/swiss-stage-web/prod/"
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${google-client-id}
            client-secret: ${google-client-secret}
            redirect-uri: "https://your-domain.com/login/oauth2/code/google"

jwt:
  secret: ${jwt-secret-key}
  expiration: 86400000
```

**依存関係追加**:

```gradle
// backend/build.gradle
dependencies {
    // AWS Parameter Store統合
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-parameter-store:3.0.0'
    
    // その他の依存関係...
}
```

**EC2インスタンスのIAM Role設定**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath"
      ],
      "Resource": "arn:aws:ssm:ap-northeast-1:YOUR_ACCOUNT_ID:parameter/swiss-stage-web/prod/*"
    },
    {
      "Effect": "Allow",
      "Action": ["kms:Decrypt"],
      "Resource": "*"
    }
  ]
}
```

#### 2.4 CI/CD（GitHub Secrets）

**GitHub Secretsの設定**:

1. GitHub Repository > Settings > Secrets and variables > Actions
2. 「New repository secret」をクリック
3. 以下のシークレットを追加:

```
PROD_GOOGLE_CLIENT_ID = prod-client-id-xxx.apps.googleusercontent.com
PROD_GOOGLE_CLIENT_SECRET = prod-client-secret-xxx
PROD_JWT_SECRET_KEY = (openssl rand -base64 32の出力)
```

**GitHub Actionsでの使用例**:

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to EC2
        env:
          GOOGLE_CLIENT_ID: ${{ secrets.PROD_GOOGLE_CLIENT_ID }}
          GOOGLE_CLIENT_SECRET: ${{ secrets.PROD_GOOGLE_CLIENT_SECRET }}
          JWT_SECRET_KEY: ${{ secrets.PROD_JWT_SECRET_KEY }}
        run: |
          # デプロイスクリプト実行
          ssh ec2-user@your-ec2-instance 'bash deploy.sh'
```

#### 2.5 Google Cloud Consoleでの環境別設定

```
開発環境用OAuth 2.0クライアントID
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
プロジェクト: swiss-stage-web
アプリケーションの種類: Webアプリケーション
名前: swiss-stage-web-dev
承認済みのリダイレクトURI:
  • http://localhost:8080/login/oauth2/code/google
  • http://localhost:3000/login/callback （フロントエンド用、オプション）

本番環境用OAuth 2.0クライアントID
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
プロジェクト: swiss-stage-web
アプリケーションの種類: Webアプリケーション
名前: swiss-stage-web-prod
承認済みのリダイレクトURI:
  • https://your-domain.com/login/oauth2/code/google
  • https://your-domain.com/login/callback （フロントエンド用、オプション）
```

**重要**: 開発環境と本番環境で異なるOAuth 2.0クライアントIDを使用することで、セキュリティ分離とテスト環境の独立性を確保します。

---

### 3. DynamoDB テーブル作成

#### 2.1 ローカル開発（DynamoDB Local）

```bash
# Docker ComposeでDynamoDB Localを起動
cd backend
docker-compose up -d dynamodb-local

# テーブル作成
aws dynamodb create-table \
  --table-name swiss_stage_table \
  --attribute-definitions \
      AttributeName=PK,AttributeType=S \
      AttributeName=SK,AttributeType=S \
  --key-schema \
      AttributeName=PK,KeyType=HASH \
      AttributeName=SK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000
```

#### 2.2 本番環境（AWS DynamoDB）

```bash
# AWS CLIでテーブル作成
aws dynamodb create-table \
  --table-name swiss_stage_table \
  --attribute-definitions \
      AttributeName=PK,AttributeType=S \
      AttributeName=SK,AttributeType=S \
  --key-schema \
      AttributeName=PK,KeyType=HASH \
      AttributeName=SK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region ap-northeast-1

# ポイントインタイムリカバリ有効化
aws dynamodb update-continuous-backups \
  --table-name swiss_stage_table \
  --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true \
  --region ap-northeast-1
```

---

### 4. バックエンド起動

#### 4.1 環境変数設定

```bash
cd backend

# .envファイルを作成
cat > .env << EOF
# Google OAuth2
GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret-here

# JWT
JWT_SECRET_KEY=$(openssl rand -base64 32)
JWT_EXPIRATION=86400000

# DynamoDB (ローカル開発)
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_REGION=ap-northeast-1
AWS_ACCESS_KEY_ID=dummy
AWS_SECRET_ACCESS_KEY=dummy

# Spring Boot
SPRING_PROFILES_ACTIVE=local
EOF
```

#### 4.2 ビルド & 起動

```bash
# Gradleビルド
./gradlew clean build

# アプリケーション起動
./gradlew bootRun

# または
java -jar build/libs/swiss-stage-web-0.0.1-SNAPSHOT.jar

# 起動確認
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

### 5. フロントエンド起動

#### 5.1 環境変数設定

```bash
cd frontend

# .env.localファイルを作成
cat > .env.local << EOF
# API Base URL
VITE_API_BASE_URL=http://localhost:8080/api

# Google OAuth2 (Spring Security経由なので不要)
# VITE_GOOGLE_CLIENT_ID は使用しない
EOF
```

#### 5.2 依存関係インストール & 起動

```bash
# 依存関係インストール
npm install

# 開発サーバー起動
npm run dev

# ビルド（本番用）
npm run build
npm run preview

# 起動確認
# ブラウザで http://localhost:3000 にアクセス
```

---

## 動作確認

### 1. ログインフロー

```bash
# 1. ブラウザで http://localhost:3000 にアクセス
# 2. 「Googleでログイン」ボタンをクリック
# 3. Googleの同意画面で「許可」をクリック
# 4. ダッシュボード画面にリダイレクトされる
# 5. ヘッダーにユーザー名が表示される
```

### 2. API動作確認

```bash
# 認証開始（ブラウザで実行）
open http://localhost:8080/oauth2/authorization/google

# 現在のユーザー情報取得（ログイン後にCookieを含めて実行）
curl -X GET http://localhost:8080/api/auth/me \
  --cookie "JWT_TOKEN=your-jwt-token-here"

# Expected:
# {
#   "userId": "550e8400-e29b-41d4-a716-446655440000",
#   "displayName": "山田太郎",
#   "createdAt": "2025-12-31T00:00:00Z",
#   "lastLoginAt": "2025-12-31T12:30:00Z"
# }

# ログアウト
curl -X POST http://localhost:8080/api/auth/logout \
  --cookie "JWT_TOKEN=your-jwt-token-here"

# Expected:
# {"message":"ログアウトしました"}
```

### 3. アカウント削除フロー

```bash
# 1. ログイン後、アカウント設定画面にアクセス
# 2. 「アカウントを削除」ボタンをクリック
# 3. 確認ダイアログでメールアドレスを再入力
# 4. 「削除を実行」ボタンをクリック
# 5. 「アカウントを削除しました」メッセージが表示され、3秒後にログイン画面にリダイレクト

# API経由での削除（開発用）
curl -X DELETE http://localhost:8080/api/users/550e8400-e29b-41d4-a716-446655440000 \
  --cookie "JWT_TOKEN=your-jwt-token-here" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "yamada@example.com",
    "confirmation": true
  }'

# Expected:
# {
#   "message": "アカウントを削除しました",
#   "redirectUrl": "/login"
# }
```

---

## テスト実行

### バックエンドテスト

```bash
cd backend

# 全テスト実行
./gradlew test

# カバレッジレポート生成
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# 単体テスト（domain層）のみ実行
./gradlew test --tests "com.swiss_stage.domain.*"

# 統合テスト（repository層）のみ実行
./gradlew test --tests "com.swiss_stage.infrastructure.repository.*"
```

### フロントエンドテスト

```bash
cd frontend

# 単体テスト（Jest）
npm test

# カバレッジレポート
npm run test:coverage

# E2Eテスト（Playwright）
npm run test:e2e

# 特定のE2Eテストのみ実行
npx playwright test tests/e2e/login.spec.ts
```

---

## トラブルシューティング

### 問題1: Google OAuth2エラー「redirect_uri_mismatch」

**原因**: Google Cloud ConsoleのリダイレクトURIが一致していない

**解決方法**:
```bash
# Google Cloud Console > APIとサービス > 認証情報
# OAuth 2.0クライアントIDの承認済みリダイレクトURIに以下を追加:
# http://localhost:8080/login/oauth2/code/google
```

### 問題2: DynamoDB接続エラー

**原因**: DynamoDB Localが起動していない、または環境変数が未設定

**解決方法**:
```bash
# DynamoDB Local起動確認
docker ps | grep dynamodb-local

# 起動していない場合
cd backend
docker-compose up -d dynamodb-local

# 環境変数確認
echo $AWS_DYNAMODB_ENDPOINT
# Expected: http://localhost:8000
```

### 問題3: JWT検証エラー「Invalid signature」

**原因**: JWT_SECRET_KEYが一致していない

**解決方法**:
```bash
# .envファイルを確認
cat backend/.env | grep JWT_SECRET_KEY

# 再生成する場合
openssl rand -base64 32

# application.ymlで環境変数を読み込んでいることを確認
# jwt.secret=${JWT_SECRET_KEY}
```

### 問題4: CORS エラー

**原因**: バックエンドでCORS設定が不足

**解決方法**:
```java
// SecurityConfig.javaに追加
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## ログ確認

### バックエンドログ

```bash
# コンソールログ（JSON形式）
tail -f backend/logs/application.log | jq

# CloudWatch Logs（本番環境）
aws logs tail /aws/ec2/swiss-stage-web --follow --format short

# 特定のuserIdでフィルタリング
aws logs filter-log-events \
  --log-group-name /aws/ec2/swiss-stage-web \
  --filter-pattern '{ $.userId = "550e8400-e29b-41d4-a716-446655440000" }'
```

### フロントエンドログ

```bash
# ブラウザのコンソールでログ確認
# Chrome DevTools > Console

# Vite開発サーバーログ
npm run dev
# ログが表示される
```

---

## 監視とメトリクス

### CloudWatch Metrics（本番環境）

```bash
# 認証成功率の確認
aws cloudwatch get-metric-statistics \
  --namespace SwissStageWeb \
  --metric-name AuthenticationSuccessRate \
  --start-time 2025-12-31T00:00:00Z \
  --end-time 2025-12-31T23:59:59Z \
  --period 3600 \
  --statistics Average

# JWT検証エラー数の確認
aws cloudwatch get-metric-statistics \
  --namespace SwissStageWeb \
  --metric-name JwtValidationErrors \
  --start-time 2025-12-31T00:00:00Z \
  --end-time 2025-12-31T23:59:59Z \
  --period 300 \
  --statistics Sum
```

### パフォーマンス監視

```bash
# Spring Boot Actuator Metricsエンドポイント
curl http://localhost:8080/actuator/metrics

# JWT検証時間
curl http://localhost:8080/actuator/metrics/jwt.validation.time

# DynamoDB読み取りレイテンシ
curl http://localhost:8080/actuator/metrics/dynamodb.read.latency
```

---

## 次のステップ

1. **テスト追加**: [spec.md](spec.md)の受け入れシナリオに基づいてE2Eテストを追加
2. **タスク分割**: `/speckit.tasks`コマンドでタスク分割を実施
3. **TDD実装開始**: テストコード作成 → 実装の順序で機能実装
4. **コードレビュー**: 憲章原則（DDD, TDD, 個人情報保護）への準拠を確認

---

## 参考資料

- [Spring Security OAuth2 Client Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth2 API Documentation](https://developers.google.com/identity/protocols/oauth2)
- [AWS DynamoDB Developer Guide](https://docs.aws.amazon.com/dynamodb/)
- [憲章](../../.specify/memory/constitution.md)
- [技術スタック詳細](../../.specify/memory/tech-stack.md)
