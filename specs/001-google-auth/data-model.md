# データモデル: Google OAuth2ログイン認証

**日付**: 2025-12-31  
**機能**: Google OAuth2ログイン認証  
**ストレージ**: AWS DynamoDB (単一テーブル設計)

---

## DynamoDBテーブル設計

### メインテーブル: `swiss_stage_table`

単一テーブル設計パターンを採用し、すべてのエンティティを1つのテーブルに格納します。

**テーブル設定**:
- **スループットモード**: オンデマンド（予算重視、予測不可能なトラフィック対応）
- **暗号化**: AWS管理キー（デフォルト）
- **ポイントインタイムリカバリ**: 有効（データ保護）

---

## エンティティ設計

### 1. User（ユーザー）

大会運営者を表すドメインエンティティ。Google OAuth2認証により自動登録される。

#### DynamoDBスキーマ

| 属性名 | 型 | 説明 | 例 |
|--------|------|------|-----|
| **PK** (Partition Key) | String | `USER#{userId}` | `USER#550e8400-e29b-41d4-a716-446655440000` |
| **SK** (Sort Key) | String | `METADATA` | `METADATA` |
| userId | String (UUID) | ユーザーの一意識別子 | `550e8400-e29b-41d4-a716-446655440000` |
| googleId | String | Google OAuth2のSub（ユーザー識別子） | `102345678901234567890` |
| email | String | Googleアカウントのメールアドレス | `user@example.com` |
| displayName | String | Googleアカウントの表示名 | `山田太郎` |
| createdAt | Number (Epoch millis) | アカウント作成日時 | `1704063600000` |
| lastLoginAt | Number (Epoch millis) | 最終ログイン日時 | `1704063600000` |

#### 不変条件（Invariants）
- `userId`は必須かつ一意（UUID v4形式）
- `googleId`は必須かつ一意（Google OAuth2のSub）
- `email`は必須（メールアドレス形式）
- `displayName`は1文字以上100文字以下
- `createdAt`と`lastLoginAt`は必須

#### ドメインロジック（domain/model/User.java）

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private UUID userId;
    private String googleId;
    private String email;
    private String displayName;
    private Instant createdAt;
    private Instant lastLoginAt;
    
    // ファクトリメソッド（新規ユーザー作成）
    public static User create(UUID userId, String googleId, String email, String displayName) {
        validateGoogleId(googleId);
        validateEmail(email);
        validateDisplayName(displayName);
        
        Instant now = Instant.now();
        return new User(userId, googleId, email, displayName, now, now);
    }
    
    // 最終ログイン日時更新
    public void updateLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    // バリデーション
    private static void validateGoogleId(String googleId) {
        if (googleId == null || googleId.isEmpty()) {
            throw new IllegalArgumentException("Google ID must not be empty");
        }
    }
    
    private static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    private static void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty() || displayName.length() > 100) {
            throw new IllegalArgumentException("Display name must be 1-100 characters");
        }
    }
}
```

#### アクセスパターン

| アクセスパターン | 操作 | キー |
|----------------|------|------|
| ユーザーID検索 | GetItem | PK=`USER#{userId}`, SK=`METADATA` |
| ユーザー作成 | PutItem | PK=`USER#{userId}`, SK=`METADATA` |
| ユーザー更新（最終ログイン日時） | UpdateItem | PK=`USER#{userId}`, SK=`METADATA` |
| ユーザー削除 | DeleteItem | PK=`USER#{userId}`, SK=`METADATA` |

**注意**: Google IDでの検索は初回ログイン時に不要（findOrCreateパターンで新規作成）。将来的にGoogle ID検索が必要な場合はGSI追加を検討。

---

### 2. AuthSession（認証セッション）

認証セッション情報を表す値オブジェクト。JWTトークンとしてHTTP-only Cookieに保存されるため、DynamoDBには保存しない（インメモリ）。

#### データ構造（domain/model/AuthSession.java）

```java
@Getter
@AllArgsConstructor
public class AuthSession {
    private String jwtToken;
    private UUID userId;
    private Instant expiresAt;
    
    // ファクトリメソッド（JWT生成時に作成）
    public static AuthSession create(String jwtToken, UUID userId, Duration validity) {
        Instant expiresAt = Instant.now().plus(validity);
        return new AuthSession(jwtToken, userId, expiresAt);
    }
    
    // 有効期限チェック
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    // トークン検証（署名検証はJwtServiceで実施）
    public boolean isValid() {
        return jwtToken != null && !jwtToken.isEmpty() && !isExpired();
    }
}
```

#### 不変条件
- `jwtToken`は必須（HS256署名済み）
- `userId`は必須（User.userIdと一致）
- `expiresAt`は発行時刻から24時間後
- 値オブジェクトのため変更不可（Immutable）

---

## DynamoDB GSI（将来拡張用）

現時点ではGSI不要。将来的に以下のアクセスパターンが必要になった場合に追加を検討：

### GSI1: Google ID検索用（オプション）

| 属性名 | 型 | 説明 |
|--------|------|------|
| **GSI1PK** | String | `GOOGLE_ID#{googleId}` |
| **GSI1SK** | String | `USER` |

**使用ケース**: Google IDから既存ユーザーを検索（初回ログイン時の重複チェック）

**現時点で不要な理由**: findOrCreateパターンでは毎回新規作成判定を行うため、Google ID検索なしで実装可能。

---

## リポジトリインターフェース（domain層）

```java
// domain/repository/UserRepository.java
public interface UserRepository {
    Optional<User> findById(UUID userId);
    Optional<User> findByGoogleId(String googleId); // 将来的にGSI1で実装
    User save(User user);
    void deleteById(UUID userId);
}
```

---

## DynamoDB操作例（infrastructure層）

```java
// infrastructure/repository/DynamoDbUserRepository.java
@Repository
public class DynamoDbUserRepository implements UserRepository {
    
    @Autowired
    private DynamoDbClient dynamoDbClient;
    
    private static final String TABLE_NAME = "swiss_stage_table";
    
    @Override
    public Optional<User> findById(UUID userId) {
        Map<String, AttributeValue> key = Map.of(
            "PK", AttributeValue.builder().s("USER#" + userId).build(),
            "SK", AttributeValue.builder().s("METADATA").build()
        );
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();
        
        GetItemResponse response = dynamoDbClient.getItem(request);
        
        if (!response.hasItem()) {
            return Optional.empty();
        }
        
        return Optional.of(mapToUser(response.item()));
    }
    
    @Override
    public User save(User user) {
        Map<String, AttributeValue> item = Map.of(
            "PK", AttributeValue.builder().s("USER#" + user.getUserId()).build(),
            "SK", AttributeValue.builder().s("METADATA").build(),
            "userId", AttributeValue.builder().s(user.getUserId().toString()).build(),
            "googleId", AttributeValue.builder().s(user.getGoogleId()).build(),
            "email", AttributeValue.builder().s(user.getEmail()).build(),
            "displayName", AttributeValue.builder().s(user.getDisplayName()).build(),
            "createdAt", AttributeValue.builder().n(String.valueOf(user.getCreatedAt().toEpochMilli())).build(),
            "lastLoginAt", AttributeValue.builder().n(String.valueOf(user.getLastLoginAt().toEpochMilli())).build()
        );
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        
        dynamoDbClient.putItem(request);
        return user;
    }
    
    @Override
    public void deleteById(UUID userId) {
        Map<String, AttributeValue> key = Map.of(
            "PK", AttributeValue.builder().s("USER#" + userId).build(),
            "SK", AttributeValue.builder().s("METADATA").build()
        );
        
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();
        
        dynamoDbClient.deleteItem(request);
    }
    
    private User mapToUser(Map<String, AttributeValue> item) {
        return new User(
            UUID.fromString(item.get("userId").s()),
            item.get("googleId").s(),
            item.get("email").s(),
            item.get("displayName").s(),
            Instant.ofEpochMilli(Long.parseLong(item.get("createdAt").n())),
            Instant.ofEpochMilli(Long.parseLong(item.get("lastLoginAt").n()))
        );
    }
}
```

---

## データ整合性とトランザクション

### 整合性保証
- **ユーザー作成**: PutItemは冪等操作、同じuserIdで複数回実行しても最後の書き込みが有効（Last Write Wins）
- **アカウント削除**: DeleteItemで物理削除、復元不可（GDPR準拠）

### トランザクション不要
- ユーザー作成・更新は単一エンティティ操作のみ
- 将来的にトーナメントデータとの連携が必要な場合はDynamoDB Transactionsを検討

---

## パフォーマンス考慮事項

### 読み取り
- **GetItem**: 一貫性のある読み取り（Consistent Read）を使用、レイテンシ < 10ms
- **キャパシティユニット**: 1 RCU = 最大4KB、Userエンティティは約200バイト（1 RCUで20ユーザー読み取り可能）

### 書き込み
- **PutItem**: 1 WCU = 最大1KB、Userエンティティは約200バイト（1 WCUで5ユーザー書き込み可能）
- **同時ログイン**: 300ユーザーが同時ログインしても300 WCU/秒（オンデマンドモードで自動スケール）

### コスト見積もり
- **月間ログイン**: 300ユーザー × 30回/月 = 9,000回
- **読み取りコスト**: 9,000回 × 1 RCU = 0.00025ドル/月（無料枠内）
- **書き込みコスト**: 9,000回 × 1 WCU = 0.01125ドル/月（無料枠内）

---

## テストデータ例

```json
{
  "PK": "USER#550e8400-e29b-41d4-a716-446655440000",
  "SK": "METADATA",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "googleId": "102345678901234567890",
  "email": "yamada@example.com",
  "displayName": "山田太郎",
  "createdAt": 1704063600000,
  "lastLoginAt": 1704063600000
}
```

---

## マイグレーション戦略

### 初期セットアップ
1. AWS CLIまたはTerraformでDynamoDBテーブル作成
2. オンデマンドモード設定
3. ポイントインタイムリカバリ有効化

### データマイグレーション
初期リリースのため既存データなし。

### スキーマ変更
DynamoDBはスキーマレスのため、属性追加は既存データに影響なし。ただし、エンティティクラス（domain/model/User.java）の変更時は以下を実施：
1. デフォルト値を設定（新規属性用）
2. マッパー関数でnullチェック追加
3. 既存データの後方互換性を保証

---

## まとめ

本データモデル設計により、以下を実現します：

1. **DDD原則準拠**: User/AuthSessionをドメインエンティティとして定義、ビジネスロジックを含む
2. **コスト最適化**: 単一テーブル設計でRCU/WCUを最小化
3. **パフォーマンス**: GetItem/PutItemで10ms以下のレイテンシ
4. **スケーラビリティ**: オンデマンドモードで300ユーザー対応
5. **個人情報保護**: DynamoDBには個人情報を保存するが、ログ出力時にはマスキング（research.mdで定義）
