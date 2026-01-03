# セットアップガイド - Google OAuth2ログイン認証

このガイドでは、Google OAuth2ログイン機能をローカル環境で動作させるために必要な手順を説明します。

## 前提条件

- ✅ Docker（DynamoDB Local用）
- ✅ Java 21
- ✅ Node.js 20.x
- ✅ Gradle 8.x

---

## 1. Google Cloud Console設定

### 1.1 プロジェクト作成

1. [Google Cloud Console](https://console.cloud.google.com/)にアクセス
2. 新しいプロジェクトを作成（例: `swiss-stage-dev`）

### 1.2 OAuth 2.0クライアント設定

1. **APIとサービス** → **認証情報** → **認証情報を作成** → **OAuth クライアント ID**
2. アプリケーションの種類: **ウェブアプリケーション**
3. 名前: `Swiss Stage Local Development`
4. **承認済みのリダイレクト URI**:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
5. **作成**をクリック
6. **クライアントID**と**クライアントシークレット**をコピー（次のステップで使用）

### 1.3 OAuth同意画面設定

1. **APIとサービス** → **OAuth同意画面**
2. User Type: **外部**（テスト用）
3. アプリ情報:
   - アプリ名: `Swiss Stage`
   - ユーザーサポートメール: あなたのメールアドレス
   - デベロッパーの連絡先情報: あなたのメールアドレス
4. スコープ: `email`, `profile`を追加
5. テストユーザーを追加（あなたのGoogleアカウント）

---

## 2. バックエンド設定

### 2.1 環境変数ファイル作成

```bash
cd backend
cp .env.example .env.local
```

### 2.2 .env.localを編集

```bash
# Google OAuth2 Settings（Google Cloud Consoleからコピー）
GOOGLE_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-actual-client-secret

# Google Redirect URI
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# JWT Secret（本番環境では必ず変更！）
JWT_SECRET_KEY=dev-secret-key-minimum-256-bits-please-change-in-production-12345678

# JWT有効期限（時間）
JWT_EXPIRATION_HOURS=24

# AWS DynamoDB Local
AWS_REGION=ap-northeast-1
DYNAMODB_ENDPOINT=http://localhost:8000
DYNAMODB_TABLE_NAME=swiss_stage_table

# Server Port
SERVER_PORT=8080

# Frontend URL
FRONTEND_URL=http://localhost:3000

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_SWISS_STAGE=DEBUG
```

**重要**: 
- `GOOGLE_CLIENT_ID`と`GOOGLE_CLIENT_SECRET`は実際の値に置き換えてください
- `JWT_SECRET_KEY`は256ビット以上のランダムな文字列を使用してください

---

## 3. フロントエンド設定

### 3.1 環境変数ファイル作成

```bash
cd frontend
cp .env.example .env.local
```

### 3.2 .env.localを編集

```bash
# Backend API URL
VITE_API_BASE_URL=http://localhost:8080
```

---

## 4. DynamoDB Local起動

### 4.1 Dockerコンテナ起動

```bash
docker run -d -p 8000:8000 --name dynamodb-local amazon/dynamodb-local
```

### 4.2 テーブル作成

```bash
aws dynamodb create-table \
    --table-name swiss_stage_table \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
        AttributeName=GSI1PK,AttributeType=S \
        AttributeName=GSI1SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --global-secondary-indexes \
        "[{\"IndexName\":\"GSI1\",\"KeySchema\":[{\"AttributeName\":\"GSI1PK\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"GSI1SK\",\"KeyType\":\"RANGE\"}],\"Projection\":{\"ProjectionType\":\"ALL\"},\"ProvisionedThroughput\":{\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}}]" \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url http://localhost:8000
```

### 4.3 テーブル確認

```bash
aws dynamodb list-tables --endpoint-url http://localhost:8000
```

**期待される出力**:
```json
{
    "TableNames": [
        "swiss_stage_table"
    ]
}
```

---

## 5. アプリケーション起動

### 5.1 バックエンド起動

```bash
cd backend
./gradlew bootRun
```

**正常起動時のログ**:
```
Started SwissStageApplication in X.XXX seconds
```

### 5.2 フロントエンド起動（別ターミナル）

```bash
cd frontend
npm install  # 初回のみ
npm run dev
```

**アクセスURL**: http://localhost:3000

---

## 6. 動作確認

### 6.1 ログイン機能テスト

1. http://localhost:3000 にアクセス
2. **Googleでログイン**ボタンをクリック
3. Google認証画面でテストユーザーアカウントを選択
4. ダッシュボードにリダイレクトされることを確認
5. ヘッダーにユーザー名が表示されることを確認

### 6.2 アカウント削除テスト

1. ダッシュボードの設定アイコンをクリック
2. **アカウントを削除**ボタンをクリック
3. メールアドレスと"DELETE"を入力
4. 削除確認後、ログイン画面にリダイレクトされることを確認

---

## トラブルシューティング

### エラー: `invalid_client`

**原因**: Google OAuth2クライアントIDまたはシークレットが正しくない

**対処**:
1. Google Cloud Consoleで認証情報を確認
2. `.env.local`の`GOOGLE_CLIENT_ID`と`GOOGLE_CLIENT_SECRET`を確認
3. バックエンドを再起動

### エラー: `redirect_uri_mismatch`

**原因**: リダイレクトURIが一致しない

**対処**:
1. Google Cloud Consoleで承認済みリダイレクトURIに以下を追加:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
2. 設定保存後、数分待ってから再試行

### エラー: `DynamoDB table not found`

**原因**: DynamoDBテーブルが作成されていない

**対処**:
```bash
# テーブル一覧確認
aws dynamodb list-tables --endpoint-url http://localhost:8000

# テーブルがない場合は「4.2 テーブル作成」を実行
```

### エラー: `Connection refused (DynamoDB)`

**原因**: DynamoDB Localが起動していない

**対処**:
```bash
# コンテナ起動確認
docker ps | grep dynamodb

# 起動していない場合
docker run -d -p 8000:8000 --name dynamodb-local amazon/dynamodb-local
```

### エラー: `JWT token invalid`

**原因**: JWT秘密鍵が一致しない、またはトークンが期限切れ

**対処**:
1. ブラウザのCookieをクリア
2. 再度ログイン
3. `.env.local`の`JWT_SECRET_KEY`が256ビット以上であることを確認

---

## テスト実行

### バックエンド単体テスト

```bash
cd backend
./gradlew test
```

### フロントエンド単体テスト

```bash
cd frontend
npm test
```

---

## 次のステップ

- [ ] 本番環境用のGoogle OAuth2クライアント設定
- [ ] AWS DynamoDBテーブル作成（本番環境）
- [ ] CI/CDパイプライン構築
- [ ] E2Eテスト実装

---

## 参考資料

- [Google OAuth2ドキュメント](https://developers.google.com/identity/protocols/oauth2)
- [Spring Security OAuth2ガイド](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [DynamoDB Localドキュメント](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)
