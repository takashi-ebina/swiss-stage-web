# 実装計画: 参加者一覧管理（グループ別CSV入出力対応）

**Branch**: `002-participant-list` | **日付**: 2026-01-03 | **仕様**: [spec.md](spec.md)
**入力**: `/specs/002-participant-list/spec.md` からの機能仕様

## 概要

スイス方式トーナメントの参加者管理機能を実装する。最大8グループ（各32名上限、合計256名対応）で参加者を管理し、グループ単位でのCSV入出力、段級位順自動ソート、奇数時のダミーユーザー自動追加機能を提供する。

**主要機能**:
- グループタブ（GROUP 1～GROUP 8）での参加者管理
- 参加者の手動登録（所属・名前・段級位）
- グループ単位でのCSV一括取込・出力（UTF-8/Shift-JIS対応）
- 段級位順の自動ソートとダミーユーザー管理
- 参加者の個別編集・削除（P2）

**技術的アプローチ**:
- バックエンド: DDD構造でドメインロジック（ソート、ダミーユーザー管理）を`domain`層に集約
- フロントエンド: Material-UIのタブコンポーネントでグループ切り替え、CSV入出力にはFile APIを使用
- データストア: DynamoDB（GroupテーブルとParticipantテーブル）
- バリデーション: 段級位フォーマット検証、32名上限チェック、必須項目チェック

## 技術的コンテキスト

**Language/Version**: 
- バックエンド: Java 21
- フロントエンド: TypeScript

**Primary Dependencies**: 
- バックエンド: Spring Boot, AWS SDK for DynamoDB
- フロントエンド: React, Material-UI, Vite

**Storage**: DynamoDB（Group、Participant テーブル）

**Testing**: 
- バックエンド: JUnit 5, Mockito
- フロントエンド: Jest, React Testing Library
- E2E: Playwright

**Target Platform**: AWS（EC2、DynamoDB）

**Project Type**: Web（フロントエンド + バックエンド）

**Performance Goals**: 
- グループ切り替え応答時間: 1秒以内
- 参加者登録後の対戦表反映: 3秒以内（全8グループ、最大256名）
- CSV一括登録（32名）: 1分以内

**Constraints**: 
- 1グループあたり32名上限
- 全8グループ、合計256名対応
- CSV文字エンコーディング: UTF-8/Shift-JIS両対応

**Scale/Scope**: 
- ユーザー規模: 大会主催者（少数）+ 参加者（最大256名/大会）
- 画面数: 参加者一覧画面（グループタブ付き）
- API数: 参加者CRUD、CSV入出力、グループ管理

## 憲章チェック

*GATE: フェーズ0の調査前に合格する必要があります。フェーズ1の設計後に再確認してください。*

### 原則I: ドメイン駆動設計 (DDD)
- [x] バックエンドはDDDレイヤー構造（application/domain/infrastructure/presentation）に従っているか
  - `domain/model`: Group、Participant、Rank、GroupParticipantList
  - `application/service`: ParticipantApplicationService（ユースケース実装）
  - `infrastructure/repository`: DynamoDBを使用したリポジトリ実装
  - `presentation/controller`: REST APIエンドポイント
- [x] ドメインロジックがdomain層に集約されているか
  - 段級位順ソート、ダミーユーザー追加、32名上限バリデーション
- [x] infrastructure層への依存が逆転していないか
  - リポジトリインターフェースをdomain層に定義、実装をinfrastructure層に配置

### 原則II: テスト駆動開発 (TDD)【非交渉】
- [x] 受け入れテストシナリオが仕様書に明記されているか
  - 4つのユーザーストーリーに対し、各5-8シナリオ定義済み
- [x] テストコード作成 → 実装の順序が守られているか
  - Phase 2の実装タスクで明示
- [x] テストカバレッジ目標（domain層90%以上、application層80%以上）を満たせるか
  - domain層: Group、Participant、Rank、GroupParticipantListの単体テスト
  - application層: ParticipantApplicationServiceの統合テスト
  - E2E: Playwrightでの主要フロー検証

### 原則III: AIペアプログラミング
- [x] AIに実施させる範囲（日本語記述、コード生成、設計提案、UI記述）が明確か
  - 日本語での実装計画・タスク記述
  - DDDエンティティ・値オブジェクトのコード生成
  - Material-UIコンポーネントの実装
- [x] 技術スタック変更の独断決定を防ぐ仕組みがあるか
  - 憲章で技術スタック（Java 21、Spring Boot、React、Material-UI、DynamoDB）を固定

### 原則IV: 段階的機能実装
- [x] 機能に優先度（P1/P2/P3）がついているか
  - P1: 手動登録、CSV取込（ユーザーストーリー1, 2）
  - P2: CSV出力、個別編集・削除（ユーザーストーリー3, 4）
- [x] MVP機能（P1）が明確に定義されているか
  - グループ別参加者登録、CSV一括取込、段級位順ソート、ダミーユーザー自動追加

### 原則V: スケーラビリティと可観測性
- [x] パフォーマンス目標（300名対応、5秒以内マッチング）を満たせるか
  - 256名対応（8グループ×32名）、参加者登録後3秒以内で対戦表反映
- [x] 構造化ログ（JSON）をCloudWatch Logsに出力する設計か
  - Logback設定でJSON形式ログ出力、個人情報マスキング適用
- [x] 主要APIエンドポイントのレスポンスタイム監視が含まれているか
  - 参加者登録API、CSV取込API、グループ取得APIのレスポンスタイム記録

### 原則VI: コード品質とシンプリシティ
- [x] 不変オブジェクトにJava標準Recordを使用しているか
  - Rank（値オブジェクト）、各種DTO、ErrorResponseにRecordを適用
- [x] Lombokの使用を最小限にしているか
  - Participantエンティティは標準Javaで実装、Recordで対応可能なものはRecordを使用
- [x] 明示的なコードを優先しているか
  - コンストラクタ、Getter、Setterを明示的に記述

### 原則VII: 個人情報保護とプライバシー【非交渉】
- [x] 個人識別情報（PII）をログに出力しないか
  - ログにはuserIdのみ記録、name、affiliation、emailはマスキング
- [x] ログ出力時のマスキングルールが適用されているか
  - `[MASKED_NAME]`、`[MASKED_EMAIL]`、`[MASKED_AFFILIATION]`形式でマスキング

**結論**: すべての憲章チェックに合格。Phase 0（調査）に進む準備が整っています。

## プロジェクト構造

### ドキュメント (この機能)

```text
specs/002-participant-list/
├── plan.md              # この実装計画
├── research.md          # Phase 0 調査結果（次のステップで作成）
├── data-model.md        # Phase 1 データモデル設計
├── quickstart.md        # Phase 1 クイックスタートガイド
├── contracts/           # Phase 1 API契約
│   ├── participant-api.yaml  # 参加者管理API仕様
│   └── group-api.yaml        # グループ管理API仕様
└── tasks.md             # Phase 2 実装タスク（/speckit.tasks command）
```

### ソースコード (リポジトリのルート)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── swiss_stage/
│   │   │           ├── application/
│   │   │           │   └── service/
│   │   │           │       └── ParticipantApplicationService.java  # 参加者管理ユースケース
│   │   │           ├── domain/
│   │   │           │   ├── model/
│   │   │           │       ├── Group.java                      # グループエンティティ
│   │   │           │       ├── Participant.java                # 参加者エンティティ
│   │   │           │       ├── Rank.java                       # 段級位値オブジェクト（Record）
│   │   │           │       └── GroupParticipantList.java       # 集約ルート
│   │   │           │   └── repository/
│   │   │           │       ├── GroupRepository.java            # Groupリポジトリインターフェース
│   │   │           │       └── ParticipantRepository.java      # Participantリポジトリインターフェース
│   │   │           ├── infrastructure/
│   │   │           │   ├── repository/
│   │   │           │       ├── DynamoDBGroupRepository.java    # Group DynamoDB実装
│   │   │           │       └── DynamoDBParticipantRepository.java # Participant DynamoDB実装
│   │   │           │   └── csv/
│   │   │           │       ├── CsvParserService.java           # CSV解析サービス
│   │   │           │       └── CsvExportService.java           # CSV出力サービス
│   │   │           └── presentation/
│   │   │               └── controller/
│   │   │                   ├── ParticipantController.java      # 参加者管理API
│   │   │                   └── GroupController.java            # グループ管理API
│   │   └── resources/
│   │       └── application.yml                                 # Spring Boot設定
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── swiss_stage/
│       │           ├── unit/
│       │           │   ├── domain/
│       │           │   │   ├── GroupTest.java                  # Groupエンティティ単体テスト
│       │           │   │   ├── ParticipantTest.java            # Participant単体テスト
│       │           │   │   ├── RankTest.java                   # Rank単体テスト
│       │           │   │   └── GroupParticipantListTest.java   # 集約ルート単体テスト
│       │           │   └── application/
│       │           │       └── ParticipantApplicationServiceTest.java # ユースケーステスト
│       │           └── integration/
│       │               ├── infrastructure/
│       │               │   └── DynamoDBRepositoryIntegrationTest.java # リポジトリ統合テスト
│       │               └── presentation/
│       │                   └── ParticipantControllerIntegrationTest.java # APIエンドポイント統合テスト
│       └── resources/
│           └── application-test.yml                            # テスト用設定

frontend/
├── src/
│   ├── components/
│   │   ├── participant/
│   │   │   ├── ParticipantListTabs.tsx         # グループタブコンポーネント
│   │   │   ├── ParticipantTable.tsx            # 参加者一覧テーブル
│   │   │   ├── ParticipantForm.tsx             # 参加者登録フォーム
│   │   │   ├── CsvImportButton.tsx             # CSV取込ボタン
│   │   │   ├── CsvExportButton.tsx             # CSV出力ボタン
│   │   │   └── ParticipantEditDialog.tsx       # 参加者編集ダイアログ（P2）
│   │   └── common/
│   │       └── ErrorMessage.tsx                # エラーメッセージ表示
│   ├── pages/
│   │   └── ParticipantListPage.tsx             # 参加者一覧画面
│   ├── services/
│   │   ├── participantService.ts               # 参加者API通信
│   │   ├── groupService.ts                     # グループAPI通信
│   │   └── csvService.ts                       # CSVパース・生成
│   ├── types/
│   │   ├── Participant.ts                      # 参加者型定義
│   │   ├── Group.ts                            # グループ型定義
│   │   └── Rank.ts                             # 段級位型定義
│   └── utils/
│       ├── rankParser.ts                       # 段級位パース処理
│       └── csvEncoder.ts                       # CSV文字エンコーディング検出
└── tests/
    ├── unit/
    │   ├── components/
    │   │   └── participant/
    │   │       ├── ParticipantListTabs.test.tsx    # タブコンポーネント単体テスト
    │   │       └── ParticipantForm.test.tsx        # フォーム単体テスト
    │   └── services/
    │       └── csvService.test.ts                  # CSVサービス単体テスト
    └── e2e/
        ├── participant-registration.spec.ts        # 参加者登録E2Eテスト
        ├── csv-import.spec.ts                      # CSV取込E2Eテスト
        └── csv-export.spec.ts                      # CSV出力E2Eテスト（P2）

specs/002-participant-list/
└── contracts/
    ├── participant-api.yaml                        # OpenAPI仕様
    └── group-api.yaml                              # OpenAPI仕様
```

**構造の決定**: Webアプリケーション構造（フロントエンド + バックエンド）を採用。バックエンドはDDDレイヤー構造（application/domain/infrastructure/presentation）、フロントエンドはReact + Material-UIのコンポーネントベース構造を使用。

## 複雑さの追跡

**該当なし**: 憲法の原則に違反する複雑さは導入されていません。

すべての実装は憲章の原則に従っています：
- DDDレイヤー構造の遵守
- TDDプロセスの適用
- Java標準Record使用によるシンプリシティ
- 個人情報保護の徹底

---

## Phase 0: 調査とアーキテクチャ決定

### 調査タスク

#### 1. DynamoDB テーブル設計

**調査内容**:
- GroupテーブルとParticipantテーブルのキー設計
- グループ内参加者の効率的な取得方法（GSI設計）
- 段級位順ソートのためのインデックス設計
- 32名上限チェックの実装方法

**決定事項**（research.mdに記録）:
- Groupテーブル: PK=`tournamentId`, SK=`groupNumber`
- Participantテーブル: PK=`groupId`, SK=`participantId`
- GSI: `groupId-registrationOrder-index`（段級位同一時のソート用）
- 上限チェック: アプリケーション層でカウント実行

#### 2. CSV文字エンコーディング検出

**調査内容**:
- UTF-8/Shift-JISの自動判定方法
- Javaでのエンコーディング検出ライブラリ（juniversalchardet等）
- フロントエンドでのCSVパース（PapaParse等）

**決定事項**（research.mdに記録）:
- バックエンド: juniversalchardet使用
- フロントエンド: PapaParse + encoding-japanese使用
- エラー時はUTF-8をデフォルトとして再試行

#### 3. 段級位フォーマット検証

**調査内容**:
- 有効な段級位フォーマット（例: "3段", "初段", "2級"）
- 正規表現パターン設計
- level値への変換ロジック

**決定事項**（research.mdに記録）:
- 正規表現: `^(初段|[2-9]段|[1-9][0-9]段|初級|[2-9]級|[1-9][0-9]級)$`
- level変換: 段=正の整数、級=負の整数
- Rank値オブジェクトにパース処理を実装

#### 4. Material-UI タブコンポーネント設計

**調査内容**:
- Tabsコンポーネントのベストプラクティス
- 8タブの表示最適化（スクロール、レスポンシブ対応）
- タブ切り替え時のパフォーマンス最適化

**決定事項**（research.mdに記録）:
- `<Tabs variant="scrollable">`を使用
- タブコンテンツはReact.memo()で最適化
- グループデータはReact Queryでキャッシュ

---

## Phase 1: 設計とAPI契約

### データモデル設計（data-model.md）

#### エンティティ

**Group**:
```java
public class Group {
    private final UUID groupId;
    private final UUID tournamentId;
    private final Integer groupNumber;  // 1-8
    private final String displayName;   // "GROUP 1"
    
    // ビジネスルール: 32名上限はシステム定数
    public static final int MAX_PARTICIPANTS = 32;
}
```

**Participant**:
```java
public class Participant {
    private final UUID participantId;
    private final UUID groupId;
    private final String affiliation;  // nullable
    private final String name;
    private final Rank rank;
    private final boolean isDummy;
    private final Integer registrationOrder;
}
```

**Rank（Record）**:
```java
public record Rank(int level, String displayName) {
    public static Rank parse(String input) { /* ... */ }
    public int compareTo(Rank other) { return Integer.compare(this.level, other.level); }
}
```

**GroupParticipantList（集約ルート）**:
```java
public class GroupParticipantList {
    private final List<Participant> participants;
    
    public void addParticipant(Participant participant) { /* ソート実行 */ }
    public void ensureEvenCount(UUID groupId) { /* ダミーユーザー追加 */ }
    public List<Participant> getSortedParticipants() { /* 段級位順 */ }
    public boolean canAddParticipants(int count) { /* 32名上限チェック */ }
}
```

### API契約設計（contracts/）

#### participant-api.yaml（OpenAPI仕様）

**主要エンドポイント**:
- `POST /api/groups/{groupId}/participants` - 参加者登録
- `GET /api/groups/{groupId}/participants` - 参加者一覧取得
- `PUT /api/participants/{participantId}` - 参加者更新（P2）
- `DELETE /api/participants/{participantId}` - 参加者削除（P2）
- `POST /api/groups/{groupId}/participants/csv-import` - CSV一括取込
- `GET /api/groups/{groupId}/participants/csv-export` - CSV出力

#### group-api.yaml（OpenAPI仕様）

**主要エンドポイント**:
- `GET /api/tournaments/{tournamentId}/groups` - グループ一覧取得
- `GET /api/groups/{groupId}` - グループ詳細取得

### クイックスタートガイド（quickstart.md）

開発者向けセットアップ手順：
1. DynamoDBローカル環境構築
2. バックエンドビルド・実行
3. フロントエンド開発サーバー起動
4. E2Eテスト実行

### エージェントコンテキスト更新

```bash
.specify/scripts/bash/update-agent-context.sh copilot
```

新規技術要素を追加:
- DynamoDB（Group、Participantテーブル）
- CSV処理（juniversalchardet、PapaParse）
- Material-UI Tabsコンポーネント

---

## Phase 2: 実装タスク（tasks.mdで詳細化）

### P1機能（MVP必須）

**優先度順**:

1. **ドメイン層実装（TDD）**
   - Rank値オブジェクト（Record）+ 単体テスト
   - Participantエンティティ + 単体テスト
   - Groupエンティティ + 単体テスト
   - GroupParticipantList集約ルート + 単体テスト

2. **インフラ層実装（TDD）**
   - DynamoDB Group/Participantテーブル作成
   - DynamoDBRepositoryImpl + 統合テスト
   - CsvParserService + 単体テスト
   - CsvExportService + 単体テスト

3. **アプリケーション層実装（TDD）**
   - ParticipantApplicationService + 単体テスト

4. **プレゼンテーション層実装（TDD）**
   - ParticipantController + 統合テスト
   - GroupController + 統合テスト

5. **フロントエンド実装（TDD）**
   - 型定義（Participant、Group、Rank）
   - API通信サービス + 単体テスト
   - ParticipantListTabsコンポーネント + 単体テスト
   - ParticipantFormコンポーネント + 単体テスト
   - CsvImportButtonコンポーネント + 単体テスト
   - ParticipantListPage + E2Eテスト

6. **E2Eテスト（Playwright）**
   - 参加者登録フロー
   - CSV一括取込フロー
   - グループ切り替えフロー

### P2機能（優先度中）

7. **CSV出力機能**
   - CsvExportButton + 単体テスト
   - CSV出力E2Eテスト

8. **参加者編集・削除機能**
   - ParticipantEditDialog + 単体テスト
   - 編集・削除E2Eテスト

---

## リスクと緩和策

### リスク1: CSV文字エンコーディング検出の精度

**影響**: SC-004（99%以上の精度）を満たせない可能性

**緩和策**:
- juniversalchardetとencoding-japaneseの組み合わせで検証
- エラー時のユーザーフレンドリーなメッセージ表示
- 手動エンコーディング選択オプション（将来）

### リスク2: 256名規模でのパフォーマンス

**影響**: SC-002（3秒以内の応答時間）を満たせない可能性

**緩和策**:
- DynamoDB GSI設計の最適化
- フロントエンドでのReact Query活用（キャッシュ）
- 負荷テストでの事前検証

### リスク3: ダミーユーザーの扱い

**影響**: ダミーユーザーの自動追加・削除タイミングでのバグ

**緩和策**:
- GroupParticipantList集約ルートで一元管理
- 単体テストでエッジケース（0名、1名、奇数、偶数）を網羅
- E2Eテストでフロー全体を検証

---

## Phase完了記録

### ✅ Phase 0: 調査（完了）

**成果物**: [research.md](research.md)

**調査内容**:
1. DynamoDBテーブル設計（Group、Participant、GSI設計）
2. CSV文字エンコーディング検出方法（juniversalchardet、encoding-japanese）
3. 段級位フォーマット検証・パース処理設計
4. Material-UIタブコンポーネント設計（React Query活用）
5. ダミーユーザー管理戦略
6. パフォーマンス検証計画

**結論**: すべての技術的不明点が解決され、Phase 1に進む準備完了

---

### ✅ Phase 1: 設計（完了）

**成果物**:
- [data-model.md](data-model.md) - DDDエンティティ・集約設計
- [contracts/group-api.yaml](contracts/group-api.yaml) - Group API仕様
- [contracts/participant-api.yaml](contracts/participant-api.yaml) - Participant API仕様（CSV含む）
- [quickstart.md](quickstart.md) - 実装ガイド

**設計内容**:
1. **ドメインエンティティ**:
   - `Group`: グループID、トーナメントID、グループ番号、表示名
   - `Participant`: 参加者ID、グループID、所属、氏名、段級位、ダミーフラグ、登録順
   - `Rank`: 段級位値オブジェクト（level値、表示名）

2. **集約ルート**:
   - `GroupParticipantList`: 参加者リストの整合性管理（32名上限、ダミーユーザー自動管理）

3. **API契約**:
   - Group管理: 作成、一覧取得、詳細取得、削除
   - Participant管理: 追加、一覧取得、更新、削除
   - CSV操作: 一括登録（import）、出力（export）

4. **DynamoDBマッピング**:
   - GroupテーブルのPK/SK設計
   - ParticipantテーブルのGSI設計（段級位順ソート）

**憲章チェック（再評価）**: 全7項目パス - 設計レベルでの違反なし

**結論**: Phase 2（実装）に進む準備完了

---

## 次のステップ

1. ✅ **Phase 0完了**: research.mdで技術調査完了
2. ✅ **Phase 1完了**: data-model.md、contracts/、quickstart.md作成完了
3. **Phase 2開始**: `/speckit.tasks`でtasks.mdを生成し、TDDで実装開始
   - ドメイン層 → インフラ層 → アプリケーション層 → プレゼンテーション層 → フロントエンド → E2E
   - 各タスクでテスト先行（Red → Green → Refactor）
