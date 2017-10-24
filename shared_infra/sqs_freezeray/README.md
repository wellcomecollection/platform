# sqs_freezeray

This is a tool for dumping the contents of an SQS queue to a file in S3.
It writes a text file with a JSON blob on every line, one blob per message in SQS.

Invoke the task with the following command:

```
aws ecs run-task \
    --cluster=services_cluster \
    --task-definition=sqs_freezeray_task_definition \
    --overrides='{
      "containerOverrides": [{
        "name": "app",
        "environment": [{
          "name":"QUEUE_URL",
          "value": "https://sqs.eu-west-1.amazonaws.com/760097843905/lambda-notify_old_deploys_dlq"
        }]
      }]
    }'
```
