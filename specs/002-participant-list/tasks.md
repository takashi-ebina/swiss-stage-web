# Implementation Tasks - 参加者一覧管理

**Feature**: 参加者一覧管理（グループ別CSV入出力対応）  
**Branch**: `002-participant-list`  
**Status**: Phase 2 - Implementation

このドキュメントは、[plan.md](plan.md)で定義された実装計画を実行可能なタスクに分解したものです。各タスクはTDD（Test-Driven Development）で進めます。

---

## タスク実行ガイドライン

### TDDサイクル

各タスクは以下のサイクルで実装：

1. **Red**: テストを先に書く（失敗することを確認）
2. **Green**: 最小限のコードでテストを通す
3. **Refactor**: コードの品質を向上させる

### タスクステータス

- `[ ]` 未着手
- `[~]` 進行中
- `[x]` 完了

### 優先度

- **P0**: 必須（MVP）
- **P1**: 高優先度（初期リリースに必要）
- **P2**: 中優先度（後続リリース）

---

## Phase 2: Implementation Tasks

### セクション 1: ドメイン層（Domain Layer）

#### Task 1.1: Rank値オブジェクトの実装 (P0) ✅

**目的**: 段級位の形式検証と比較機能を提供する値オブジェクトを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/participant/Rank.java`
- `backend/src/test/java/com/swiss_stage/unit/domain/participant/RankTest.java`

**受け入れ条件**:
- [x] 正規表現パターンで段級位フォーマットを検証（初段、2-9段、1-20級）
- [x] `parse(String input)`メソッドで文字列からRankインスタンスを生成
- [x] 不正なフォーマットで`IllegalArgumentException`をスロー
- [x] level値が正しく変換される（初段→1、3段→3、1級→-1、3級→-3）
- [x] `compareTo()`でlevel値降順の比較が可能
- [x] 単体テストで10以上のテストケースをカバー

**テストケース**:
- 正常系: "初段", "3段", "9段", "1級", "3級", "20級"
- 異常系: "abc段", "0段", "10段", "0級", "21級", "初級", null, 空文字

**依存**: なし

---

#### Task 1.2: Groupエンティティの実装 (P0)

**目的**: グループの基本情報を管理するエンティティを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/group/Group.java`
- `backend/src/test/java/com/swiss_stage/unit/domain/group/GroupTest.java`

**受け入れ条件**:
- [ ] `groupId`, `tournamentId`, `groupNumber`, `displayName`属性を持つ
- [ ] `groupNumber`が1-8の範囲外で`IllegalArgumentException`をスロー
- [ ] `displayName`が"GROUP {groupNumber}"形式で自動生成される
- [ ] `MAX_PARTICIPANTS = 32`, `MAX_GROUPS = 8`定数を定義
- [ ] 単体テストで不変条件を検証

**テストケース**:
- 正常系: groupNumber=1, 8
- 異常系: groupNumber=0, 9, -1

**依存**: なし

---

#### Task 1.3: Participantエンティティの実装 (P0)

**目的**: 参加者の基本情報を管理するエンティティを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/participant/Participant.java`
- `backend/src/test/java/com/swiss_stage/unit/domain/participant/ParticipantTest.java`

**受け入れ条件**:
- [ ] `participantId`, `groupId`, `affiliation`, `name`, `rank`, `isDummy`, `registrationOrder`属性を持つ
- [ ] `name`が空でない（必須バリデーション）
- [ ] `name`が50文字以内
- [ ] `affiliation`が100文字以内
- [ ] `isDummy=true`の場合、`rank`はnull許容
- [ ] 単体テストで不変条件を検証

**テストケース**:
- 正常系: 通常参加者、ダミーユーザー、affiliation=null
- 異常系: name=null, name=空文字, name>50文字, affiliation>100文字

**依存**: Task 1.1 (Rank)

---

#### Task 1.4: GroupParticipantList集約ルートの実装 (P0)

**目的**: グループ参加者リストの整合性を管理する集約ルートを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/participant/GroupParticipantList.java`
- `backend/src/test/java/com/swiss_stage/unit/domain/participant/GroupParticipantListTest.java`

**受け入れ条件**:
- [ ] `addParticipant()`で参加者追加（32名上限チェック + ダミーユーザー調整）
- [ ] `removeParticipant()`で参加者削除（ダミーユーザー調整）
- [ ] `ensureEvenCount()`で奇数→ダミー追加、偶数→ダミー削除
- [ ] `getSortedParticipants()`で段級位順→登録順ソート
- [ ] `importFromCsv()`でCSV一括登録（既存削除 + 新規追加）
- [ ] `exportToCsv()`でダミーユーザー除外リスト取得
- [ ] 32名超過時に`DomainException`をスロー
- [ ] 単体テストで15以上のテストケースをカバー

**テストケース**:
- addParticipant: 0名→1名（ダミー追加）、1名→2名（ダミー削除）、31名→32名
- removeParticipant: 2名→1名（ダミー追加）、3名→2名（ダミー削除）
- ensureEvenCount: 0名、1名、奇数、偶数
- importFromCsv: 32名一括登録、既存削除
- 上限チェック: 32名超過エラー

**依存**: Task 1.2 (Group), Task 1.3 (Participant)

---

#### Task 1.5: DomainExceptionの実装 (P0)

**目的**: ドメイン層の例外クラスを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/common/DomainException.java`
- `backend/src/test/java/com/swiss_stage/unit/domain/common/DomainExceptionTest.java`

**受け入れ条件**:
- [ ] メッセージ付きコンストラクタ
- [ ] 原因（cause）付きコンストラクタ
- [ ] RuntimeExceptionを継承

**依存**: なし

---

### セクション 2: インフラ層（Infrastructure Layer）

#### Task 2.1: DynamoDB Local環境構築 (P0)

**目的**: ローカル開発用のDynamoDB環境をセットアップ

**受け入れ条件**:
- [ ] Docker ComposeでDynamoDB Localを起動
- [ ] `Group`テーブル作成（PK: tournamentId, SK: groupNumber）
- [ ] `Participant`テーブル作成（PK: groupId, SK: participantId）
- [ ] GSI `groupId-rankLevel-index`作成（PK: groupId, SK: rankLevel#registrationOrder）
- [ ] テーブル作成スクリプトを`backend/scripts/dynamodb/`に配置

**ファイル**:
- `backend/scripts/dynamodb/create-tables.sh`

**依存**: なし

---

#### Task 2.2: DynamoDBエンティティマッピングの実装 (P0)

**目的**: DynamoDBのItem ↔ ドメインエンティティの変換を実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/infrastructure/dynamodb/entity/GroupEntity.java`
- `backend/src/main/java/com/swiss_stage/infrastructure/dynamodb/entity/ParticipantEntity.java`
- `backend/src/test/java/com/swiss_stage/unit/infrastructure/dynamodb/entity/GroupEntityTest.java`
- `backend/src/test/java/com/swiss_stage/unit/infrastructure/dynamodb/entity/ParticipantEntityTest.java`

**受け入れ条件**:
- [x] `GroupEntity`がDynamoDB属性アノテーション（@DynamoDbPartitionKey, @DynamoDbSortKey）を持つ
- [x] `ParticipantEntity`がGSI用の属性を持つ
- [x] `toDomain()`メソッドでドメインエンティティに変換
- [x] `fromDomain()`メソッドでDynamoDBエンティティに変換
- [x] 単体テストで双方向変換を検証

**依存**: Task 1.2 (Group), Task 1.3 (Participant)

---

#### Task 2.3: DynamoDBGroupRepositoryの実装 (P0)

**目的**: GroupリポジトリのDynamoDB実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/group/GroupRepository.java`
- `backend/src/main/java/com/swiss_stage/infrastructure/dynamodb/DynamoDBGroupRepository.java`
- `backend/src/test/java/com/swiss_stage/unit/infrastructure/dynamodb/DynamoDBGroupRepositoryTest.java`

**受け入れ条件**:
- [x] `findByTournamentIdAndGroupNumber(UUID tournamentId, int groupNumber)`を実装
- [x] `findByTournamentId(UUID tournamentId)`を実装
- [x] `save(Group group)`を実装
- [x] `delete(UUID tournamentId, int groupNumber)`を実装
- [x] 単体テストでDynamoDB操作をモック化して検証

**依存**: Task 1.2 (Group), Task 2.2 (GroupEntity)

---

#### Task 2.4: DynamoDBGroupParticipantListRepositoryの実装 (P0)

**目的**: GroupParticipantListリポジトリのDynamoDB実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/domain/participant/GroupParticipantListRepository.java`
- `backend/src/main/java/com/swiss_stage/infrastructure/dynamodb/DynamoDBGroupParticipantListRepository.java`
- `backend/src/test/java/com/swiss_stage/unit/infrastructure/dynamodb/DynamoDBGroupParticipantListRepositoryTest.java`

**受け入れ条件**:
- [x] `findByGroupId(UUID groupId)`を実装（GSIクエリ使用）
- [x] `save(GroupParticipantList list)`を実装（集約全体を永続化）
- [x] `findParticipantsSortedByRank(UUID groupId)`でGSIクエリを実装
- [x] 単体テストでDynamoDB操作をモック化して検証

**依存**: Task 1.4 (GroupParticipantList), Task 2.2 (ParticipantEntity)

---

### セクション 3: アプリケーション層（Application Layer）

#### Task 3.1: GroupApplicationServiceの実装 (P0)

**目的**: グループ管理のアプリケーションサービスを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/application/group/GroupApplicationService.java`
- `backend/src/main/java/com/swiss_stage/application/group/dto/CreateGroupCommand.java`
- `backend/src/main/java/com/swiss_stage/application/group/dto/GroupDto.java`
- `backend/src/test/java/com/swiss_stage/unit/application/group/GroupApplicationServiceTest.java`

**受け入れ条件**:
- [ ] `createGroup(CreateGroupCommand command)`を実装
- [ ] `getGroupsByTournamentId(UUID tournamentId)`を実装
- [ ] `getGroupById(UUID groupId)`を実装
- [ ] `deleteGroup(UUID groupId)`を実装（参加者存在チェック）
- [ ] DTO変換処理を実装
- [ ] 単体テストでリポジトリをモック化して検証

**依存**: Task 1.2 (Group), Task 2.3 (GroupRepository)

---

#### Task 3.2: ParticipantApplicationServiceの実装 (P0)

**目的**: 参加者管理のアプリケーションサービスを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/application/participant/ParticipantApplicationService.java`
- `backend/src/main/java/com/swiss_stage/application/participant/dto/CreateParticipantCommand.java`
- `backend/src/main/java/com/swiss_stage/application/participant/dto/UpdateParticipantCommand.java`
- `backend/src/main/java/com/swiss_stage/application/participant/dto/ParticipantDto.java`
- `backend/src/test/java/com/swiss_stage/unit/application/participant/ParticipantApplicationServiceTest.java`

**受け入れ条件**:
- [ ] `addParticipant(CreateParticipantCommand command)`を実装
- [ ] `getParticipantsByGroupId(UUID groupId, boolean includeDummy)`を実装
- [ ] `updateParticipant(UUID participantId, UpdateParticipantCommand command)`を実装
- [ ] `deleteParticipant(UUID participantId)`を実装
- [ ] DTO変換処理を実装
- [ ] 単体テストでリポジトリをモック化して検証

**依存**: Task 1.3 (Participant), Task 1.4 (GroupParticipantList), Task 2.4 (Repository)

---

#### Task 3.3: CsvImportServiceの実装 (P1)

**目的**: CSV一括登録のサービスロジックを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/application/participant/CsvImportService.java`
- `backend/src/main/java/com/swiss_stage/application/participant/dto/CsvRow.java`
- `backend/src/test/java/com/swiss_stage/unit/application/participant/CsvImportServiceTest.java`

**受け入れ条件**:
- [ ] `parseCsv(byte[] csvBytes)`でエンコーディング自動検出（juniversalchardet）
- [ ] CSV行をバリデーション（name必須、rank形式、32名上限）
- [ ] `importParticipants(UUID groupId, List<CsvRow> rows)`を実装
- [ ] エラー行の詳細情報を返す
- [ ] 単体テストでUTF-8, Shift-JIS, 異常系をカバー

**テストケース**:
- UTF-8エンコーディング
- Shift-JISエンコーディング
- UTF-8 with BOM
- 不正な段級位フォーマット
- 32名超過
- 必須列不足

**依存**: Task 1.1 (Rank), Task 1.4 (GroupParticipantList)

---

#### Task 3.4: CsvExportServiceの実装 (P1)

**目的**: CSV出力のサービスロジックを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/application/participant/CsvExportService.java`
- `backend/src/test/java/com/swiss_stage/unit/application/participant/CsvExportServiceTest.java`

**受け入れ条件**:
- [ ] `exportParticipants(UUID groupId)`でダミーユーザー除外リストをCSV生成
- [ ] UTF-8 with BOM形式で出力
- [ ] ヘッダー行（氏名、所属、段級位）を含む
- [ ] 単体テストでCSV形式を検証

**依存**: Task 1.4 (GroupParticipantList)

---

### セクション 4: プレゼンテーション層（Presentation Layer）

#### Task 4.1: GroupControllerの実装 (P0)

**目的**: Group管理のREST APIを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/presentation/controller/GroupController.java`
- `backend/src/main/java/com/swiss_stage/presentation/controller/request/CreateGroupRequest.java`
- `backend/src/main/java/com/swiss_stage/presentation/controller/response/GroupResponse.java`
- `backend/src/test/java/com/swiss_stage/unit/presentation/controller/GroupControllerTest.java`

**受け入れ条件**:
- [ ] `POST /api/v1/tournaments/{tournamentId}/groups`を実装
- [ ] `GET /api/v1/tournaments/{tournamentId}/groups`を実装
- [ ] `GET /api/v1/groups/{groupId}`を実装
- [ ] `DELETE /api/v1/groups/{groupId}`を実装
- [ ] バリデーション（groupNumber 1-8）
- [ ] エラーハンドリング（404, 409）
- [ ] 単体テストでMockMvcを使用

**依存**: Task 3.1 (GroupApplicationService)

---

#### Task 4.2: ParticipantControllerの実装 (P0)

**目的**: Participant管理のREST APIを実装

**ファイル**:
- `backend/src/main/java/com/swiss_stage/presentation/controller/ParticipantController.java`
- `backend/src/main/java/com/swiss_stage/presentation/controller/request/CreateParticipantRequest.java`
- `backend/src/main/java/com/swiss_stage/presentation/controller/request/UpdateParticipantRequest.java`
- `backend/src/main/java/com/swiss_stage/presentation/controller/response/ParticipantResponse.java`
- `backend/src/test/java/com/swiss_stage/unit/presentation/controller/ParticipantControllerTest.java`

**受け入れ条件**:
- [ ] `POST /api/v1/groups/{groupId}/participants`を実装
- [ ] `GET /api/v1/groups/{groupId}/participants`を実装（クエリパラメータ: includeDummy）
- [ ] `PUT /api/v1/participants/{participantId}`を実装
- [ ] `DELETE /api/v1/participants/{participantId}`を実装
- [ ] `POST /api/v1/groups/{groupId}/participants/import`を実装（CSV）
- [ ] `GET /api/v1/groups/{groupId}/participants/export`を実装（CSV）
- [ ] バリデーション（段級位フォーマット、文字数制限）
- [ ] エラーハンドリング（400, 404, 409, 413）
- [ ] 単体テストでMockMvcを使用

**依存**: Task 3.2 (ParticipantApplicationService), Task 3.3 (CsvImportService), Task 3.4 (CsvExportService)

---

### セクション 5: フロントエンド（Frontend）

#### Task 5.1: Participant型定義の実装 (P0)

**目的**: TypeScript型定義を実装

**ファイル**:
- `frontend/src/types/Participant.ts`
- `frontend/src/types/Group.ts`
- `frontend/src/types/Rank.ts`

**受け入れ条件**:
- [ ] `Participant`, `Group`, `Rank`インターフェースを定義
- [ ] APIレスポンス型に対応

**依存**: なし

---

#### Task 5.2: participantServiceの実装 (P1)

**目的**: 参加者管理のAPIクライアントを実装

**ファイル**:
- `frontend/src/services/participantService.ts`
- `frontend/src/services/__tests__/participantService.test.ts`

**受け入れ条件**:
- [ ] `getParticipants(groupId, includeDummy)`を実装
- [ ] `addParticipant(groupId, data)`を実装
- [ ] `updateParticipant(participantId, data)`を実装
- [ ] `deleteParticipant(participantId)`を実装
- [ ] `importParticipantsFromCsv(groupId, file)`を実装
- [ ] `exportParticipantsToCsv(groupId)`を実装
- [ ] 単体テストでfetchをモック化

**依存**: Task 5.1 (型定義)

---

#### Task 5.3: groupServiceの実装 (P0)

**目的**: グループ管理のAPIクライアントを実装

**ファイル**:
- `frontend/src/services/groupService.ts`
- `frontend/src/services/__tests__/groupService.test.ts`

**受け入れ条件**:
- [ ] `getGroups(tournamentId)`を実装
- [ ] `createGroup(tournamentId, groupNumber)`を実装
- [ ] `deleteGroup(groupId)`を実装
- [ ] 単体テストでfetchをモック化

**依存**: Task 5.1 (型定義)

---

#### Task 5.4: GroupTabsコンポーネントの実装 (P0)

**目的**: Material-UIタブでグループ切り替えUIを実装

**ファイル**:
- `frontend/src/components/participant/GroupTabs.tsx`
- `frontend/src/components/participant/__tests__/GroupTabs.test.tsx`

**受け入れ条件**:
- [ ] Material-UI `Tabs`コンポーネントを使用
- [ ] GROUP 1〜GROUP 8のタブを表示
- [ ] 選択タブをURLパラメータと連動（`?group=1`）
- [ ] React Queryでグループデータをキャッシュ
- [ ] 単体テストでタブクリックを検証

**依存**: Task 5.3 (groupService)

---

#### Task 5.5: ParticipantTableコンポーネントの実装 (P0)

**目的**: 参加者一覧テーブルを実装

**ファイル**:
- `frontend/src/components/participant/ParticipantTable.tsx`
- `frontend/src/components/participant/__tests__/ParticipantTable.test.tsx`

**受け入れ条件**:
- [ ] Material-UI `Table`で参加者一覧を表示（所属、氏名、段級位）
- [ ] 段級位順→登録順でソート済み
- [ ] ダミーユーザーの表示切り替え
- [ ] React.memo()で最適化
- [ ] 単体テストで表示内容を検証

**依存**: Task 5.2 (participantService)

---

#### Task 5.6: CsvImportButtonコンポーネントの実装 (P1)

**目的**: CSV一括登録ボタンとダイアログを実装

**ファイル**:
- `frontend/src/components/participant/CsvImportButton.tsx`
- `frontend/src/components/participant/__tests__/CsvImportButton.test.tsx`

**受け入れ条件**:
- [ ] ファイル選択ボタン（`<input type="file" accept=".csv">`）
- [ ] PapaParse + encoding-japaneseでCSVパース（UTF-8/Shift-JIS自動検出）
- [ ] プログレスインジケーター表示
- [ ] エラー時に詳細メッセージ表示
- [ ] 単体テストでファイル選択とパースを検証

**依存**: Task 5.2 (participantService)

---

#### Task 5.7: CsvExportButtonコンポーネントの実装 (P1)

**目的**: CSV出力ボタンを実装

**ファイル**:
- `frontend/src/components/participant/CsvExportButton.tsx`
- `frontend/src/components/participant/__tests__/CsvExportButton.test.tsx`

**受け入れ条件**:
- [ ] CSV出力ボタン
- [ ] APIからCSVデータを取得してダウンロード
- [ ] ファイル名: `group_{groupNumber}_participants.csv`
- [ ] 単体テストでダウンロード処理を検証

**依存**: Task 5.2 (participantService)

---

#### Task 5.8: ParticipantFormDialogコンポーネントの実装 (P0)

**目的**: 参加者追加・編集ダイアログを実装

**ファイル**:
- `frontend/src/components/participant/ParticipantFormDialog.tsx`
- `frontend/src/components/participant/__tests__/ParticipantFormDialog.test.tsx`

**受け入れ条件**:
- [ ] Material-UI `Dialog`で入力フォーム（所属、氏名、段級位）
- [ ] 段級位のドロップダウン（初段〜9段、1級〜20級）
- [ ] バリデーション（氏名必須、文字数制限）
- [ ] 追加・編集モード切り替え
- [ ] 単体テストでフォーム入力を検証

**依存**: Task 5.2 (participantService)

---

#### Task 5.9: ParticipantsPageの実装 (P0)

**目的**: 参加者一覧ページのメインコンポーネントを実装

**ファイル**:
- `frontend/src/pages/ParticipantsPage.tsx`
- `frontend/src/pages/__tests__/ParticipantsPage.test.tsx`

**受け入れ条件**:
- [ ] GroupTabsコンポーネントを配置
- [ ] ParticipantTableコンポーネントを配置
- [ ] CsvImportButton, CsvExportButton, ParticipantFormDialogを配置
- [ ] React Queryでデータフェッチとキャッシュ
- [ ] 単体テストでページ全体を検証

**依存**: Task 5.4〜5.8

---

### セクション 6: E2Eテスト（End-to-End Testing）

#### Task 6.1: 手動登録シナリオのE2Eテスト (P0)

**目的**: Playwrightで参加者手動登録のE2Eテストを実装

**ファイル**:
- `frontend/tests/e2e/participant-manual-registration.spec.ts`

**受け入れ条件**:
- [ ] GROUP 1タブを選択
- [ ] 「参加者追加」ボタンをクリック
- [ ] 参加者情報を入力（所属、氏名、段級位）
- [ ] 保存ボタンをクリック
- [ ] 参加者一覧に追加されることを確認
- [ ] ダミーユーザーが自動追加されることを確認（奇数の場合）

**依存**: Task 5.9 (ParticipantsPage), バックエンドAPI完成

---

#### Task 6.2: CSV一括登録シナリオのE2Eテスト (P1)

**目的**: PlaywrightでCSV一括登録のE2Eテストを実装

**ファイル**:
- `frontend/tests/e2e/participant-csv-import.spec.ts`
- `frontend/tests/test-data/participants.csv`

**受け入れ条件**:
- [ ] GROUP 1タブを選択
- [ ] 「CSV一括登録」ボタンをクリック
- [ ] テスト用CSVファイルをアップロード
- [ ] 登録完了メッセージを確認
- [ ] 参加者一覧が更新されることを確認
- [ ] 32名一括登録が1分以内に完了することを確認（パフォーマンス）

**依存**: Task 5.9 (ParticipantsPage), バックエンドAPI完成

---

#### Task 6.3: CSV出力シナリオのE2Eテスト (P1)

**目的**: PlaywrightでCSV出力のE2Eテストを実装

**ファイル**:
- `frontend/tests/e2e/participant-csv-export.spec.ts`

**受け入れ条件**:
- [ ] GROUP 1に10名を登録
- [ ] 「CSV出力」ボタンをクリック
- [ ] CSVファイルがダウンロードされることを確認
- [ ] CSVファイルの内容を検証（ダミーユーザー除外）

**依存**: Task 5.9 (ParticipantsPage), バックエンドAPI完成

---

#### Task 6.4: 参加者編集・削除シナリオのE2Eテスト (P2)

**目的**: Playwrightで参加者編集・削除のE2Eテストを実装

**ファイル**:
- `frontend/tests/e2e/participant-edit-delete.spec.ts`

**受け入れ条件**:
- [ ] GROUP 1に5名を登録
- [ ] 参加者の編集アイコンをクリック
- [ ] 段級位を変更して保存
- [ ] 参加者一覧が再ソートされることを確認
- [ ] 参加者の削除アイコンをクリック
- [ ] 削除確認ダイアログで「削除」をクリック
- [ ] 参加者一覧から削除されることを確認
- [ ] ダミーユーザーが自動追加されることを確認（奇数の場合）

**依存**: Task 5.9 (ParticipantsPage), バックエンドAPI完成

---

### セクション 7: パフォーマンス検証

#### Task 7.1: 負荷テストの実施 (P1)

**目的**: JMeterで目標性能を検証

**受け入れ条件**:
- [ ] CSV一括登録（32名）: 1分以内
- [ ] 参加者登録後の対戦表反映（256名規模）: 3秒以内
- [ ] グループ切り替え: 1秒以内
- [ ] 負荷テストレポートを作成

**ファイル**:
- `backend/performance/participant-load-test.jmx`
- `backend/performance/load-test-report.md`

**依存**: 全バックエンドAPI完成

---

## タスク完了チェックリスト

### Phase 2完了条件

- [ ] すべてのP0タスクが完了
- [ ] すべてのP1タスクが完了
- [ ] 単体テストカバレッジ80%以上
- [ ] E2Eテストがすべてパス
- [ ] パフォーマンス目標を達成
- [ ] コードレビュー完了
- [ ] ドキュメント更新（README, API仕様）

---

## 次のステップ

1. **Task 1.1から順次実装**: TDDサイクル（Red → Green → Refactor）で進める
2. **毎日の進捗記録**: 完了したタスクに`[x]`マークを付ける
3. **ブロッカー発生時**: plan.mdにリスクとして記録し、緩和策を検討
4. **Phase 2完了後**: プルリクエスト作成、メインブランチへマージ

**関連ドキュメント**:
- [仕様書](./spec.md)
- [実装計画](./plan.md)
- [データモデル](./data-model.md)
- [API契約](./contracts/)
- [開発ガイド](./quickstart.md)
