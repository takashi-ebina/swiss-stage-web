# Quickstart Guide - 参加者一覧管理機能

このガイドは、参加者一覧管理機能の実装を開始するための技術的なガイドラインを提供します。

---

## 1. 概要

**機能**: グループ別の参加者一覧管理（CSV入出力対応）  
**対象ユーザー**: トーナメント主催者  
**主な機能**:
- 最大8グループの管理
- グループごとに最大32名の参加者登録
- CSV一括登録・出力（UTF-8/Shift-JIS対応）
- 段級位順→登録順の自動ソート
- ダミーユーザー自動管理（奇数時）

---

## 2. アーキテクチャ

### 2.1 技術スタック

**バックエンド**:
- Java 21
- Spring Boot 3.2+
- AWS SDK for DynamoDB
- juniversalchardet（文字エンコーディング検出）

**フロントエンド**:
- TypeScript 5.0+
- React 18
- Material-UI 5
- React Query（データフェッチ・キャッシング）
- PapaParse（CSVパース）
- encoding-japanese（文字エンコーディング変換）

**データベース**:
- DynamoDB
  - `Group`テーブル
  - `Participant`テーブル（GSI: `groupId-rankLevel-index`）

### 2.2 DDDレイヤー構成

```
backend/src/main/java/com/swiss_stage/
├── domain/
│   ├── group/
│   │   ├── Group.java                    # グループエンティティ
│   │   ├── GroupRepository.java          # リポジトリインターフェース
│   ├── participant/
│   │   ├── Participant.java              # 参加者エンティティ
│   │   ├── Rank.java                     # 段級位値オブジェクト
│   │   ├── GroupParticipantList.java     # 集約ルート
│   │   ├── GroupParticipantListRepository.java
│   ├── common/
│   │   ├── DomainException.java          # ドメイン例外
│
├── application/
│   ├── group/
│   │   ├── GroupApplicationService.java  # グループ管理サービス
│   │   ├── dto/
│   │   │   ├── CreateGroupCommand.java
│   │   │   ├── GroupDto.java
│   ├── participant/
│   │   ├── ParticipantApplicationService.java
│   │   ├── CsvImportService.java         # CSV一括登録サービス
│   │   ├── CsvExportService.java         # CSV出力サービス
│   │   ├── dto/
│   │   │   ├── CreateParticipantCommand.java
│   │   │   ├── ParticipantDto.java
│
├── infrastructure/
│   ├── dynamodb/
│   │   ├── DynamoDBGroupRepository.java  # Group永続化
│   │   ├── DynamoDBGroupParticipantListRepository.java
│   │   ├── entity/
│   │   │   ├── GroupEntity.java          # DynamoDBエンティティ
│   │   │   ├── ParticipantEntity.java
│
├── presentation/
│   ├── controller/
│   │   ├── GroupController.java          # Group API
│   │   ├── ParticipantController.java    # Participant API
│   │   ├── request/
│   │   │   ├── CreateGroupRequest.java
│   │   │   ├── CreateParticipantRequest.java
│   │   ├── response/
│   │   │   ├── GroupResponse.java
│   │   │   ├── ParticipantResponse.java
```

---

## 3. 開発の始め方

### 3.1 Phase 2 実装タスクの流れ

Phase 2（実装）は以下の順序で進めます：

```
1. ドメイン層
   ├── Rank値オブジェクト（段級位）
   ├── Groupエンティティ
   ├── Participantエンティティ
   └── GroupParticipantList集約ルート

2. インフラ層
   ├── DynamoDBテーブル作成（local）
   ├── DynamoDBGroupRepository実装
   └── DynamoDBGroupParticipantListRepository実装

3. アプリケーション層
   ├── GroupApplicationService
   ├── ParticipantApplicationService
   ├── CsvImportService
   └── CsvExportService

4. プレゼンテーション層
   ├── GroupController
   └── ParticipantController

5. フロントエンド
   ├── GroupTabs UI
   ├── ParticipantTable UI
   ├── CSV Import/Export UI
   └── API連携

6. E2Eテスト
   ├── 手動登録シナリオ
   ├── CSV登録シナリオ
   └── CSV出力シナリオ
```

### 3.2 TDD開発サイクル

各タスクは以下のサイクルで実装：

```
1. テスト作成（Red）
   - 単体テスト: JUnit 5 + Mockito
   - E2Eテスト: Playwright

2. 実装（Green）
   - 最小限のコードでテストを通す

3. リファクタリング（Refactor）
   - コード品質向上
   - 重複排除
```

---

## 4. 重要な実装ポイント

### 4.1 段級位（Rank）の扱い

**値オブジェクト設計**:
```java
public record Rank(int level, String displayName) implements Comparable<Rank> {
    private static final Pattern RANK_PATTERN = 
        Pattern.compile("^(初段|[2-9]段|[1-9]級|1[0-9]級|20級)$");
    
    public static Rank parse(String input) {
        // バリデーション + level変換
        // 初段 → 1, 3段 → 3, 9段 → 9, 1級 → -1, 3級 → -3, 20級 → -20
    }
    
    @Override
    public int compareTo(Rank other) {
        return Integer.compare(other.level, this.level); // 降順
    }
}
```

**重要**: 
- level値は段（正数）、級（負数）で表現
- ソートは降順（高段位が先）
- バリデーション失敗時は `IllegalArgumentException`

### 4.2 ダミーユーザー管理

**自動管理ロジック**:
```java
public class GroupParticipantList {
    private void ensureEvenCount() {
        long realCount = participants.stream()
            .filter(p -> !p.isDummy())
            .count();
        
        removeExistingDummy(); // 既存ダミー削除
        
        if (realCount % 2 == 1) {
            Participant dummy = new Participant(
                UUID.randomUUID(),
                groupId,
                null, // affiliation
                "ダミーユーザー（不戦勝）",
                null, // rank
                true, // isDummy
                getMaxRegistrationOrder() + 1
            );
            participants.add(dummy);
        }
    }
}
```

**トリガー**:
- 参加者追加後
- 参加者削除後
- CSV一括登録後

### 4.3 CSV文字エンコーディング検出

**バックエンド（juniversalchardet）**:
```java
public class CsvImportService {
    public List<CsvRow> parseCsv(byte[] csvBytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(csvBytes, 0, csvBytes.length);
        detector.dataEnd();
        
        String encoding = detector.getDetectedCharset();
        if (encoding == null) {
            encoding = "UTF-8"; // フォールバック
        }
        
        String csvText = new String(csvBytes, Charset.forName(encoding));
        // CSV解析処理
    }
}
```

**フロントエンド（encoding-japanese）**:
```typescript
export const parseCsvFile = async (file: File): Promise<CsvRow[]> => {
  const arrayBuffer = await file.arrayBuffer();
  const uint8Array = new Uint8Array(arrayBuffer);
  
  const detected = Encoding.detect(uint8Array);
  const unicodeArray = Encoding.convert(uint8Array, {
    to: 'UNICODE',
    from: detected || 'AUTO'
  });
  
  const csvText = Encoding.codeToString(unicodeArray);
  const parsed = Papa.parse<CsvRow>(csvText, {
    header: true,
    skipEmptyLines: true
  });
  
  return parsed.data;
};
```

### 4.4 DynamoDB GSIクエリ

**参加者の段級位順取得**:
```java
public class DynamoDBGroupParticipantListRepository {
    public List<Participant> findParticipantsSortedByRank(UUID groupId) {
        QueryRequest request = QueryRequest.builder()
            .tableName("Participant")
            .indexName("groupId-rankLevel-index")
            .keyConditionExpression("groupId = :groupId")
            .expressionAttributeValues(Map.of(
                ":groupId", AttributeValue.builder().s(groupId.toString()).build()
            ))
            .scanIndexForward(false) // 降順（level大→小）
            .build();
        
        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
            .map(this::toParticipant)
            .toList();
    }
}
```

**GSI設計**:
- PK: `groupId`
- SK: `rankLevel#registrationOrder`（複合ソートキー）
- 効果: アプリケーション層でのソート不要

### 4.5 React Query キャッシング

**グループデータのキャッシュ**:
```typescript
export const useGroupParticipants = (groupId: string) => {
  return useQuery(
    ['participants', groupId],
    () => participantService.getParticipants(groupId),
    {
      staleTime: 30000, // 30秒キャッシュ
      refetchOnWindowFocus: false,
    }
  );
};
```

**効果**: タブ切り替え時の無駄なAPI呼び出し削減（目標: 1秒以内）

---

## 5. テスト戦略

### 5.1 単体テスト

**ドメイン層**:
```java
@Test
void testRankParse_初段() {
    Rank rank = Rank.parse("初段");
    assertEquals(1, rank.level());
    assertEquals("初段", rank.displayName());
}

@Test
void testEnsureEvenCount_奇数でダミー追加() {
    GroupParticipantList list = new GroupParticipantList(group);
    list.addParticipant(participant1); // 1名（奇数）
    
    assertEquals(2, list.getParticipants().size()); // ダミー含む
    assertTrue(list.getParticipants().get(1).isDummy());
}
```

**アプリケーション層**:
```java
@Test
void testCsvImport_32名登録成功() {
    List<CsvRow> rows = createCsvRows(32);
    csvImportService.importParticipants(groupId, rows);
    
    List<Participant> result = participantRepository.findByGroupId(groupId);
    assertEquals(32, result.size());
}
```

### 5.2 E2Eテスト

**Playwright シナリオ**:
```typescript
test('CSV一括登録が成功する', async ({ page }) => {
  await page.goto('/tournaments/1/participants');
  
  // GROUP 1タブを選択
  await page.click('button:has-text("GROUP 1")');
  
  // CSVアップロード
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles('test-data/participants.csv');
  
  // 登録完了を確認
  await expect(page.locator('text=32名登録されました')).toBeVisible();
  
  // 参加者一覧を確認
  const rows = page.locator('table tbody tr');
  await expect(rows).toHaveCount(32);
});
```

---

## 6. パフォーマンス目標

| 項目 | 目標 | 測定方法 |
|------|------|----------|
| CSV一括登録（32名） | 1分以内 | JMeter負荷テスト |
| 参加者登録後の対戦表反映（256名規模） | 3秒以内 | JMeterまたはPlaywright |
| グループ切り替え | 1秒以内 | Playwright Performance測定 |

---

## 7. 開発環境セットアップ

### 7.1 バックエンド

```bash
# DynamoDB Local起動
docker run -p 8000:8000 amazon/dynamodb-local

# テーブル作成
aws dynamodb create-table \
  --table-name Group \
  --attribute-definitions \
    AttributeName=tournamentId,AttributeType=S \
    AttributeName=groupNumber,AttributeType=N \
  --key-schema \
    AttributeName=tournamentId,KeyType=HASH \
    AttributeName=groupNumber,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000

# アプリケーション起動
cd backend
./gradlew bootRun
```

### 7.2 フロントエンド

```bash
cd frontend
npm install
npm run dev
```

---

## 8. よくある質問

### Q1: 段級位の範囲は？
A: 段位は初段〜9段、級位は1級〜20級です。

### Q2: CSVファイルの最大サイズは？
A: 5MB（Spring Bootデフォルト）。32名規模なら十分。

### Q3: ダミーユーザーは対戦表に含まれる？
A: はい。不戦勝扱いで対戦表に含まれます。

### Q4: グループ間で参加者を移動できる？
A: Phase 2では対象外。将来の拡張として検討。

---

## 9. 次のステップ

1. **Phase 2実装開始**: `/speckit.tasks`コマンドでタスクリスト生成
2. **TDDサイクル実施**: ドメイン層から順次実装
3. **E2Eテスト**: Playwrightでユーザーシナリオ検証
4. **パフォーマンス測定**: JMeterで目標値達成確認

---

**関連ドキュメント**:
- [仕様書](./spec.md)
- [実装計画](./plan.md)
- [データモデル](./data-model.md)
- [API契約](./contracts/)
