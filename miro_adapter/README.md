# miro_adapter

The MIRO adapter pushes records from a Miro XML export and stores them in a DynamoDB table.
Our XML exports are kept in an S3 bucket -- the adapter pulls them down, parses them, and pushes them back into DynamoDB.

## Usage

The Miro adapter is kept as a task definition in ECS, and can be invoked as a one-off task using the RunTask API.
For example:

```console
aws ecs run-task --cluster=services_cluster \
    --task-definition=miro_adapter_task_definition \
    --overrides='{
      "containerOverrides": [
        {
          "name": "app",
          "environment": [
            { "name": "KEY", "value": "images-V.xml" },
            { "name": "COLLECTION", "value": "Images-V"}
          ]
        }
      ]
    }'
```

## Deployment

The Miro adapter is built and deployed automatically by Travis.
