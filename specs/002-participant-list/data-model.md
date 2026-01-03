# データモデル設計 - 参加者一覧管理

**機能**: 参加者一覧管理（グループ別CSV入出力対応）
**日付**: 2026-01-03
**ステータス**: Phase 1設計

このドキュメントは、research.mdの調査結果を基に、DDDの集約とエンティティの詳細設計を定義します。

---

## 1. ドメインエンティティ

### 1.1 Group（グループ）

**役割**: トーナメント内の参加者を分割する論理的なグループ（最大8グループ）

**属性**:

| 属性名 | 型 | 必須 | 説明 | 制約 |
|--------|------|------|------|------|
| `groupId` | UUID | ✓ | グループの一意識別子 | - |
| `tournamentId` | UUID | ✓ | 所属トーナメントのID | 外部キー |
| `groupNumber` | Integer | ✓ | グループ番号 | 1〜8 |
| `displayName` | String | ✓ | 表示名 | 例: "GROUP 1" |

**システム定数**:
```java
public static final int MAX_PARTICIPANTS = 32; // 1グループあたりの上限
public static final int MAX_GROUPS = 8; // トーナメントあたりの上限
```

**DynamoDBマッピング**:
- **テーブル名**: `Group`
- **PK**: `tournamentId` (String)
- **SK**: `groupNumber` (Number)
- **属性**: `groupId`, `displayName`

**不変条件**:
- `groupNumber`は1〜8の範囲内
- 同一`tournamentId`内で`groupNumber`は一意
- `displayName`は"GROUP {groupNumber}"の形式

---

### 1.2 Participant（参加者）

**役割**: トーナメント参加者の基本情報と段級位を管理

**属性**:

| 属性名 | 型 | 必須 | 説明 | 制約 |
|--------|------|------|------|------|
| `participantId` | UUID | ✓ | 参加者の一意識別子 | - |
| `groupId` | UUID | ✓ | 所属グループのID | 外部キー |
| `affiliation` | String | - | 所属（会社、道場、地域など） | 最大100文字 |
| `name` | String | ✓ | 氏名 | 最大50文字 |
| `rank` | Rank | ✓ | 段級位 | Value Object（下記参照） |
| `isDummy` | Boolean | ✓ | ダミーユーザーフラグ | デフォルト: false |
| `registrationOrder` | Integer | ✓ | グループ内の登録順 | 1〜32 |

**DynamoDBマッピング**:
- **テーブル名**: `Participant`
- **PK**: `groupId` (String)
- **SK**: `participantId` (String)
- **属性**: `affiliation`, `name`, `rankLevel` (Number), `rankDisplayName` (String), `isDummy` (Boolean), `registrationOrder` (Number)
- **GSI**: `groupId-rankLevel-index`
  - PK: `groupId`
  - SK: `rankLevel#registrationOrder`（複合ソートキー）

**不変条件**:
- `name`は1文字以上
- `registrationOrder`は同一グループ内で一意
- `isDummy=true`の場合、`rank`はnull

---

### 1.3 Rank（段級位）- Value Object

**役割**: 段級位の形式検証と比較を提供する値オブジェクト

**属性**:

| 属性名 | 型 | 説明 |
|--------|------|------|
| `level` | Integer | 段級位のレベル値（段は正数、級は負数） |
| `displayName` | String | 表示名（例: "初段", "3級"） |

**有効なフォーマット**:
- **段**: `初段`, `2段`, `3段`, ..., `9段`（初段〜9段）
- **級**: `1級`, `2級`, `3級`, ..., `20級`（1級〜20級）

**level値の変換ルール**:
```java
初段 → level = 1
3段 → level = 3
9段 → level = 9
1級 → level = -1
3級 → level = -3
20級 → level = -20
```

**ソート順**: `level`降順（9段 > 5段 > 3段 > 初段 > 1級 > 3級 > 20級）

**実装例**:
```java
public record Rank(int level, String displayName) implements Comparable<Rank> {
    private static final Pattern RANK_PATTERN = 
        Pattern.compile("^(初段|[2-9]段|[1-9]級|1[0-9]級|20級)$");
    
    public static Rank parse(String input) {
        if (!RANK_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("段級位の形式が不正です: " + input);
        }
        
        if (input.equals("初段")) return new Rank(1, "初段");
        
        int number = Integer.parseInt(input.substring(0, input.length() - 1));
        return input.endsWith("段") 
            ? new Rank(number, input) 
            : new Rank(-number, input);
    }
    
    @Override
    public int compareTo(Rank other) {
        return Integer.compare(other.level, this.level); // 降順
    }
}
```

---

## 2. 集約ルート（Aggregate Root）

### 2.1 GroupParticipantList

**役割**: グループに所属する参加者のライフサイクルを管理する集約ルート

**責務**:
- 参加者の追加・削除（32名上限チェック）
- ダミーユーザーの自動管理（奇数→ダミー追加、偶数→削除）
- 参加者リストの段級位順・登録順ソート
- CSV入出力のデータ整合性保証

**集約境界**:
- `Group`エンティティ（1個）
- `Participant`エンティティ（最大32個）

**ドメインメソッド**:

```java
public class GroupParticipantList {
    private final Group group;
    private final List<Participant> participants;
    
    // 参加者追加（上限チェック + ダミーユーザー管理）
    public void addParticipant(Participant participant) {
        validateCapacity(); // 32名チェック
        participants.add(participant);
        ensureEvenCount(); // ダミーユーザー調整
    }
    
    // 参加者削除（ダミーユーザー管理）
    public void removeParticipant(UUID participantId) {
        participants.removeIf(p -> p.participantId().equals(participantId));
        ensureEvenCount();
    }
    
    // CSV一括登録（トランザクション境界）
    public void importFromCsv(List<CsvRow> rows) {
        validateCsvData(rows); // 形式検証
        validateCapacity(rows.size()); // 上限チェック
        
        List<Participant> newParticipants = rows.stream()
            .map(this::csvRowToParticipant)
            .toList();
        
        participants.clear();
        participants.addAll(newParticipants);
        ensureEvenCount();
    }
    
    // CSV出力（ダミーユーザー除外）
    public List<Participant> exportToCsv() {
        return participants.stream()
            .filter(p -> !p.isDummy())
            .sorted(Comparator.comparing(Participant::registrationOrder))
            .toList();
    }
    
    // ダミーユーザー管理
    private void ensureEvenCount() {
        long realCount = participants.stream().filter(p -> !p.isDummy()).count();
        removeExistingDummy();
        
        if (realCount % 2 == 1) {
            Participant dummy = createDummyParticipant();
            participants.add(dummy);
        }
    }
    
    private void validateCapacity() {
        long realCount = participants.stream().filter(p -> !p.isDummy()).count();
        if (realCount >= Group.MAX_PARTICIPANTS) {
            throw new DomainException("参加者数が上限（32名）に達しています");
        }
    }
    
    // 段級位順 → 登録順でソート
    public List<Participant> getSortedParticipants() {
        return participants.stream()
            .sorted(Comparator
                .comparing(Participant::rank)
                .thenComparing(Participant::registrationOrder))
            .toList();
    }
}
```

**不変条件（集約内で保証）**:
- 実際の参加者数は0〜32名
- 参加者が奇数の場合、必ずダミーユーザーが1名存在
- ダミーユーザーは最大1名
- `registrationOrder`は1から連番

---

## 3. データモデル関係図

```
Tournament (既存)
    |
    | 1:N
    |
  Group (新規)
    |
    | groupId
    |
    +--------+
    |        |
    v        v
Participant  GroupParticipantList (集約ルート)
 (N個)       |
    |        +-- Group (1個)
    |        +-- Participant (0〜32個)
    |
    +-- Rank (Value Object)
```

**集約間の関係**:
- `Tournament` → `Group`: 1対N（トーナメントは最大8グループ）
- `Group` → `Participant`: 1対N（グループは最大32参加者）
- `GroupParticipantList`が集約ルートとして整合性を保証

---

## 4. リポジトリインターフェース

### 4.1 GroupRepository

```java
public interface GroupRepository {
    Group findById(UUID groupId);
    List<Group> findByTournamentId(UUID tournamentId);
    void save(Group group);
    void delete(UUID groupId);
}
```

### 4.2 GroupParticipantListRepository

```java
public interface GroupParticipantListRepository {
    GroupParticipantList findByGroupId(UUID groupId);
    void save(GroupParticipantList aggregateRoot);
    
    // DynamoDB最適化クエリ
    List<Participant> findParticipantsSortedByRank(UUID groupId);
}
```

**実装ポイント**:
- `findParticipantsSortedByRank()`は`groupId-rankLevel-index` GSIを使用
- `save()`は集約全体をトランザクション境界として永続化

---

## 5. エンティティライフサイクル

### 5.1 参加者手動登録フロー

```
1. ユーザーが参加者情報入力（name, affiliation, rank）
2. Application層がGroupParticipantListを取得
3. GroupParticipantList.addParticipant()呼び出し
   → 32名チェック
   → registrationOrder自動採番（max + 1）
   → ダミーユーザー調整
4. リポジトリ経由で永続化
```

### 5.2 CSV一括登録フロー

```
1. ユーザーがCSVファイルアップロード
2. Application層がCSVパース（エンコーディング自動検出）
3. GroupParticipantList.importFromCsv()呼び出し
   → 各行の形式検証（name, affiliation, rank）
   → 32名上限チェック
   → 既存参加者を削除して新規リスト登録
   → ダミーユーザー調整
4. リポジトリ経由で永続化
```

### 5.3 ダミーユーザー管理フロー

```
参加者追加/削除 → ensureEvenCount()
  ├→ 実際の参加者数カウント
  ├→ 既存ダミーユーザー削除
  └→ 奇数の場合のみダミーユーザー追加
```

---

## 6. 制約とバリデーション

### 6.1 ドメイン層でのバリデーション

| 制約 | 検証場所 | エラーメッセージ |
|------|----------|------------------|
| 参加者上限（32名） | `GroupParticipantList.validateCapacity()` | "参加者数が上限（32名）に達しています" |
| 段級位形式 | `Rank.parse()` | "段級位の形式が不正です: {input}" |
| 氏名必須 | `Participant`コンストラクタ | "氏名は必須です" |
| 氏名長さ | `Participant`コンストラクタ | "氏名は50文字以内で入力してください" |
| 所属長さ | `Participant`コンストラクタ | "所属は100文字以内で入力してください" |
| グループ番号範囲 | `Group`コンストラクタ | "グループ番号は1〜8の範囲内です" |

### 6.2 アプリケーション層でのバリデーション

| 制約 | 検証場所 | エラーメッセージ |
|------|----------|------------------|
| CSV形式 | `CsvImportService` | "CSVファイルの形式が不正です" |
| CSV列名 | `CsvImportService` | "必須列が不足しています: name, rank" |
| CSV重複チェック | `CsvImportService` | "同一グループ内に重複した氏名があります" |
| CSVエンコーディング | `CsvImportService` | "CSVファイルの文字エンコーディングが不正です" |

---

## 7. パフォーマンス最適化

### 7.1 DynamoDBクエリ最適化

**課題**: 参加者一覧を段級位順→登録順でソートして取得

**解決策**: GSI `groupId-rankLevel-index`を活用
```java
// DynamoDB Query
QueryRequest request = QueryRequest.builder()
    .tableName("Participant")
    .indexName("groupId-rankLevel-index")
    .keyConditionExpression("groupId = :groupId")
    .expressionAttributeValues(Map.of(":groupId", groupIdValue))
    .scanIndexForward(false) // 降順（level大→小）
    .build();
```

**効果**: アプリケーション層でのソート処理不要、レスポンス時間短縮

### 7.2 キャッシング戦略

- フロントエンドで`React Query`を使用
- グループデータを30秒キャッシュ
- タブ切り替え時の再フェッチを抑制

---

## Phase 1 完了チェックリスト

- [x] Groupエンティティ設計完了
- [x] Participantエンティティ設計完了
- [x] Rank値オブジェクト設計完了
- [x] GroupParticipantList集約ルート設計完了
- [x] リポジトリインターフェース定義完了
- [x] ライフサイクル・バリデーション設計完了
- [x] DynamoDBマッピング設計完了

**次のステップ**: API契約設計（contracts/）
