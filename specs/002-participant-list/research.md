# Phase 0: 調査結果 - 参加者一覧管理

**機能**: 参加者一覧管理（グループ別CSV入出力対応）
**日付**: 2026-01-03
**ステータス**: 完了

このドキュメントは、実装計画Phase 0で特定されたすべての技術的な不明点を解決し、Phase 1（設計）に進むための意思決定を記録します。

---

## 1. DynamoDB テーブル設計

### 決定事項

**Groupテーブル**:
- **テーブル名**: `Group`
- **PK**: `tournamentId` (String/UUID)
- **SK**: `groupNumber` (Number, 1-8)
- **属性**:
  - `groupId` (UUID)
  - `displayName` (String, 例: "GROUP 1")
- **理由**: トーナメントIDとグループ番号の組み合わせで一意に識別。グループ番号でソート可能。

**Participantテーブル**:
- **テーブル名**: `Participant`
- **PK**: `groupId` (String/UUID)
- **SK**: `participantId` (String/UUID)
- **属性**:
  - `affiliation` (String, nullable)
  - `name` (String)
  - `rankLevel` (Number)
  - `rankDisplayName` (String)
  - `isDummy` (Boolean)
  - `registrationOrder` (Number)
- **GSI**: `groupId-rankLevel-index`
  - PK: `groupId`
  - SK: `rankLevel#registrationOrder`（複合ソートキー）
  - 用途: グループ内の参加者を段級位順→登録順で効率的に取得
- **理由**: グループIDで参加者をグルーピング。段級位順ソートのためのGSI設計。

**32名上限チェック**:
- アプリケーション層で `COUNT` クエリを実行
- 新規登録前に `groupId` の参加者数をチェック
- DynamoDBの条件付き書き込みは使用しない（複雑化を避ける）

### 代替案の検討

**代替案1**: 単一テーブル設計（Group + Participant）
- **却下理由**: アクセスパターンが異なり、GSI設計が複雑化。DDDの集約境界と不整合。

**代替案2**: 段級位順ソートをアプリケーション層で実施
- **却下理由**: 32名×8グループ（256名）規模では許容できるが、GSI活用でDB側でソート済みデータを取得する方が効率的。

---

## 2. CSV文字エンコーディング検出

### 決定事項

**バックエンド（Java）**:
- **ライブラリ**: `juniversalchardet` (Mozilla's Universal Charset Detector)
- **実装**:
  ```java
  UniversalDetector detector = new UniversalDetector(null);
  detector.handleData(csvBytes, 0, csvBytes.length);
  detector.dataEnd();
  String encoding = detector.getDetectedCharset(); // "UTF-8" or "Shift_JIS"
  ```
- **フォールバック**: 検出失敗時はUTF-8をデフォルトとして再試行
- **エラーハンドリング**: デコード失敗時は「文字エンコーディングを判定できませんでした」エラーを返す

**フロントエンド（TypeScript）**:
- **ライブラリ**: 
  - `papaparse`: CSV解析
  - `encoding-japanese`: 文字エンコーディング検出・変換
- **実装**:
  ```typescript
  const detected = Encoding.detect(fileBytes);
  const unicodeArray = Encoding.convert(fileBytes, {
    to: 'UNICODE',
    from: detected
  });
  const csvText = Encoding.codeToString(unicodeArray);
  const parsed = Papa.parse(csvText);
  ```
- **精度**: encoding-japaneseはShift-JIS検出に特化し、日本語CSVで高精度

### ベストプラクティス

- **BOM（Byte Order Mark）の処理**: UTF-8 BOM付きファイルに対応
- **エラーメッセージ**: 「CSVファイルの文字エンコーディングが不正です。UTF-8またはShift-JISで保存してください。」
- **テストケース**: UTF-8、UTF-8 with BOM、Shift-JIS、ASCII（UTF-8として扱う）

### 代替案の検討

**代替案1**: ユーザーにエンコーディングを選択させる
- **却下理由**: UX悪化。自動判定で99%以上の精度を目指す（SC-004）。

**代替案2**: UTF-8のみサポート
- **却下理由**: Excel for Windows（Shift-JISデフォルト）との互換性が必須（FR-012）。

---

## 3. 段級位フォーマット検証

### 決定事項

**有効な段級位フォーマット**:
- **段**: `初段`, `2段`, `3段`, ..., `9段`（初段〜9段）
- **級**: `1級`, `2級`, `3級`, ..., `20級`（1級〜20級）

**正規表現**:
```java
private static final Pattern RANK_PATTERN = 
    Pattern.compile("^(初段|[2-9]段|[1-9]級|1[0-9]級|20級)$");
```

**level値への変換**:
```java
public record Rank(int level, String displayName) {
    public static Rank parse(String input) {
        if (!RANK_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("段級位の形式が不正です: " + input);
        }
        
        if (input.equals("初段")) return new Rank(1, "初段");
        if (input.equals("初級")) return new Rank(-1, "初級");
        
        if (input.endsWith("段")) {
            int level = Integer.parseInt(input.substring(0, input.length() - 1));
            return new Rank(level, input);
        } else { // 級
            int level = -Integer.parseInt(input.substring(0, input.length() - 1));
            return new Rank(level, input);
        }
    }
    
    public int compareTo(Rank other) {
        return Integer.compare(this.level, other.level); // 降順: level大→小
    }
}
```

**level値の例**:
- `初段` → `1`
- `3段` → `3`
- `9段` → `9`
- `1級` → `-1`
- `3級` → `-3`
- `20級` → `-20`

**ソート順**: level値降順（例: 9段(9) → 5段(5) → 3段(3) → 初段(1) → 1級(-1) → 3級(-3) → 20級(-20)）

### エラーメッセージ

- **形式不正**: 「段級位の形式が不正です（行X）: "abc段"」
- **CSV列名**: 段級位列のヘッダーは「段級位」または「rank」を許容

---

## 4. Material-UI タブコンポーネント設計

### 決定事項

**コンポーネント構成**:
```tsx
<Tabs 
  value={selectedGroupNumber}
  onChange={handleTabChange}
  variant="scrollable"
  scrollButtons="auto"
>
  <Tab label="GROUP 1" value={1} />
  <Tab label="GROUP 2" value={2} />
  {/* ... GROUP 8まで */}
</Tabs>
```

**パフォーマンス最適化**:
- **タブコンテンツの遅延レンダリング**: 非選択タブのコンテンツは`display: none`
- **React.memo()**: ParticipantTableコンポーネントをメモ化
- **React Query**: グループデータをキャッシュ、タブ切り替え時に即座に表示
  ```typescript
  const { data: participants } = useQuery(
    ['participants', groupId],
    () => participantService.getParticipants(groupId),
    { staleTime: 30000 } // 30秒キャッシュ
  );
  ```

**レスポンシブ対応**:
- **PC**: タブを横スクロール（`scrollButtons="auto"`）
- **タブレット/スマホ**: タブサイズ縮小、スワイプ操作対応

**タブ切り替え応答時間目標**: 1秒以内（SC-003）
- React Queryのキャッシュで即座にデータ表示
- 新規データフェッチは非同期バックグラウンド

### ベストプラクティス

- **URLパラメータ連動**: `/participants?group=1` でグループ指定、ブックマーク・共有可能
- **初期表示**: 最初のグループ（GROUP 1）をデフォルト選択
- **空グループの扱い**: 参加者0名のグループもタブ表示、「参加者が登録されていません」メッセージ

### 代替案の検討

**代替案1**: ドロップダウンメニューでグループ選択
- **却下理由**: タブの方が一覧性が高く、複数グループ間の切り替えが容易。

**代替案2**: 全グループを単一画面に縦並び表示
- **却下理由**: 256名分のデータを一度に表示するとパフォーマンス悪化。

---

## 5. ダミーユーザー管理戦略

### 決定事項

**ダミーユーザーの属性**:
- `name`: "ダミーユーザー（不戦勝）"
- `isDummy`: `true`
- `rank`: `null`（段級位なし）
- `affiliation`: `null`
- `registrationOrder`: グループ内の最大値 + 1（末尾に配置）

**自動追加・削除タイミング**:
- **追加**: 参加者登録後、グループ内が奇数の場合
- **削除**: 参加者削除後、グループ内が偶数になった場合（ダミーユーザーを削除）
- **実装場所**: `GroupParticipantList.ensureEvenCount()`メソッド

**エッジケース処理**:
- **0名**: ダミーユーザー追加なし（0は偶数）
- **1名**: ダミーユーザー1名追加（合計2名）
- **既にダミーユーザー存在**: 奇数チェック後、既存ダミーユーザーを削除してから再追加

**CSV出力時の扱い**:
- ダミーユーザー（`isDummy=true`）はCSV出力に含めない
- フィルタリング処理: `participants.stream().filter(p -> !p.isDummy())`

### 実装パターン

```java
public class GroupParticipantList {
    public void ensureEvenCount(UUID groupId) {
        long realParticipantCount = participants.stream()
            .filter(p -> !p.isDummy()).count();
        
        if (realParticipantCount % 2 == 1) {
            // 奇数 → ダミーユーザー追加
            removeExistingDummy(); // 既存削除
            addDummy(groupId);
        } else {
            // 偶数 → ダミーユーザー削除
            removeExistingDummy();
        }
    }
}
```

---

## 6. パフォーマンス検証計画

### 目標値（再確認）

- **SC-001**: CSV一括登録（32名）を1分以内
- **SC-002**: 参加者登録後の対戦表反映を3秒以内（256名規模）
- **SC-003**: グループ切り替えを1秒以内

### 検証方法

**負荷テスト**:
- **ツール**: JMeter
- **シナリオ**: 
  - 8グループに各32名を順次登録（合計256名）
  - 各グループでCSV一括登録（32名）を実行
  - グループ切り替え（タブクリック）を連続実行
- **測定**: 各API呼び出しのレスポンスタイム、DynamoDBクエリ時間

**最適化ポイント**:
- DynamoDB GSIのクエリ効率
- React QueryのキャッシュヒットHuman: 率
- CSVパース処理の並列化

---

## Phase 0 完了チェックリスト

- [x] DynamoDBテーブル設計完了（Group、Participant）
- [x] CSV文字エンコーディング検出方法決定（juniversalchardet、encoding-japanese）
- [x] 段級位フォーマット検証・パース処理設計完了
- [x] Material-UIタブコンポーネント設計完了
- [x] ダミーユーザー管理戦略決定
- [x] パフォーマンス検証計画策定

**結論**: すべての技術的不明点が解決されました。Phase 1（設計）に進む準備が整っています。
