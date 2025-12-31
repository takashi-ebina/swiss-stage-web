# 実装計画: [機能]

**Branch**: `[###-feature-name]` | **日付**: [DATE] | **仕様**: [link]
**入力**: `/specs/[###-feature-name]/spec.md` からの機能仕様

**注意**: このテンプレートは `/speckit.plan` コマンドによって記入されます。実行ワークフローについては `.specify/templates/commands/plan.md` を参照してください。

## 概要

[機能仕様から抽出: 主な要件 + 調査からの技術的アプローチ]

## 技術的コンテキスト
<!--
  必要なアクション: このセクションの内容をプロジェクトの技術的詳細に置き換えてください。
  ここでの構造は、反復プロセスをガイドするための助言的なものです。
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## 憲章チェック

*GATE:フェーズ0の調査前に合格する必要があります。フェーズ1の設計後に再確認してください。*

### 原則I: ドメイン駆動設計 (DDD)
- [ ] バックエンドはDDDレイヤー構造（application/domain/infrastructure/presentation）に従っているか
- [ ] ドメインロジックがdomain層に集約されているか
- [ ] infrastructure層への依存が逆転していないか

### 原則II: テスト駆動開発 (TDD)【非交渉】
- [ ] 受け入れテストシナリオが仕様書に明記されているか
- [ ] テストコード作成 → 実装の順序が守られているか
- [ ] テストカバレッジ目標（domain層90%以上、application層80%以上）を満たせるか

### 原則III: AIペアプログラミング
- [ ] AIに実施させる範囲（日本語記述、コード生成、設計提案、UI記述）が明確か
- [ ] 技術スタック変更の独断決定を防ぐ仕組みがあるか

### 原則IV: 段階的機能実装
- [ ] 機能に優先度（P1/P2/P3）がついているか
- [ ] MVP機能（P1）が明確に定義されているか

### 原則V: スケーラビリティと可観測性
- [ ] パフォーマンス目標（300名対応、5秒以内マッチング）を満たせるか
- [ ] 構造化ログ（JSON）をCloudWatch Logsに出力する設計か
- [ ] 主要APIエンドポイントのレスポンスタイム監視が含まれているか

## プロジェクト構造

### ドキュメント (この機能)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### ソースコード (リポジトリのルート)
<!--
  必要なアクション: この機能の具体的なレイアウトで以下のプレースホルダーのツリーを置き換えてください。未使用のオプションは削除し、選択した構造を実際のパス（例: apps/admin, packages/something）で展開してください。提供された計画にはオプションのラベルを含めないでください。
-->

```text
# [REMOVE IF UNUSED] オプション 1: 単一プロジェクト (デフォルト)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] オプション 2: Web アプリケーション (「フロントエンド」+「バックエンド」が検出された場合)
backend/
├── src/
│   ├── application/      # ユースケース・アプリケーションサービス
│   ├── domain/           # ビジネスロジック・エンティティ・リポジトリインターフェース
│   ├── infrastructure/   # DB・外部API実装
│   └── presentation/     # REST APIエンドポイント
└── tests/
    ├── unit/
    ├── integration/
    └── contract/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] オプション 3: モバイル + API (「iOS/Android」が検出された場合)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**構造の決定**: [選択した構造を文書化し、上記で示した実際のディレクトリを参照してください]

## 複雑さの追跡

> **憲法チェックに違反があり、それを正当化する必要がある場合にのみ記入してください**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
