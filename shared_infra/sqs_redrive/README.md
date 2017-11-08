# sqs_redrive

This is a tool for reading all messages in a queue and sending them to another queue. Typically used to redrive messages from DLQs.
Invoke the task with the following command:

```
aws ecs run-task \
    --cluster=services_cluster \
    --task-definition=sqs_redrive_task_definition \
    --overrides='{
      "containerOverrides": [{
        "name":"SQS_SOURCE_URL",
        "environment": [{
          "name":"SQS_SOURCE_URL",
          "value": "https://sqs.eu-west-1.amazonaws.com/12345678910/source_queue"
        },
        {
          "name":"SQS_TARGET_URL",
          "value": "https://sqs.eu-west-1.amazonaws.com/12345678910/target_queue"
        }]
      }]
    }'
```
