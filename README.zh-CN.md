# cosmos-workflow

[English](./README.md) | [日本語](./README.ja.md)

`cosmos-workflow` 是一个基于 `java-cosmos` 的 Java 工作流库，面向文档型存储场景。它把“流程设计”和“流程运行”分开，并通过少量核心入口提供工作流定义与实例流转能力。

## 概览

- 创建工作流与定义
- 启动流程实例
- 执行审批流转
- 支持单人审批、多人审批、机器人节点
- 支持版本控制、回退、撤回、拒绝、重绑定义、强制跳转节点
- 支持插件、通知、操作者展开、权限限制和自定义校验

主要入口：

- `ProcessDesign`: 流程定义与版本管理
- `ProcessEngine`: 流程实例启动与流转

内置能力：

- 开始节点和结束节点
- 单人审批节点
- 支持 `OR` / `AND` 的多人审批节点
- 机器人节点
- 工作流版本控制
- `NEXT`、`BACK`、`REJECT`、`CANCEL`、`WITHDRAW`、`RETRIEVE`、`REBINDING`、`RELOCATE` 等动作

## 运行要求

- Java 17
- Maven
- 一个兼容 `java-cosmos` 的后端

后端可以是 Cosmos DB、MongoDB 或 Postgres。对于 Postgres 和 MongoDB，首次访问时会自动初始化表结构或索引。

## 安装

依赖：

```xml
<dependency>
  <groupId>jp.co.onehr.workflow</groupId>
  <artifactId>cosmos-workflow</artifactId>
  <version>0.2.11</version>
</dependency>
```

构建：

```bash
mvn test
mvn package
```

## 快速开始

### 1. 注册数据库

将一个逻辑 `host` 绑定到一个数据库和一个集合：

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

对于 Postgres，框架会在首次访问时自动创建工作流相关表和索引。

如果启动期或容器初始化阶段已经处理注册，只需要在那里调用一次 `registerDB(...)` 并复用同一个 `host`。

默认数据库自动注册使用以下环境变量：

```env
ENABLE_WORKFLOW_DEFAULT_DB=true
FW_WORKFLOW_CONNECTION_STRING=...
FW_WORKFLOW_DATABASE_NAME=Data
FW_WORKFLOW_COLLECTION_NAME=Data
```

构建入口对象：

```java
var processDesign = configuration.buildProcessDesign();
var processEngine = configuration.buildProcessEngine();
```

### 2. 全局初始化与租户注册

应用启动时：

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

按租户或 `host` 注册数据库：

```java
public class WorkflowTenantRegistrar {

    public void register(String host, CosmosDatabase db, String collectionName) {
        var configuration = ProcessConfiguration.getConfiguration();
        configuration.registerDB(host, db, collectionName);
    }
}
```

只有 `registerDB(...)` 是 `host` 级配置。其他 `register*` 方法以及 `setPartitionSuffix(...)` 都写入单例 `ProcessConfiguration`，应在应用启动时统一设置一次，不要随着 `host` 切换。

### 3. 创建工作流

```java
var creation = new WorkflowCreationParam();
creation.name = "Expense Approval";
creation.enableOperatorControl = false;

var managerApproval = new SingleNode("Manager Approval");
managerApproval.operatorId = "manager-1";
creation.nodes.add(managerApproval);

var workflow = processDesign.createWorkflow(host, creation);
```

结果：

- 一个 `Workflow`
- 一个初始 `Definition`
- 自动补上的开始节点和结束节点

### 4. 更新定义

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

开启版本管理时，`upsertDefinition` 生成新版本；关闭时，更新当前版本。

### 5. 启动实例

```java
var application = new ApplicationParam();
application.workflowId = workflow.id;
application.applicant = "employee-1";
application.comment = "April expense claim";

var instance = processEngine.startInstance(host, application);
```

### 6. 推进实例

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

## 核心接口

### 定义接口

`ProcessDesign` 提供以下接口：

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

### 运行接口

`ProcessEngine` 提供以下接口：

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

## 动作说明

`ProcessEngine.resolve(...)` 通过 `Action` 选择当前实例的处理动作。

- `SAVE`: 停留在当前节点
- `NEXT`: 流转到下一个节点
- `BACK`: 回退；需要附加信息时使用 `ActionExtendParam.backMode` 和 `backNodeId`
- `CANCEL`: 将状态设为 `CANCELED`
- `REJECT`: 将状态设为 `REJECTED`
- `WITHDRAW`: 将实例从主分区移除，并把原始数据写入回收分区；默认会在 90 天后随 TTL 过期
- `RETRIEVE`: 将实例从下一节点取回到上一操作者
- `REBINDING`: 将实例绑定到其他定义版本
- `RELOCATE`: 将实例移动到指定节点
- `APPLY`: `startInstance(...)` 写入初始日志时使用的动作名；显式调用时，将实例重置到首节点并把状态设为 `PROCESSING`
- `REAPPLY`: 状态为 `PROCESSING` 且位于首节点时的再次提交；节点流转与 `NEXT` 相同，日志动作名保持为 `REAPPLY`

`ActionExtendParam` 中的相关字段：

- `comment`: 写入操作日志
- `instanceContext`: 本次动作使用的业务上下文
- `logContext`: 写入操作日志的扩展数据
- `notification`: 传给通知发送器的通知内容
- `pluginParam`: 节点插件执行时使用的输入参数
- `operationMode`: `OPERATOR_MODE` 或 `ADMIN_MODE`
- `backMode`、`backNodeId`: `BACK` 的附加参数

## 运行说明

- `ProcessDesign` 和 `ProcessEngine` 只构建一次，放到项目级公共 service 里
- 在容器初始化、租户初始化或应用启动阶段，把数据库注册和扩展点注册放在一起做
- `host` 通常作为租户或环境维度的逻辑键使用
- 在启动期一次性配置 `setPartitionSuffix(...)`，用于不同产品线或上下文的数据隔离；不要按 `host` 动态修改
- 在调用 `upsertDefinition(...)` 之前，先在业务层校验审批人 ID 是否有效
- 如果产品规则固定，可以在封装层统一覆盖某些默认行为

一种做法是先在业务层完成 approver 校验，再在封装层覆盖 `returnToStartNode` 这类默认行为。

## 常用参数说明

### `WorkflowCreationParam`

字段：

- `name`: 工作流名称
- `id`: 可选，自定义工作流 ID
- `enableVersion`: 是否启用定义版本控制
- `enableOperatorControl`: 是否限制节点操作者必须出现在 `allowedOperatorIds` 中
- `allowedOperatorIds`: 允许使用的操作者白名单
- `nodes`: 自定义节点列表，会插入到自动生成的开始和结束节点之间
- `startNodeName`, `endNodeName`: 自动生成的边界节点名称
- `startNodeConfiguration`, `endNodeConfiguration`: 附加到开始或结束节点的自定义配置
- `returnToStartNode`: 当执行 `REJECT` 或 `CANCEL` 时，是否回到第一个节点

默认值：

- 默认开启版本控制
- 默认开启操作人限制
- 默认申请模式是 `SELF`
- 如果不传自定义节点，流程就是 `Start -> End`

### `DefinitionParam`

字段：

- `workflowId`: 目标工作流 ID
- `nodes`: 要保存的完整节点列表
- `applicationModes`: 允许的申请模式，例如 `SELF`、`PROXY`
- `enableOperatorControl`
- `allowedOperatorIds`
- `returnToStartNode`

`nodes` 表示最终要保存的完整节点列表。更新现有定义时，通常包含当前的开始节点和结束节点。

### `ApplicationParam`

字段：

- `workflowId`: 目标工作流 ID
- `applicationMode`: `SELF` 或 `PROXY`
- `applicant`: 申请人 ID
- `proxyApplicant`: 代申请时的原始申请人
- `comment`: 首次申请备注
- `instanceContext`: 实例业务上下文
- `logContext`: 操作日志扩展上下文
- `notification`: 传给通知发送器的通知内容

### `ActionExtendParam`

字段：

- `comment`: 本次操作备注
- `instanceContext`: 更新后的实例上下文
- `logContext`: 操作日志扩展数据
- `notification`: 通知内容
- `pluginParam`: 节点插件参数
- `operationMode`: `OPERATOR_MODE` 或 `ADMIN_MODE`
- `backMode`: 与 `Action.BACK` 搭配使用
- `backNodeId`: 某些回退场景下的目标节点 ID

### 其他参数对象

- `WorkflowUpdatingParam`: 更新工作流名称、是否启用版本管理等元信息
- `RebindingParam`: 将单个实例重绑到指定定义版本
- `BulkRebindingParam`: 按状态批量重绑实例
- `RelocateParam`: 强制将实例移动到指定节点

## 节点类型

### `SingleNode`

- 关键字段：`operatorId`

### `MultipleNode`

- 关键字段：`operatorIdSet`、`operatorOrgIdSet`、`approvalType`
- `approvalType = OR`: 任意一个审批人通过即可
- `approvalType = AND`: 所有展开后的审批人都需要通过

### `RobotNode`

机器人节点，不需要人工操作者。

### `StartNode` / `EndNode`

流程边界节点，通常在工作流创建时自动生成。

## 配置

`ProcessConfiguration` 扩展点：

- `registerOperatorService(...)`: 把操作者 ID 或组织 ID 展开成真实审批人
- `registerPlugin(...)`: 注册自定义插件
- `registerNotificationSender(...)`: 接入通知发送逻辑
- `registerActionRestriction(...)`: 控制普通操作者可见或可执行的动作
- `registerAdminActionRestriction(...)`: 控制管理员动作
- `registerOperatorLogService(...)`: 自定义操作日志处理
- `registerContextParamService(...)`: 为批量操作补充上下文
- `registerValidationsService(...)`: 注册自定义定义校验
- `enableRetrieveResetParallelApproval(true)`: 对 `AND` 并行审批启用 retrieve 后重新全员审批
- `setPartitionSuffix(...)`: 给分区名追加后缀，可用于测试隔离；这是进程级启动配置，不是按 `host` 动态切换的选项

## 环境变量

默认数据库自动注册使用以下变量：

- `ENABLE_WORKFLOW_DEFAULT_DB`
- `FW_WORKFLOW_CONNECTION_STRING`
- `FW_WORKFLOW_DATABASE_NAME`，默认值是 `Data`
- `FW_WORKFLOW_COLLECTION_NAME`，默认值是 `Data`

如果项目根目录存在 `.env`，库也会读取其中的配置。

## 补充说明

- `ProcessConfiguration` 是单例，JVM 内全局共享。
- `host` 是逻辑标识，用来选取已注册的数据库和集合名。
- 首次访问数据时，框架会按需创建表或索引。
- `Workflow.currentVersion` 决定新实例使用哪个定义版本。
- 每个实例的操作历史保存在 `operateLogList` 中。

## License

[MIT](./LICENSE.md)
