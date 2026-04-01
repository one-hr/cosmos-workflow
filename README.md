# cosmos-workflow

[日本語](./README.ja.md) | [中文](./README.zh-CN.md)

`cosmos-workflow` is a Java workflow library for document-oriented storage built on top of `java-cosmos`. It separates workflow design from workflow execution and provides a small set of core entry points for:

- creating workflows and definitions
- starting workflow instances
- moving instances through approval steps
- extending behavior with plugins, notifications, operator expansion, and validations

## Overview

Main entry points:

- `ProcessDesign`: create and update workflows and definitions
- `ProcessEngine`: start and operate workflow instances

Built-in features:

- start and end nodes
- single-approver nodes
- multi-approver nodes with `OR` or `AND` approval
- robot nodes for automated steps
- workflow versioning
- actions such as `NEXT`, `BACK`, `REJECT`, `CANCEL`, `WITHDRAW`, `RETRIEVE`, `REBINDING`, and `RELOCATE`

## Requirements

- Java 17
- Maven
- A `java-cosmos`-compatible backend

The library works with Cosmos DB, MongoDB, or Postgres through `java-cosmos`. For Postgres and MongoDB, schema or index initialization runs automatically on first access.

## Installation

Dependency:

```xml
<dependency>
  <groupId>jp.co.onehr.workflow</groupId>
  <artifactId>cosmos-workflow</artifactId>
  <version>0.2.11</version>
</dependency>
```

Build:

```bash
mvn test
mvn package
```

## Quick Start

### 1. Register a database

Register one logical `host` with one database and one collection:

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

With Postgres, the library creates workflow-related tables and indexes on first access.

If startup or container initialization already handles registration, call `registerDB(...)` there once and reuse the same `host`.

Default DB registration via environment variables:

```env
ENABLE_WORKFLOW_DEFAULT_DB=true
FW_WORKFLOW_CONNECTION_STRING=...
FW_WORKFLOW_DATABASE_NAME=Data
FW_WORKFLOW_COLLECTION_NAME=Data
```

Build entry points:

```java
var processDesign = configuration.buildProcessDesign();
var processEngine = configuration.buildProcessEngine();
```

### 2. Global setup and tenant registration

Application startup:

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

Host registration:

```java
public class WorkflowTenantRegistrar {

    public void register(String host, CosmosDatabase db, String collectionName) {
        var configuration = ProcessConfiguration.getConfiguration();
        configuration.registerDB(host, db, collectionName);
    }
}
```

Only `registerDB(...)` is host-scoped. The other `register*` methods and `setPartitionSuffix(...)` write to the singleton `ProcessConfiguration`, so treat them as process-wide startup configuration and set them once before handling workflow requests.

### 3. Create a workflow

```java
var creation = new WorkflowCreationParam();
creation.name = "Expense Approval";
creation.enableOperatorControl = false;

var managerApproval = new SingleNode("Manager Approval");
managerApproval.operatorId = "manager-1";
creation.nodes.add(managerApproval);

var workflow = processDesign.createWorkflow(host, creation);
```

Creates:

- one `Workflow`
- one initial `Definition`
- implicit start and end nodes around your custom nodes

### 4. Update the definition

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

With versioning enabled, `upsertDefinition` creates a new definition version. Otherwise it updates the current version in place.

### 5. Start an instance

```java
var application = new ApplicationParam();
application.workflowId = workflow.id;
application.applicant = "employee-1";
application.comment = "April expense claim";

var instance = processEngine.startInstance(host, application);
```

### 6. Move the instance forward

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

## Core API

### Design API

`ProcessDesign` exposes:

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

### Runtime API

`ProcessEngine` exposes:

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

## Actions

`ProcessEngine.resolve(...)` uses `Action` to select the instance operation.

- `SAVE`: stay at the current node
- `NEXT`: move to the next node
- `BACK`: move back; use `ActionExtendParam.backMode` and `backNodeId` when required
- `CANCEL`: set status to `CANCELED`
- `REJECT`: set status to `REJECTED`
- `WITHDRAW`: remove the instance from the main partition and move a recycle copy to the recycle partition; by default the recycle entry expires after 90 days
- `RETRIEVE`: return the instance from the next node to the previous operator
- `REBINDING`: bind the instance to another definition version
- `RELOCATE`: move the instance to a specified node
- `APPLY`: action name used by the initial log written in `startInstance(...)`; as an explicit action, reset to the first node and set status to `PROCESSING`
- `REAPPLY`: resubmit from the first node while status is `PROCESSING`; node transition is the same as `NEXT`, log action remains `REAPPLY`

Related fields in `ActionExtendParam`:

- `comment`: stored in the operation log
- `instanceContext`: business context used during the action
- `logContext`: extra payload stored in the operation log
- `notification`: notification payload passed to the registered sender
- `pluginParam`: plugin input for nodes that execute plugins
- `operationMode`: `OPERATOR_MODE` or `ADMIN_MODE`
- `backMode`, `backNodeId`: additional input for `BACK`

## Operational notes

- build `ProcessDesign` and `ProcessEngine` once and keep them behind a project-level service
- register database and extension hooks together during container or tenant initialization
- use `host` as the tenant or environment key, not just a hard-coded string
- configure `setPartitionSuffix(...)` once at startup to isolate workflow partitions by product or bounded context; do not change it dynamically per `host`
- validate approver IDs in your application layer before calling `upsertDefinition(...)`
- normalize workflow defaults in your wrapper service when your product has fixed rules

One example is validating approver IDs in the application layer before `upsertDefinition(...)`, then overriding defaults such as `returnToStartNode` in the wrapper service.

## Important Parameters

### `WorkflowCreationParam`

Fields:

- `name`: workflow name
- `id`: optional custom workflow ID
- `enableVersion`: whether definition changes create new versions
- `enableOperatorControl`: whether node operators are restricted by `allowedOperatorIds`
- `allowedOperatorIds`: allowlist used when operator control is enabled
- `nodes`: custom nodes inserted between the generated start and end nodes
- `startNodeName`, `endNodeName`: custom names for generated boundary nodes
- `startNodeConfiguration`, `endNodeConfiguration`: custom payload attached to those boundary nodes
- `returnToStartNode`: on `REJECT` or `CANCEL`, return to the first node or stay on the current node

Defaults:

- versioning is enabled
- operator control is enabled
- the initial application mode is `SELF`
- a workflow with no custom node becomes `Start -> End`

### `DefinitionParam`

Fields:

- `workflowId`: required target workflow
- `nodes`: the full node list to persist
- `applicationModes`: allowed application modes such as `SELF` and `PROXY`
- `enableOperatorControl`
- `allowedOperatorIds`
- `returnToStartNode`

`nodes` is the full node list to persist. When updating an existing definition, it typically includes the current start and end nodes.

### `ApplicationParam`

Fields:

- `workflowId`: target workflow ID
- `applicationMode`: `SELF` or `PROXY`
- `applicant`: applicant user ID
- `proxyApplicant`: original applicant when proxy mode is used
- `comment`: initial operation comment
- `instanceContext`: business context stored with the instance
- `logContext`: extra payload attached to the operation log
- `notification`: notification payload passed to a registered sender

### `ActionExtendParam`

Fields:

- `comment`: operation comment
- `instanceContext`: updated business context
- `logContext`: extra operation log payload
- `notification`: notification payload
- `pluginParam`: parameters used by plugins attached to a node
- `operationMode`: `OPERATOR_MODE` or `ADMIN_MODE`
- `backMode`: used with `Action.BACK`
- `backNodeId`: target node when the back strategy needs an explicit node

### Other operation parameters

- `WorkflowUpdatingParam`: update workflow metadata such as `name` and `enableVersion`
- `RebindingParam`: move one instance to another definition version
- `BulkRebindingParam`: move many instances by status to another definition version
- `RelocateParam`: force one instance to a specific node

## Node Types

### `SingleNode`

- field: `operatorId`
- empty operator IDs fail node validation

### `MultipleNode`

- fields: `operatorIdSet`, `operatorOrgIdSet`, `approvalType`
- `approvalType = OR`: any approver can move the instance
- `approvalType = AND`: all expanded approvers must approve

### `RobotNode`

Automated node without manual operators.

### `StartNode` and `EndNode`

Boundary nodes generated automatically during workflow creation.

## Configuration

`ProcessConfiguration` hooks:

- `registerOperatorService(...)`: expand operator IDs or organization IDs into actual approvers
- `registerPlugin(...)`: register custom node plugins
- `registerNotificationSender(...)`: send notifications during workflow actions
- `registerActionRestriction(...)`: hide or block operator actions
- `registerAdminActionRestriction(...)`: hide or block admin actions
- `registerOperatorLogService(...)`: customize operation log handling
- `registerContextParamService(...)`: enrich context for bulk operations
- `registerValidationsService(...)`: run custom definition validation
- `enableRetrieveResetParallelApproval(true)`: when retrieving an `AND` approval node, require all parallel approvers to approve again
- `setPartitionSuffix(...)`: append a suffix to partition names for tests or isolated environments; set it once at startup, not per `host`

## Environment Variables

Used when default DB registration is enabled:

- `ENABLE_WORKFLOW_DEFAULT_DB`: set to `true` to auto-register the default database
- `FW_WORKFLOW_CONNECTION_STRING`: connection string for `java-cosmos`
- `FW_WORKFLOW_DATABASE_NAME`: database name, default is `Data`
- `FW_WORKFLOW_COLLECTION_NAME`: collection name, default is `Data`

If present, `.env` is also read.

## Notes

- `ProcessConfiguration` is a singleton. Configuration is global within the JVM.
- `host` is a logical key used to select a registered database and collection.
- The first data access triggers schema or index initialization when needed.
- `Workflow.currentVersion` controls which definition version is used for new instances.
- Operation history is stored in `operateLogList` on each instance.

## License

[MIT](./LICENSE.md)
