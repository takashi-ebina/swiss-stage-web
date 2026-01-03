# 技術調査: Google OAuth2ログイン認証

**日付**: 2025-12-31  
**対象機能**: Google OAuth2ログイン認証  
**調査者**: AI Agent

---

## 1. Spring Security OAuth2 Client設定

### Decision
Spring Boot 3の`spring-boot-starter-oauth2-client`を使用し、`application.yml`でGoogle OAuth2設定を宣言的に記述する。

### Rationale
- Spring Boot 3のOAuth2自動設定により、最小限のコードで標準準拠の実装が可能
- `application.yml`での設定により環境変数での上書きが容易（本番/開発環境切り替え）
- Spring Securityのデフォルトエンドポイント（`/oauth2/authorization/google`, `/login/oauth2/code/google`）を活用できる

### 実装例

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid
              - email
              - profile
            redirect-uri: "{baseUrl}/login/oauth2/code/google"
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
            user-name-attribute: sub
```

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/login/**", "/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2AuthenticationSuccessHandler())
                .failureHandler(oAuth2AuthenticationFailureHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JWT_TOKEN")
            )
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        
        return http.build();
    }
}
```

### Alternatives Considered
- **手動でOAuth2フロー実装**: Spring Securityの自動設定を使わず、HTTPリクエストを直接送信
  - 却下理由: 実装コスト大、セキュリティリスク（CSRF/state検証の実装ミス）、保守性低下
- **Spring Boot 2.x系**: 古いバージョンを使用
  - 却下理由: Java 21 (LTS)との互換性、セキュリティパッチサポート期間

---

## 2. JWT生成・検証

### Decision
`io.jsonwebtoken:jjwt-api`ライブラリを使用し、HS256アルゴリズムでJWT署名を行う。秘密鍵は環境変数で管理し、有効期限は24時間に設定する。

### Rationale
- HS256は共通鍵暗号方式で署名検証が高速（RS256より10倍高速）
- 単一サーバー構成では鍵配布の複雑性が不要
- JJWTライブラリは標準仕様（RFC 7519）準拠で広く使用されている

### 実装例

```java
// JwtService.java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}") // 24時間（ミリ秒）
    private long jwtExpiration;
    
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .setSubject(user.getUserId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
    
    public String validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token", e);
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
```

```properties
# application-prod.properties（本番環境）
jwt.secret=${JWT_SECRET_KEY}  # 環境変数から読み込み
jwt.expiration=86400000       # 24時間
```

### Alternatives Considered
- **RS256（公開鍵暗号方式）**: 
  - 利点: マイクロサービス間で公開鍵を共有可能
  - 却下理由: 単一サーバー構成では過剰、署名検証が遅い（300名規模では問題化）
- **セッションベース認証（Redis/DynamoDB）**:
  - 却下理由: ステートフル、DynamoDB読み取りコスト増加、スケーラビリティ低下

---

## 3. セッション保存方法（HTTP-only Cookie vs LocalStorage）

### Decision
JWTトークンをHTTP-only Cookieに保存する。

### Rationale
- **XSS攻撃対策**: HTTP-only属性によりJavaScriptからのアクセス不可、XSS攻撃でトークン盗難を防止
- **Secure属性**: HTTPS環境でのみ送信、中間者攻撃を防止
- **SameSite属性**: CSRF攻撃を軽減（`SameSite=Strict`または`Lax`）
- **自動送信**: ブラウザがAPIリクエスト時に自動付与、フロントエンド実装がシンプル

### 実装例

```java
// OAuth2AuthenticationSuccessHandler.java
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        
        // ユーザー情報をDynamoDBに保存（初回登録またはログイン日時更新）
        User user = userService.findOrCreateUser(email, oAuth2User);
        
        // JWT生成
        String jwtToken = jwtService.generateToken(user);
        
        // HTTP-only Cookieに保存
        Cookie jwtCookie = new Cookie("JWT_TOKEN", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true); // HTTPS必須
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(86400); // 24時間
        jwtCookie.setAttribute("SameSite", "Strict");
        response.addCookie(jwtCookie);
        
        // フロントエンドのダッシュボードにリダイレクト
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/dashboard");
    }
}
```

### Alternatives Considered
- **LocalStorage**:
  - 利点: CORS設定が不要、サブドメイン間共有不要
  - 却下理由: XSS攻撃でJavaScriptからアクセス可能、OWASPがアンチパターンとして非推奨
- **SessionStorage**:
  - 却下理由: タブ/ウィンドウごとに独立、ユーザー体験低下（別タブでログインが必要）

---

## 4. DynamoDB User保存パターン

### Decision
単一テーブル設計で`PK=USER#{userId}`, `SK=METADATA`のパターンを採用。初回ログイン時に`findOrCreate`パターンでユーザー自動登録を行う。

### Rationale
- 単一テーブル設計によりDynamoDBの読み取り/書き込みコストを最小化
- `SK=METADATA`により将来的にユーザーに関連する他のエンティティ（例: `SK=SESSION#{sessionId}`）を同じPK配下に格納可能
- GSI不要（email検索が不要なため）、コスト削減

### DynamoDBスキーマ

| PK (Partition Key) | SK (Sort Key) | Attributes |
|--------------------|---------------|-----------|
| USER#uuid-1234 | METADATA | `{ googleId, email, displayName, createdAt, lastLoginAt }` |

### 実装例

```java
// DynamoDbUserRepository.java
@Repository
public class DynamoDbUserRepository implements UserRepository {
    
    @Autowired
    private DynamoDbTemplate dynamoDbTemplate;
    
    @Override
    public Optional<User> findByGoogleId(String googleId) {
        // GSIで検索（GSI: googleId → userId）
        // または初回ログイン時はOptional.empty()を返してfindOrCreateで作成
        // ※spec.mdではGSI不要と判断、初回のみ新規作成
        return Optional.empty(); // 簡略化
    }
    
    @Override
    public User save(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s("USER#" + user.getUserId()).build());
        item.put("SK", AttributeValue.builder().s("METADATA").build());
        item.put("googleId", AttributeValue.builder().s(user.getGoogleId()).build());
        item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("displayName", AttributeValue.builder().s(user.getDisplayName()).build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(user.getCreatedAt().toEpochMilli())).build());
        item.put("lastLoginAt", AttributeValue.builder().n(String.valueOf(user.getLastLoginAt().toEpochMilli())).build());
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName("swiss_stage_table")
                .item(item)
                .build();
        
        dynamoDbTemplate.putItem(request);
        return user;
    }
}
```

```java
// UserService.java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User findOrCreateUser(String email, OAuth2User oAuth2User) {
        String googleId = oAuth2User.getAttribute("sub");
        String displayName = oAuth2User.getAttribute("name");
        
        return userRepository.findByGoogleId(googleId)
                .map(user -> {
                    // 既存ユーザー: lastLoginAtを更新
                    user.updateLastLoginAt(Instant.now());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    // 新規ユーザー: 自動登録
                    User newUser = User.create(UUID.randomUUID(), googleId, email, displayName);
                    return userRepository.save(newUser);
                });
    }
}
```

### Alternatives Considered
- **マルチテーブル設計（Users専用テーブル）**:
  - 却下理由: テーブル数増加によるコスト増、クエリ結合不可（DynamoDBはJOIN非対応）
- **RDS（PostgreSQL/MySQL）**:
  - 却下理由: 予算制約（EC2に加えてRDSインスタンス費用）、DynamoDBの自動スケーリングが不要になる

---

## 5. 個人情報マスキングのロギング実装

### Decision
Logbackカスタムアペンダーまたはマスキング関数を使用し、email/displayNameをログ出力前にマスキングする。CloudWatch Logsには構造化ログ（JSON形式）でuserIdのみ記録する。

### Rationale
- 憲章原則VI「個人情報保護とプライバシー」に準拠
- GDPR/個人情報保護法のコンプライアンス要件を満たす
- ログが外部に漏洩した場合でも個人特定不可

### 実装例

```java
// LoggingUtil.java
public class LoggingUtil {
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[MASKED_EMAIL]";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        
        String maskedLocal = localPart.substring(0, 1) + "***";
        String maskedDomain = domainPart.substring(0, 1) + "***." + domainPart.substring(domainPart.lastIndexOf(".") + 1);
        
        return maskedLocal + "@" + maskedDomain;
    }
    
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "[MASKED_NAME]";
        }
        return name.substring(0, 1) + "***";
    }
}
```

```java
// OAuth2AuthenticationSuccessHandler.java（修正版）
@Override
public void onAuthenticationSuccess(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   Authentication authentication) throws IOException {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");
    
    User user = userService.findOrCreateUser(email, oAuth2User);
    
    // ❌NG: 個人情報をログ出力
    // log.info("User logged in: email={}, name={}", user.getEmail(), user.getDisplayName());
    
    // ✅OK: userIdのみ記録
    log.info("User logged in: userId={}", user.getUserId());
    
    // ✅OK: マスキング関数を使用（デバッグ時のみ）
    log.debug("User logged in: email={}, userId={}", LoggingUtil.maskEmail(user.getEmail()), user.getUserId());
    
    // JWT生成・Cookie設定（以下同じ）
    // ...
}
```

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- JSON形式で出力 -->
            <includeMdc>false</includeMdc>
            <includeContext>false</includeContext>
        </encoder>
    </appender>
    
    <appender name="CLOUDWATCH" class="ca.pjer.logback.AwsLogsAppender">
        <logGroupName>/aws/ec2/swiss-stage-web</logGroupName>
        <logStreamName>backend-app</logStreamName>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="CLOUDWATCH" />
    </root>
</configuration>
```

### Alternatives Considered
- **ログ出力後にCloudWatch Logsでフィルタリング**:
  - 却下理由: アプリケーションログに個人情報が含まれる時点でリスク、コンテナログに残る
- **個人情報を完全にログ出力しない**:
  - 却下理由: デバッグ時にマスキングされたemailすら不要とするとトラブルシューティング困難

---

## 6. エラーハンドリング

### Decision
`OAuth2AuthenticationFailureHandler`で認証失敗を捕捉し、ユーザーフレンドリーなエラーメッセージをフロントエンドにクエリパラメータで返す。

### 実装例

```java
// OAuth2AuthenticationFailureHandler.java
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {
        String errorMessage = "認証に失敗しました。";
        
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
            if ("access_denied".equals(error.getErrorCode())) {
                errorMessage = "認証がキャンセルされました";
            } else {
                errorMessage = "認証エラーが発生しました。管理者にお問い合わせください";
            }
        }
        
        log.error("OAuth2 authentication failed: errorCode={}", exception.getMessage());
        
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login")
                .queryParam("error", errorMessage)
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

### Alternatives Considered
- **例外をそのままスタックトレースで返す**:
  - 却下理由: セキュリティリスク（内部実装の露出）、ユーザー体験低下

---

## 7. シークレット管理（client-id/client-secret）

### Decision
環境ごとに異なるOAuth2クライアントIDを使用し、ローカル開発では`.env`ファイル、本番環境ではAWS Systems Manager Parameter Storeで管理する。

### Rationale
- **環境分離**: 開発環境と本番環境でリダイレクトURIが異なるため、別々のOAuth2クライアントが必要
- **セキュリティ**: client-secretをGitリポジトリに含めない
- **コスト**: AWS Systems Manager Parameter Store（標準パラメータ）は完全無料
- **IAM統合**: 細かいアクセス制御とCloudWatch Logsによる監査が可能

### 環境ごとの設定

#### ローカル開発環境

```bash
# backend/.env（Gitには含めない）
GOOGLE_CLIENT_ID=dev-client-id-xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=dev-client-secret-xxx
JWT_SECRET_KEY=your-generated-secret-key
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_REGION=ap-northeast-1
```

```gitignore
# backend/.gitignore
.env
.env.local
.env.*.local
```

```bash
# backend/.env.example（リポジトリにコミット可能）
GOOGLE_CLIENT_ID=your-dev-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-dev-client-secret
JWT_SECRET_KEY=run-openssl-rand-base64-32
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_REGION=ap-northeast-1
```

**チーム内共有**: `.env.example`をコピーして各開発者が自分の`.env`を作成

#### 本番環境（AWS Systems Manager Parameter Store）

```bash
# パラメータ保存（SecureString = 暗号化）
aws ssm put-parameter \
  --name /swiss-stage-web/prod/google-client-id \
  --value "prod-client-id-xxx.apps.googleusercontent.com" \
  --type String \
  --region ap-northeast-1

aws ssm put-parameter \
  --name /swiss-stage-web/prod/google-client-secret \
  --value "prod-client-secret-xxx" \
  --type SecureString \
  --region ap-northeast-1

aws ssm put-parameter \
  --name /swiss-stage-web/prod/jwt-secret-key \
  --value "$(openssl rand -base64 32)" \
  --type SecureString \
  --region ap-northeast-1
```

**Spring Bootでの読み込み**:

```yaml
# application-prod.yml
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

jwt:
  secret: ${jwt-secret-key}
```

**依存関係追加**:

```gradle
// build.gradle
implementation 'io.awspring.cloud:spring-cloud-aws-starter-parameter-store:3.0.0'
```

**IAM Role設定（EC2インスタンス用）**:

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
      "Resource": "arn:aws:ssm:ap-northeast-1:*:parameter/swiss-stage-web/prod/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": "arn:aws:kms:ap-northeast-1:*:key/*"
    }
  ]
}
```

### Google Cloud Consoleでの設定

```
開発環境用OAuth 2.0クライアントID
- プロジェクト: swiss-stage-web
- アプリケーションの種類: Webアプリケーション
- 名前: swiss-stage-web-dev
- 承認済みのリダイレクトURI:
  - http://localhost:8080/login/oauth2/code/google

本番環境用OAuth 2.0クライアントID
- プロジェクト: swiss-stage-web
- アプリケーションの種類: Webアプリケーション
- 名前: swiss-stage-web-prod
- 承認済みのリダイレクトURI:
  - https://your-domain.com/login/oauth2/code/google
```

### CI/CD（GitHub Secrets）

GitHub Actionsでのデプロイ時に使用:

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
      - name: Deploy to EC2
        env:
          GOOGLE_CLIENT_ID: ${{ secrets.PROD_GOOGLE_CLIENT_ID }}
          GOOGLE_CLIENT_SECRET: ${{ secrets.PROD_GOOGLE_CLIENT_SECRET }}
          JWT_SECRET_KEY: ${{ secrets.PROD_JWT_SECRET_KEY }}
        run: |
          # デプロイスクリプト実行
          ssh ec2-user@your-ec2-instance 'bash deploy.sh'
```

**GitHub Secretsの設定場所**: Repository > Settings > Secrets and variables > Actions

### 推奨構成まとめ

| 環境 | 管理方法 | コスト | 利点 |
|------|----------|--------|------|
| ローカル開発 | `.env`ファイル（.gitignore） | 無料 | シンプル、即座に反映 |
| 本番環境 | AWS Systems Manager Parameter Store | 無料 | IAM統合、暗号化、監査ログ |
| CI/CD | GitHub Secrets | 無料 | GitHub Actions統合 |

### Alternatives Considered

- **AWS Secrets Manager**: 
  - 利点: 自動ローテーション、より高度なシークレット管理
  - 却下理由: コスト（$0.40/シークレット/月）、OAuth2のclient-secretは手動ローテーションで十分
- **環境変数のみ（EC2インスタンスで直接設定）**:
  - 却下理由: 環境変数の変更時にアプリケーション再起動が必要、監査ログなし、IAM統合不可
- **.propertiesファイルをS3に保存**:
  - 却下理由: S3のバージョニング管理が複雑、Parameter Storeの方がシンプル

---

## 8. アカウント削除実装

### Decision
削除前に進行中トーナメント存在チェックを実行し、存在する場合はエラーを返す。削除時はDynamoDBからユーザー情報と関連トーナメントデータを物理削除する。

### 実装例

```java
// UserService.java
public void deleteAccount(UUID userId) {
    // 進行中トーナメント存在チェック
    List<Tournament> activeTournaments = tournamentRepository.findActiveByUserId(userId);
    if (!activeTournaments.isEmpty()) {
        throw new BusinessException("進行中のトーナメントがあるため削除できません。トーナメントを完了または削除してから再度お試しください");
    }
    
    // ユーザー削除
    userRepository.deleteById(userId);
    
    // 関連トーナメントデータ削除（カスケード）
    tournamentRepository.deleteAllByUserId(userId);
    
    // 削除ログ記録（監査用）
    log.info("Account deleted: userId={}", userId);
}
```

### Alternatives Considered
- **論理削除（deleted_atフラグ）**:
  - 却下理由: GDPR「忘れられる権利」に非準拠、データ保持がプライバシー侵害

---

## まとめ

本調査により、以下の技術的決定を行いました:

1. **Spring Security OAuth2 Client**: 宣言的設定で標準準拠の実装
2. **JWT (HS256)**: 高速な署名検証、24時間有効期限
3. **HTTP-only Cookie**: XSS攻撃対策、自動送信
4. **DynamoDB単一テーブル**: コスト最適化、findOrCreateパターン
5. **個人情報マスキング**: Logback + 構造化ログ、userIdのみ記録
6. **エラーハンドリング**: ユーザーフレンドリーなメッセージ
7. **シークレット管理**: 環境ごとに分離、AWS Systems Manager Parameter Store（無料）
8. **物理削除**: GDPR準拠、進行中トーナメントチェック

すべての決定は憲章原則（DDD, TDD, スケーラビリティ, 個人情報保護）に準拠しています。
