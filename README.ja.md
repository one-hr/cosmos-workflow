# cosmos-workflow

[English](./README.md) | [中文](./README.zh-CN.md)

`cosmos-workflow` は、`java-cosmos` を前提にした Java のワークフローライブラリです。ワークフロー定義と実行を分けた構成で、少数のコアな入口を通して定義管理とインスタンス操作を提供します。

## 概要

- ワークフローと定義の作成
- インスタンスの起動
- 承認フローの進行
- 単一承認者ノード、複数承認者ノード、ロボットノード
- バージョン管理
- `BACK`、`REJECT`、`CANCEL`、`RETRIEVE`、`REBINDING`、`RELOCATE` などの操作
- プラグイン、通知、承認者展開、制限、独自バリデーションの拡張

主要な入口:

- `ProcessDesign`: ワークフロー定義とバージョン管理
- `ProcessEngine`: インスタンスの開始と操作

組み込み機能:

- 開始ノードと終了ノード
- 単一承認者ノード
- `OR` / `AND` を持つ複数承認者ノード
- ロボットノード
- ワークフローのバージョン管理
- `NEXT`、`BACK`、`REJECT`、`CANCEL`、`WITHDRAW`、`RETRIEVE`、`REBINDING`、`RELOCATE` などの操作

## 要件

- Java 17
- Maven
- `java-cosmos` 互換のバックエンド

バックエンドは Cosmos DB、MongoDB、Postgres を利用できます。Postgres と MongoDB では、初回アクセス時に必要なテーブルやインデックスが自動で初期化されます。

## インストール

依存関係:

```xml
<dependency>
  <groupId>jp.co.onehr.workflow</groupId>
  <artifactId>cosmos-workflow</artifactId>
  <version>0.2.11</version>
</dependency>
```

ビルド:

```bash
mvn test
mvn package
```

## クイックスタート

### 1. データベースを登録する

1 つの論理 `host` に対して 1 つのデータベースと 1 つのコレクションを登録します:

```java
var host = "tenant-demo";
var collectionName = "workflow_data";

var db = new CosmosBuilder()
        .withDatabaseType("postgres")
        .withConnectionString("jdbc:postgresql://localhost:5432/workflow?user=postgres&password=postgres")
        .build()
        .getDatabase("Data");

var configuration = ProcessConfiguration.getConfiguration();
configuration.registerDB(host, db, collectionName);
```

Postgres では、必要なテーブルやインデックスは初回アクセス時に自動作成されます。

起動時やコンテナ初期化時に登録フローがある場合は、その中で `registerDB(...)` を 1 回だけ呼び、同じ `host` を使い回します。

デフォルト DB 自動登録に使う環境変数:

```env
ENABLE_WORKFLOW_DEFAULT_DB=true
FW_WORKFLOW_CONNECTION_STRING=...
FW_WORKFLOW_DATABASE_NAME=Data
FW_WORKFLOW_COLLECTION_NAME=Data
```

入口オブジェクトの生成:

```java
var processDesign = configuration.buildProcessDesign();
var processEngine = configuration.buildProcessEngine();
```

### 2. グローバル初期化とテナント登録

アプリケーション起動時:

```java
public final class WorkflowBootstrap {

    private static final ProcessConfiguration configuration = ProcessConfiguration.getConfiguration();

    public static final ProcessDesign processDesign;
    public static final ProcessEngine processEngine;

    static {
        configuration.setPartitionSuffix("APP");
        configuration.registerOperatorService(customOperatorService);
        configuration.registerPlugin(customPlugin);
        configuration.registerNotificationSender(customNotificationSender);
        configuration.registerOperatorLogService(customOperatorLogService);
        configuration.registerActionRestriction(customActionRestriction);
        configuration.registerAdminActionRestriction(customAdminActionRestriction);
        configuration.registerValidationsService(customValidationService);

        processDesign = configuration.buildProcessDesign();
        processEngine = configuration.buildProcessEngine();
    }

    private WorkflowBootstrap() {
    }
}
```

`host` ごとの DB 登録:

```java
public class WorkflowTenantRegistrar {

    public void register(String host, CosmosDatabase db, String collectionName) {
        var configuration = ProcessConfiguration.getConfiguration();
        configuration.registerDB(host, db, collectionName);
    }
}
```

`host` ごとの設定は `registerDB(...)` だけです。その他の `register*` メソッドと `setPartitionSuffix(...)` は単一の `ProcessConfiguration` に保存されるため、プロセス全体の起動設定として 1 回だけ行ってください。

### 3. ワークフローを作成する

```java
var creation = new WorkflowCreationParam();
creation.name = "Expense Approval";
creation.enableOperatorControl = false;

var managerApproval = new SingleNode("Manager Approval");
managerApproval.operatorId = "manager-1";
creation.nodes.add(managerApproval);

var workflow = processDesign.createWorkflow(host, creation);
```

生成結果:

- `Workflow`
- 初期 `Definition`
- 自動生成される開始ノードと終了ノード

### 4. 定義を更新する

```java
var current = processDesign.getCurrentDefinition(host, workflow.id, 0);

var financeApproval = new MultipleNode(
        "Finance Approval",
        ApprovalType.OR,
        Set.of("finance-1", "finance-2"),
        Set.of()
);

current.nodes.add(2, financeApproval);

var definitionParam = new DefinitionParam();
definitionParam.workflowId = workflow.id;
definitionParam.enableOperatorControl = false;
definitionParam.nodes.addAll(current.nodes);

processDesign.upsertDefinition(host, definitionParam);
```

バージョン管理が有効な場合、`upsertDefinition` は新しい定義バージョンを作成します。無効な場合は現在の定義を更新します。

### 5. インスタンスを開始する

```java
var application = new ApplicationParam();
application.workflowId = workflow.id;
application.applicant = "employee-1";
application.comment = "April expense claim";

var instance = processEngine.startInstance(host, application);
```

### 6. インスタンスを進める

```java
var extendParam = new ActionExtendParam();
extendParam.comment = "Approved by manager";

var result = processEngine.resolve(
        host,
        instance.id,
        Action.NEXT,
        "manager-1",
        extendParam
);

instance = result.instance;
```

## 主要 API

### 定義 API

`ProcessDesign` の公開メソッド:

- `createWorkflow(host, WorkflowCreationParam)`
- `updateWorkflow(host, WorkflowUpdatingParam)`
- `getWorkflow(host, workflowId)`
- `readWorkflow(host, workflowId)`
- `deleteWorkflow(host, workflowId)`
- `findWorkflows(host, Condition)`
- `upsertDefinition(host, DefinitionParam)`
- `getDefinition(host, definitionId)`
- `getCurrentDefinition(host, workflowId, version)`
- `findDefinitions(host, Condition)`

### 実行 API

`ProcessEngine` の公開メソッド:

- `startInstance(host, ApplicationParam)`
- `getInstance(host, instanceId)`
- `getInstanceWithOps(host, instanceId, operatorId)`
- `resolve(host, instanceId, Action, operatorId)`
- `resolve(host, instanceId, Action, operatorId, ActionExtendParam)`
- `rebinding(host, instanceId, operatorId, RebindingParam)`
- `bulkRebinding(host, workflowId, operatorId, BulkRebindingParam)`
- `relocate(host, definitionId, operatorId, RelocateParam)`
- `findInstances(host, Condition)`
- `migrationInstance(host, Instance)`

## 操作の説明

`ProcessEngine.resolve(...)` は `Action` によって現在のインスタンス操作を選択します。

- `SAVE`: 現在のノードに留まる
- `NEXT`: 次のノードへ進む
- `BACK`: 戻る。追加情報が必要な場合は `ActionExtendParam.backMode` と `backNodeId` を使う
- `CANCEL`: 状態を `CANCELED` に設定する
- `REJECT`: 状態を `REJECTED` に設定する
- `WITHDRAW`: インスタンスをメインパーティションから削除し、元データのコピーをリサイクル用パーティションへ移す。既定では 90 日後に TTL で失効する
- `RETRIEVE`: 次ノードから前の担当者側へ引き戻す
- `REBINDING`: インスタンスを別の定義バージョンに再バインドする
- `RELOCATE`: 指定ノードへ移動する
- `APPLY`: `startInstance(...)` が書き込む初期ログの操作名。明示的に実行した場合は、先頭ノードに戻して状態を `PROCESSING` に設定する
- `REAPPLY`: 状態が `PROCESSING` で先頭ノードにあるときの再申請。ノード遷移は `NEXT` と同じで、ログ上の操作名は `REAPPLY` のまま

`ActionExtendParam` の関連フィールド:

- `comment`: 操作ログに保存されるコメント
- `instanceContext`: この操作で使う業務コンテキスト
- `logContext`: 操作ログに保存される追加データ
- `notification`: 登録済み送信処理に渡される通知内容
- `pluginParam`: プラグイン実行時の入力
- `operationMode`: `OPERATOR_MODE` または `ADMIN_MODE`
- `backMode`、`backNodeId`: `BACK` 用の追加入力

## 運用メモ

- `ProcessDesign` と `ProcessEngine` は 1 回だけ生成し、共通サービスの中で保持する
- DB 登録と拡張ポイント登録は、コンテナ初期化やテナント初期化のタイミングでまとめて行う
- `host` は固定文字列ではなく、テナントや環境を表す論理キーとして扱う
- `setPartitionSuffix(...)` は起動時に 1 回だけ設定し、製品やコンテキストごとのデータ分離に使う。`host` ごとに動的変更しない
- `upsertDefinition(...)` の前に、承認者 ID の妥当性をアプリケーション側で検証する
- 製品仕様が決まっている場合は、ラッパーサービス側でデフォルト動作を固定する

一例として、定義更新前に approver ID の妥当性を確認し、ラッパー層で `returnToStartNode` のような既定動作を固定する構成があります。

## よく使うパラメータ

### `WorkflowCreationParam`

フィールド:

- `name`: ワークフロー名
- `id`: 任意のワークフロー ID
- `enableVersion`: 定義変更時に新しいバージョンを作るか
- `enableOperatorControl`: ノードの承認者を `allowedOperatorIds` で制限するか
- `allowedOperatorIds`: 許可された承認者 ID 一覧
- `nodes`: 開始ノードと終了ノードの間に入るカスタムノード
- `startNodeName`, `endNodeName`: 自動生成ノードの表示名
- `startNodeConfiguration`, `endNodeConfiguration`: それぞれのノードに保持させる任意設定
- `returnToStartNode`: `REJECT` または `CANCEL` 時に先頭ノードへ戻すか

デフォルト:

- バージョン管理は有効
- 承認者制御は有効
- 初期申請モードは `SELF`
- カスタムノードがなければ `Start -> End`

### `DefinitionParam`

フィールド:

- `workflowId`: 対象ワークフロー ID
- `nodes`: 保存する完全なノード一覧
- `applicationModes`: `SELF` や `PROXY` などの申請モード
- `enableOperatorControl`
- `allowedOperatorIds`
- `returnToStartNode`

`nodes` は保存対象となる完全なノード一覧です。既存定義の更新時には、現在の開始ノードと終了ノードを含みます。

### `ApplicationParam`

フィールド:

- `workflowId`: 対象ワークフロー ID
- `applicationMode`: `SELF` または `PROXY`
- `applicant`: 申請者 ID
- `proxyApplicant`: 代理申請時の元の申請者
- `comment`: 初回申請コメント
- `instanceContext`: インスタンスに紐づく業務コンテキスト
- `logContext`: 操作ログに載せる追加情報
- `notification`: 通知送信に渡すペイロード

### `ActionExtendParam`

フィールド:

- `comment`: 操作コメント
- `instanceContext`: 更新後の業務コンテキスト
- `logContext`: 操作ログの追加データ
- `notification`: 通知ペイロード
- `pluginParam`: ノードプラグイン向けパラメータ
- `operationMode`: `OPERATOR_MODE` または `ADMIN_MODE`
- `backMode`: `Action.BACK` 用の戻り方
- `backNodeId`: 戻り先ノード ID が必要な場合に使用

### その他

- `WorkflowUpdatingParam`: 名前や `enableVersion` などの更新
- `RebindingParam`: 単一インスタンスを別バージョン定義へ再バインド
- `BulkRebindingParam`: 条件に合うインスタンスをまとめて再バインド
- `RelocateParam`: 指定ノードへ強制移動

## ノード種別

### `SingleNode`

- 主なフィールド: `operatorId`

### `MultipleNode`

- 主なフィールド: `operatorIdSet`、`operatorOrgIdSet`、`approvalType`
- `approvalType = OR`: 誰か 1 人の承認で進む
- `approvalType = AND`: 展開後の全承認者の承認が必要

### `RobotNode`

人手を必要としない自動ノードです。

### `StartNode` / `EndNode`

ワークフローの境界ノードで、通常は作成時に自動生成されます。

## 設定

`ProcessConfiguration` の拡張点:

- `registerOperatorService(...)`: 承認者 ID や組織 ID を実際の承認者へ展開
- `registerPlugin(...)`: カスタムプラグインを登録
- `registerNotificationSender(...)`: 通知送信を差し込む
- `registerActionRestriction(...)`: 一般ユーザー向けの操作制御
- `registerAdminActionRestriction(...)`: 管理者向けの操作制御
- `registerOperatorLogService(...)`: 操作ログ処理のカスタマイズ
- `registerContextParamService(...)`: 一括処理向けコンテキスト生成
- `registerValidationsService(...)`: 独自バリデーション
- `enableRetrieveResetParallelApproval(true)`: `AND` 承認を retrieve した後に再度全員承認を要求
- `setPartitionSuffix(...)`: パーティション名へサフィックスを付与。これはプロセス起動時の全体設定であり、`host` ごとの動的オプションではない

## 環境変数

デフォルト DB 自動登録に使う変数:

- `ENABLE_WORKFLOW_DEFAULT_DB`
- `FW_WORKFLOW_CONNECTION_STRING`
- `FW_WORKFLOW_DATABASE_NAME`、デフォルトは `Data`
- `FW_WORKFLOW_COLLECTION_NAME`、デフォルトは `Data`

ルートに `.env` があれば、その内容も読み込みます。

## 補足

- `ProcessConfiguration` はシングルトンです。
- `host` は登録済み DB とコレクションを引くための論理キーです。
- 初回アクセス時に必要ならスキーマやインデックスが作られます。
- `Workflow.currentVersion` が新規インスタンスに使う定義バージョンを制御します。
- 操作履歴は各インスタンスの `operateLogList` に保存されます。

## License

[MIT](./LICENSE.md)
